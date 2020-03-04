package com.winterfell.common.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;

import java.util.List;

/**
 * @author winterfell
 */
public class TransferDecoder extends ReplayingDecoder<Void> {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        // 需要将得到的字节 转成msg
        int random = in.readInt();
        int len = in.readInt();
        byte[] content = new byte[len];
        in.readBytes(content);

        // 封装成msg对象 放入到 out 交个下一个handler处理
        TransferProtocol transferProtocol = new TransferProtocol(random, len, content);
        out.add(transferProtocol);
    }
}
