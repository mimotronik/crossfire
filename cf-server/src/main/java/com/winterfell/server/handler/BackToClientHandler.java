package com.winterfell.server.handler;

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
public class BackToClientHandler extends ChannelInboundHandlerAdapter {

    private final Channel receiveChannel;
    private String addr;
    private int port;

    public BackToClientHandler(Channel receiveChannel) {
        this.receiveChannel = receiveChannel;
    }

    public BackToClientHandler(Channel receiveChannel, String addr, int port) {
        this.receiveChannel = receiveChannel;
        this.addr = addr;
        this.port = port;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // 数据回传
        if (receiveChannel.isActive()) {
            // TODO 这里回传的数据要加密
            System.out.println("收到 " + addr + ":" + port + " 回送的消息 " + ((ByteBuf) msg).readableBytes());
            receiveChannel.writeAndFlush(msg);
        } else {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("channelInactive" + ctx.channel());
        ctx.close();
    }
}
