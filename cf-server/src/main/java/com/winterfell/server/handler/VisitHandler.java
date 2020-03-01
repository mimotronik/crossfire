package com.winterfell.server.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

/**
 * 一旦连接成功，就把数据发送到外面
 *
 * @author winterfell
 */
@ChannelHandler.Sharable
public class VisitHandler extends ChannelInboundHandlerAdapter {
    private Channel receiveChannel;
    private ByteBuf receiveData;

    public VisitHandler(Channel receiveChannel, ByteBuf receiveData) {
        this.receiveChannel = receiveChannel;
        this.receiveData = receiveData;
    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        final Channel redirectChannel = ctx.channel();
        if (ctx.channel().isActive()) {
            redirectChannel.writeAndFlush(receiveData);
        } else {
            receiveChannel.close();
            ReferenceCountUtil.release(msg);
            ReferenceCountUtil.release(receiveData);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println(cause.getMessage());
        ctx.close();
    }
}
