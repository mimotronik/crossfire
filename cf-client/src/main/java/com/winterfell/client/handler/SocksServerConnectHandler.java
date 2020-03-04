package com.winterfell.client.handler;

import com.winterfell.client.connect.RemoteConnectHandler;
import io.netty.channel.*;
import io.netty.handler.codec.socksx.SocksMessage;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandResponse;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;

/**
 * @author winterfell
 */
@ChannelHandler.Sharable
public class SocksServerConnectHandler extends SimpleChannelInboundHandler<SocksMessage> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, SocksMessage message) throws Exception {
        if (message instanceof Socks5CommandRequest) {
            // 浏览器->client
            final Channel localSocksChannel = ctx.channel();
            final Socks5CommandRequest request = (Socks5CommandRequest) message;

            // socks5 connect success
            ChannelFuture responseFuture = localSocksChannel.writeAndFlush(new DefaultSocks5CommandResponse(
                    Socks5CommandStatus.SUCCESS,
                    request.dstAddrType(),
                    request.dstAddr(),
                    request.dstPort()));

            responseFuture.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (future.isSuccess()) {
                        localSocksChannel.pipeline().remove(SocksServerConnectHandler.this);

                        localSocksChannel.pipeline().addLast(new LocalHandler(request));

                        RemoteConnectHandler.addLocalChannel(localSocksChannel.id().asLongText(), localSocksChannel);

                    }
                }
            });

        } else {
            ctx.close();
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("local socks channel connected " + ctx.channel().id().asLongText());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("local socks channel disconnected " + ctx.channel().id().asLongText());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println("local socks channel disconnected " + ctx.channel().id().asLongText());
        ctx.close();
    }
}
