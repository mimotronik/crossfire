package com.winterfell.server.handler;

import com.winterfell.common.message.*;
import com.winterfell.common.protocol.TransferProtocol;
import com.winterfell.common.utils.RandomUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.Promise;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 处理 client - server 的连接
 *
 * @author winterfell
 */
@ChannelHandler.Sharable
public class ClientConnectHandler extends SimpleChannelInboundHandler<TransferProtocol> {


    /**
     * 客户端连接的Channel
     */
    private Channel connectChannel;

    /**
     * 客户端 那边的 Socks5 Channel映射
     * <p>
     * 利用client那边发送过来的channelId在软件层面作负载均衡
     */
    Map<String, Channel> clientSocksChannelMap = new ConcurrentHashMap<>();


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.connectChannel = ctx.channel();
        System.out.println(ctx.channel().remoteAddress() + " connected ...");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println(ctx.channel().remoteAddress() + " lose connection ...");
    }

    /**
     * 读取到客户端发送过来的消息
     *
     * @param ctx
     * @param msg
     * @throws Exception
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TransferProtocol msg) throws Exception {

        byte[] content = msg.getContent();
        ClientToServerContent clientToServerContent = ContentParser.parseC2S(content);
        ReferenceCountUtil.release(msg);

        boolean success = clientToServerContent.getSuccess() > 0;

        String clientSocksChannelId = clientToServerContent.getChannelId();

        if (success) {   // 如果是一个成功的包
            byte option = clientToServerContent.getOption();
            if (option == Option.connect) {
                // 期望创建连接 并返回 成功
                String address = clientToServerContent.getAddress();
                int port = clientToServerContent.getPort();
                Bootstrap bootstrap = new Bootstrap();

                bootstrap.group(connectChannel.eventLoop()).channel(NioSocketChannel.class)
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                        .option(ChannelOption.SO_KEEPALIVE, true)
                        .handler(new VisitOuterHandler(this, clientSocksChannelId));
                System.out.println("try to connect to outer world : " + address + ":" + port + " ### " + clientSocksChannelId);
                bootstrap.connect(address, port).addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if (!future.isSuccess()) {
                            // 如果连接失败 返回一个连接成功的包
                            ServerToClientContent failContent = new ServerToClientContent(
                                    clientSocksChannelId,
                                    RandomUtils.getNegativeInt(),
                                    Option.other,
                                    new byte[]{-16}
                            );
                            byte[] pack = ContentPackage.pack(failContent);
                            ctx.writeAndFlush(new TransferProtocol(RandomUtils.getRandomInt(), pack.length, pack));
                        }else{
                            ServerToClientContent successS2CContent = new ServerToClientContent(
                                    clientSocksChannelId,
                                    RandomUtils.getPositiveInt(),
                                    Option.connect,
                                    new byte[]{16}
                            );
                            byte[] pack = ContentPackage.pack(successS2CContent);
                            ctx.writeAndFlush(new TransferProtocol(RandomUtils.getRandomInt(), pack.length, pack));
                        }
                    }
                });

            } else if (option == Option.send) {
                // 期望发送数据
                Channel visitChannel = clientSocksChannelMap.get(clientSocksChannelId);
                byte[] c2SContentMsg = clientToServerContent.getMsg();
                visitChannel.writeAndFlush(Unpooled.copiedBuffer(c2SContentMsg));
                ReferenceCountUtil.release(msg);
            }
        } else { // 如果是一个失败的包
            ReferenceCountUtil.release(msg);
            Channel visitChannel = clientSocksChannelMap.get(clientSocksChannelId);
            if (Objects.nonNull(visitChannel) && visitChannel.isActive()) {
                visitChannel.close();
            }
            clientSocksChannelMap.remove(clientSocksChannelId);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println("来自客户端的连接断开 " + ctx.channel().remoteAddress());
        cause.printStackTrace();
        ctx.close();
    }

    public Channel getConnectChannel() {
        return connectChannel;
    }
}
