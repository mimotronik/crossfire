package com.winterfell.server.handler;

import com.winterfell.common.message.ContentPackage;
import com.winterfell.common.message.Option;
import com.winterfell.common.message.ServerToClientContent;
import com.winterfell.common.protocol.TransferProtocol;
import com.winterfell.common.utils.RandomUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

/**
 * @author winterfell
 */
@ChannelHandler.Sharable
public class VisitOuterHandler extends ChannelInboundHandlerAdapter {

    private ClientConnectHandler connectHandler;

    private String clientSocksChannelId;

    public VisitOuterHandler(ClientConnectHandler connectHandler, String clientSocksChannelId) {
        this.connectHandler = connectHandler;
        this.clientSocksChannelId = clientSocksChannelId;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("channelActive -> get connection : " + clientSocksChannelId);

        connectHandler.clientSocksChannelMap.put(clientSocksChannelId, ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("channelInactive -> lose connection : " + clientSocksChannelId);
        connectHandler.clientSocksChannelMap.remove(clientSocksChannelId);
        ctx.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println("exceptionCaught -> lose connection : " + clientSocksChannelId);
        connectHandler.clientSocksChannelMap.remove(clientSocksChannelId);
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf) msg;
        if (buf.readableBytes() < 0) {
            ReferenceCountUtil.release(msg);
            return;
        }
        byte[] s2cMsg = new byte[buf.readableBytes()];
        buf.readBytes(s2cMsg);

        ServerToClientContent serverToClientContent = new ServerToClientContent(
                clientSocksChannelId,
                // 设置为失败消息通知客户端处理连接
                RandomUtils.getPositiveInt(),
                Option.send,
                s2cMsg
        );
        byte[] pack = ContentPackage.pack(serverToClientContent);
        TransferProtocol transferProtocol = new TransferProtocol(
                RandomUtils.getRandomInt(),
                pack.length,
                pack
        );
        // 回传给客户端
        connectHandler.getConnectChannel().writeAndFlush(transferProtocol);

        ReferenceCountUtil.release(msg);

    }
}
