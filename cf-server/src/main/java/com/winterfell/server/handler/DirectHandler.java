package com.winterfell.server.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.concurrent.Promise;

/**
 * @author winterfell
 */
@ChannelHandler.Sharable
public class DirectHandler extends ChannelInboundHandlerAdapter {

    private final Promise<Channel> promise;


    public DirectHandler(Promise<Channel> promise) {
        this.promise = promise;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        if(ctx.channel().isActive()){
            ctx.pipeline().remove(this);
            promise.setSuccess(ctx.channel());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable throwable) {
        promise.setFailure(throwable);
    }
}