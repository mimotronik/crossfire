package com.winterfell.server.handler;

import com.winterfell.common.message.ContentPackage;
import com.winterfell.common.message.ServerToClientContent;
import com.winterfell.common.protocol.TransferProtocol;
import com.winterfell.common.utils.RandomUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

/**
 * @author winterfell
 */
@ChannelHandler.Sharable
public class VisitGoogleHandler extends ChannelInboundHandlerAdapter {

    private ClientConnectHandler connectHandler;
    private String clientSockChannelId;

    private ByteBuf receiveData;

    public VisitGoogleHandler(ClientConnectHandler clientConnectHandler, String clientSockChannelId, ByteBuf receiveData) {
        this.connectHandler = clientConnectHandler;
        this.clientSockChannelId = clientSockChannelId;
        this.receiveData = receiveData;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Channel visitChannel = ctx.channel();
        System.out.println("channelActive -> get connection : " + clientSockChannelId);
        connectHandler.clientSocksChannelMap.put(clientSockChannelId, ctx.channel());
        visitChannel.writeAndFlush(receiveData);
    }

    /**
     * 读取到 Google 回复的消息
     *
     * @param ctx
     * @param msg
     * @throws Exception
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        System.out.println("receive data from outer world : " + clientSockChannelId);

        Channel visitChannel = ctx.channel();
        if (visitChannel.isActive()) {
            ctx.executor().execute(() -> {
                ByteBuf buf = (ByteBuf) msg;
                if (buf.readableBytes() < 0) {
                    ReferenceCountUtil.release(msg);
                    return;
                }

                byte[] s2cMsg = new byte[buf.readableBytes()];
                buf.readBytes(s2cMsg);

                if (connectHandler.getConnectChannel().isActive()) {
                    ServerToClientContent serverToClientContent = new ServerToClientContent(
                            clientSockChannelId,
                            // 设置为失败消息通知客户端处理连接
                            RandomUtils.getPositiveInt(),
                            s2cMsg
                    );
                    byte[] pack = ContentPackage.pack(serverToClientContent);
                    TransferProtocol transferProtocol = new TransferProtocol(
                            RandomUtils.getRandomInt(),
                            pack.length,
                            pack
                    );
                    connectHandler.getConnectChannel().writeAndFlush(transferProtocol);
                }
                ReferenceCountUtil.release(msg);
            });

        } else {
            visitChannel.close();
            connectHandler.clientSocksChannelMap.remove(clientSockChannelId);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("channelInactive -> lose connection : " + clientSockChannelId);
        connectHandler.clientSocksChannelMap.remove(clientSockChannelId);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println("exceptionCaught -> lose connection : " + clientSockChannelId);
        connectHandler.clientSocksChannelMap.remove(clientSockChannelId);

        cause.printStackTrace();
        ctx.close();
    }

}
