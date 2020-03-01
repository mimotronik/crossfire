package com.winterfell.server.handler;

import com.winterfell.common.utils.IpPortUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

/**
 * @author winterfell
 */
@ChannelHandler.Sharable
public class TransferHandler extends ChannelInboundHandlerAdapter {
    private final Channel redirectChannel;
    private String addr;
    private int port;

    public TransferHandler(Channel redirectChannel) {
        this.redirectChannel = redirectChannel;
    }

    public TransferHandler(Channel redirectChannel, String addr, int port) {
        this.redirectChannel = redirectChannel;
        this.addr = addr;
        this.port = port;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (redirectChannel.isActive()) {
            ByteBuf data = (ByteBuf) msg;
            if (data.readableBytes() <= 0) {
                return;
            }
            byte[] dataBytes = new byte[data.readableBytes()];
            data.readBytes(dataBytes);
            // TODO 这里要解密一次 解密(dataBytes)

            byte[] addrLenBytes = new byte[4];
            System.arraycopy(dataBytes, 1, addrLenBytes, 0, 4);

            // 域名或者ip的长度
            int addrLen = IpPortUtils.byteArrayToInt(addrLenBytes);
            byte[] addrBytes = new byte[addrLen];
            System.arraycopy(dataBytes, 1 + addrLenBytes.length,
                    addrBytes, 0, addrBytes.length);

            String addr = new String(addrBytes);

            byte[] portBytes = new byte[2];
            System.arraycopy(dataBytes, 1 + addrLenBytes.length + addrBytes.length,
                    portBytes, 0, portBytes.length);
            int port = IpPortUtils.getPortByBytes(portBytes[0], portBytes[1]);

            System.out.println("address : " + addr + " port: " + port);
            byte[] transferDataBytes = new byte[dataBytes.length
                    - (1 + addrLenBytes.length + addrBytes.length + portBytes.length)];
            System.arraycopy(dataBytes, 1 + addrLenBytes.length + addrBytes.length + portBytes.length,
                    transferDataBytes, 0, transferDataBytes.length);

            System.out.println("连接 " + addr + ":" + port + " 发送消息 " + ((ByteBuf) msg).readableBytes());


            redirectChannel.writeAndFlush(Unpooled.copiedBuffer(transferDataBytes));
        } else {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
    }
}
