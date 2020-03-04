package com.winterfell.client.connect;

import com.winterfell.common.protocol.TransferDecoder;
import com.winterfell.common.protocol.TransferEncoder;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;

/**
 * @author winterfell
 */
public class RemoteChannelHandlerInit extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();

        // 解决tcp粘包问题
        pipeline.addLast(new TransferDecoder());
        pipeline.addLast(new TransferEncoder());

        pipeline.addLast(new RemoteConnectHandler());

    }
}
