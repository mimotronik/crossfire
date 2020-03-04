package com.winterfell.client.handler;

import com.winterfell.client.connect.RemoteConnectHandler;
import com.winterfell.common.message.ClientToServerContent;
import com.winterfell.common.message.Option;
import com.winterfell.common.utils.RandomUtils;
import com.winterfell.common.utils.SocksServerUtils;
import io.netty.channel.*;
import io.netty.handler.codec.socksx.SocksMessage;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandResponse;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;

/**
 * Socks5 连接成功的处理方式
 *
 * @author winterfell
 */
@ChannelHandler.Sharable
public class SocksServerConnectHandler extends SimpleChannelInboundHandler<SocksMessage> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, SocksMessage message) throws Exception {
        if (message instanceof Socks5CommandRequest) {
            // 浏览器->client
            final Channel localSocksChannel = ctx.channel();
            String channelId = localSocksChannel.id().asLongText();
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

                        System.out.println("local socks channel init success " + localSocksChannel.id().asLongText());
//                        localSocksChannel.pipeline().remove(SocksServerConnectHandler.this);
//
//                        localSocksChannel.pipeline().addLast(new LocalHandler(request));
//
//                        RemoteConnectHandler.addLocalChannel(localSocksChannel.id().asLongText(), localSocksChannel);
                        // socks5原来客户端的实现是先用Promise 保证可以拿到 连接 dstAddr 的 Channel

                        // 通过 connectChannel 发送 IP和端口的信息 让 远端server 建立连接
                        // 获取到远端Server建立连接的信息
                        // 1. 远端建立连接成功 继续发送消息 2. 远端建立连接失败 回复给socksClient 失败消息 并关闭这个 localSocksChannel

                        // 构造一个 期望 服务器 创建连接的 包

                        Promise<Object> promiseConnect = future.channel().eventLoop().newPromise();

                        promiseConnect.addListener(new GenericFutureListener<Future<Object>>() {
                            @Override
                            public void operationComplete(Future<Object> future) throws Exception {
                                Object now = future.getNow();
                                if (future.isSuccess()) {
                                    localSocksChannel.pipeline().remove(SocksServerConnectHandler.this);
                                    localSocksChannel.pipeline().addLast(new LocalHandler(request));
                                }
                            }
                        });

                        ClientToServerContent promiseContent = new ClientToServerContent(
                                channelId,
                                RandomUtils.getPositiveInt(),
                                Option.connect,
                                request.dstAddr(),
                                request.dstPort(),
                                new byte[]{0}
                        );


                        RemoteConnectHandler.putLocalChannelInfo(localSocksChannel.id().asLongText(), new LocalChannelInfo(localSocksChannel, request));
                        // 期望 远程服务器连接
                        RemoteConnectHandler.sendPromiseConnectToRemoteServer(channelId, promiseContent, promiseConnect);

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

    /**
     * socks5失败的消息回复
     */
    public static void failureSocks5CommandResponse(Channel localSocksChannel, Socks5CommandRequest request) {
        localSocksChannel.writeAndFlush(new DefaultSocks5CommandResponse(
                Socks5CommandStatus.FAILURE, request.dstAddrType()));
        SocksServerUtils.closeOnFlush(localSocksChannel);
    }
}
