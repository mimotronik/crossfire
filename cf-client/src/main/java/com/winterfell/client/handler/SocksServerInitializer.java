package com.winterfell.client.handler;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.socksx.SocksPortUnificationServerHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

/**
 * @author winterfell
 */
public final class SocksServerInitializer extends ChannelInitializer<SocketChannel> {

    @Override
    public void initChannel(SocketChannel ch) throws Exception {
        ch.pipeline().addLast(
                new LoggingHandler(LogLevel.DEBUG),
                new SocksPortUnificationServerHandler(),
                SocksServerHandler.INSTANCE);
    }

}
