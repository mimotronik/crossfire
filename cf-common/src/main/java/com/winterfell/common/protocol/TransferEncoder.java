package com.winterfell.common.protocol;

import com.winterfell.common.utils.RandomUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * @author winterfell
 */
public class TransferEncoder extends MessageToByteEncoder<TransferProtocol> {
    @Override
    protected void encode(ChannelHandlerContext ctx, TransferProtocol msg, ByteBuf out) throws Exception {
        out.writeInt(RandomUtils.getRandomInt());
        out.writeInt(msg.getLen());
        out.writeBytes(msg.getContent());
    }

}
