package com.winterfell.server.handler;

import com.winterfell.common.message.ClientToServerContent;
import com.winterfell.common.message.ContentPackage;
import com.winterfell.common.message.ContentParser;
import com.winterfell.common.message.ServerToClientContent;
import com.winterfell.common.protocol.TransferProtocol;
import com.winterfell.common.utils.RandomUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.ReferenceCountUtil;

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
     * 客户端 那边的 Socks5 Channel映射
     * <p>
     * 利用client那边发送过来的channelId在软件层面作负载均衡
     */
    Map<String, Channel> clientSocksChannelMap = new ConcurrentHashMap<>();

    /**
     * 客户端连接的Channel
     */
    private Channel connectChannel;

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

        final Channel clientConnectChannel = ctx.channel();

        byte[] content = msg.getContent();
        // 客户端发送给服务端的内容
        ClientToServerContent clientToServerContent = ContentParser.parseC2S(content);

        String channelId = clientToServerContent.getChannelId();

        Channel visitChannel = clientSocksChannelMap.get(channelId);

        if (Objects.nonNull(visitChannel)) {
            // 连接已经建立了
            boolean success = clientToServerContent.getSuccess() > 0;
            if (success) {
                byte[] c2SContentMsg = clientToServerContent.getMsg();
                visitChannel.writeAndFlush(Unpooled.copiedBuffer(c2SContentMsg));
                ReferenceCountUtil.release(msg);
            } else {
                // 发送的消息是失败通知消息
                visitChannel.close();
                clientSocksChannelMap.remove(channelId);
            }
        } else {
            // 连接还没有建立
            String address = clientToServerContent.getAddress();
            int port = clientToServerContent.getPort();

            byte[] receiveData = clientToServerContent.getMsg();

            Bootstrap bootstrap = new Bootstrap();

            bootstrap.group(clientConnectChannel.eventLoop()).channel(NioSocketChannel.class)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new VisitGoogleHandler(this, channelId, Unpooled.copiedBuffer(receiveData)));

            System.out.println("try to connect to outer world : " + address + ":" + port + " ### " + channelId);

            bootstrap.connect(address, port).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (!future.isSuccess()) {
                        // 外网连接失败
                        ServerToClientContent serverToClientContent = new ServerToClientContent(
                                channelId,
                                RandomUtils.getNegativeInt(),
                                new byte[]{0}
                        );
                        byte[] pack = ContentPackage.pack(serverToClientContent);
                        TransferProtocol transferProtocol = new TransferProtocol(
                                RandomUtils.getRandomInt(),
                                pack.length,
                                pack
                        );
                        clientConnectChannel.writeAndFlush(transferProtocol);
                    }
                }
            });
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
