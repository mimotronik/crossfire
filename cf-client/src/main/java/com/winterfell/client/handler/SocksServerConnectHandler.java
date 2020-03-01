package com.winterfell.client.handler;

import com.winterfell.client.Client;
import com.winterfell.client.config.RemoteServerConfig;
import com.winterfell.common.utils.SocksServerUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.socksx.SocksMessage;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandResponse;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;

/**
 * @author winterfell
 */
@ChannelHandler.Sharable
public class SocksServerConnectHandler extends SimpleChannelInboundHandler<SocksMessage> {

    private final Bootstrap b = new Bootstrap();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, SocksMessage message) throws Exception {

        // 浏览器->client
        final Channel inboundChannel = ctx.channel();

        if (message instanceof Socks5CommandRequest) {
            final Socks5CommandRequest request = (Socks5CommandRequest) message;

            Promise<Channel> promise = ctx.executor().newPromise();
            promise.addListener(
                    new FutureListener<Channel>() {
                        @Override
                        public void operationComplete(final Future<Channel> future) throws Exception {
                            // client -- server
                            final Channel outboundChannel = future.getNow();
                            if (future.isSuccess()) {

                                /*
                                 *    The SOCKS request information is sent by the client as soon as it has
                                 *    established a connection to the SOCKS server, and completed the
                                 *    authentication negotiations.  The server evaluates the request, and
                                 *    returns a reply formed as follows
                                 */
                                System.out.println("connected socks success");

                                ChannelFuture responseFuture =
                                        inboundChannel.writeAndFlush(new DefaultSocks5CommandResponse(
                                                Socks5CommandStatus.SUCCESS,
                                                request.dstAddrType(),
                                                request.dstAddr(),
                                                request.dstPort()));

                                responseFuture.addListener(new ChannelFutureListener() {

                                    // socks握手成功
                                    @Override
                                    public void operationComplete(ChannelFuture channelFuture) {
                                        // ctx 浏览器 - client
                                        ctx.pipeline().remove(SocksServerConnectHandler.this);

                                        // 对外连接读取到消息的处理
                                        outboundChannel.pipeline().addLast(new OuterRelayHandler(inboundChannel));

                                        // 客户端添加处理器，处理 连接端发过来的消息
                                        inboundChannel.pipeline().addLast(
                                                new InnerRelayHandler(outboundChannel, request));
                                    }
                                });
                            } else {
                                ctx.channel().writeAndFlush(new DefaultSocks5CommandResponse(
                                        Socks5CommandStatus.FAILURE, request.dstAddrType()));
                                SocksServerUtils.closeOnFlush(ctx.channel());
                            }
                        }
                    });

            b.group(inboundChannel.eventLoop())
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new DirectClientHandler(promise)); // 产生 outboundChannel

//            System.out.println("ip:" + request.dstAddr() + " port:" + request.dstPort());
//            System.out.print("ip bytes :");
//            byte[] address = InetAddress.getByName(request.dstAddr()).getAddress();
//            for (int i = 0; i < address.length; i++) {
//                System.out.print(address[i] + ",");
//            }
//            System.out.println();


            RemoteServerConfig remoteConfig = Client.remoteConf;

//            b.connect(request.dstAddr(), request.dstPort())
            // 这里有一个问题就是 每次都要连接握手 理论只要连接一次就可以了
            b.connect(remoteConfig.getAddr(), remoteConfig.getPort())
                    .addListener(new ChannelFutureListener() {
                        @Override
                        public void operationComplete(ChannelFuture future) throws Exception {
                            if (!future.isSuccess()) {
                                ctx.channel().writeAndFlush(
                                        new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, request.dstAddrType()));
                                SocksServerUtils.closeOnFlush(ctx.channel());
                            }
                        }
                    });

        } else {
            ctx.close();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
    }
}
