package com.winterfell.client.handler;

import com.winterfell.common.utils.SocksServerUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

/**
 * @author winterfell
 */
@ChannelHandler.Sharable
public class OuterRelayHandler extends ChannelInboundHandlerAdapter {

    private final Channel inboundChannel;

    public OuterRelayHandler(Channel inboundChannel) {
        this.inboundChannel = inboundChannel;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ctx.writeAndFlush(Unpooled.EMPTY_BUFFER);
    }

    /**
     * 连接端收到消息，客户端解密回发给浏览器
     *
     * @param ctx
     * @param msg
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf buf = (ByteBuf) msg;
        System.out.println("连接端 接收到服务器传回的消息 " + ctx.channel().remoteAddress() + " " + buf.readableBytes());
        if (inboundChannel.isActive()) {
            inboundChannel.writeAndFlush(buf);
        } else {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        if (inboundChannel.isActive()) {
            SocksServerUtils.closeOnFlush(inboundChannel);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

}
