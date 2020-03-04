package com.winterfell.client;

import com.winterfell.client.config.RemoteServerConfig;
import com.winterfell.client.connect.RemoteConnectStarter;
import com.winterfell.client.handler.SocksServerInitializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;


/**
 * @author winterfell
 */
public class Client {

    private static final int PORT = 12306;

    public static final RemoteServerConfig remoteConf = new RemoteServerConfig("127.0.0.1", 9001);

    public static void main(String[] args) throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup(3);
        try {

            new RemoteConnectStarter(remoteConf)
                    .start(workerGroup);

            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new SocksServerInitializer());
            System.out.println("local socks5 listen in " + PORT);
            b.bind(PORT).sync().channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
