package com.winterfell.client.connect;

import com.winterfell.client.config.RemoteServerConfig;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.InetSocketAddress;

/**
 * TODO 里面有个bug不容易发现就是要先连接上客户端 Socks5的服务端才有用 异步
 *
 * @author winterfell
 */
public class RemoteConnectStarter {

    private Bootstrap bootstrap;

    private RemoteServerConfig remoteConfig;

    public RemoteConnectStarter(RemoteServerConfig remoteConfig) {
        this.remoteConfig = remoteConfig;
        this.bootstrap = new Bootstrap();
    }

    /**
     * 启动远程连接
     *
     * @param group
     * @return
     * @throws Exception
     */
    public RemoteConnectStarter start(EventLoopGroup group) throws Exception {
        bootstrap.group(group).channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new RemoteChannelHandlerInit());
                    }
                });

        // 不需要异步连接
        System.out.println("try to connect remote server ...");
        // connect 操作是在线程里面操作的
        ChannelFuture channelFuture = bootstrap.connect(new InetSocketAddress(remoteConfig.getAddr(), remoteConfig.getPort()));
        channelFuture.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                System.out.println("connect remote server " + (future.isSuccess() ? "success" : "fail"));
            }
        });
        return this;
    }
}
