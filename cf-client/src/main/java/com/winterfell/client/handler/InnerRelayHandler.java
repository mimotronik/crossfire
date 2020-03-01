package com.winterfell.client.handler;

import com.winterfell.common.utils.IpPortUtils;
import com.winterfell.common.utils.SocksServerUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.socksx.v5.Socks5AddressType;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import io.netty.util.ReferenceCountUtil;

/**
 * @author winterfell
 */
@ChannelHandler.Sharable
public class InnerRelayHandler extends ChannelInboundHandlerAdapter {
    private final Channel outboundChannel;
    private Socks5CommandRequest request;


    public InnerRelayHandler(Channel outboundChannel, Socks5CommandRequest request) {
        this.outboundChannel = outboundChannel;
        this.request = request;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ctx.writeAndFlush(Unpooled.EMPTY_BUFFER);
    }


    /**
     * 客户端收到的消息 , 连接端加密转发到外面
     *
     * @param ctx
     * @param msg
     */
//    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (outboundChannel.isActive()) {

            ByteBuf buf = (ByteBuf) msg;
            System.out.println("客户端 接收到Socks5的消息 " + ctx.channel().remoteAddress() + " " + buf.readableBytes());
            System.out.println(request);
            byte[] bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);
            for (byte aByte : bytes) {
                System.out.print(new Byte(aByte).intValue() + ",");
            }
            System.out.println("\r\n");

            ByteBuf data = combine(request, Unpooled.copiedBuffer(bytes));
            outboundChannel.writeAndFlush(data);
        } else {
            ReferenceCountUtil.release(msg);
        }
    }

    /**
     * 客户端收到的消息 , 连接端加密转发到外面
     *
     * @param ctx
     * @param msg
     */
    // @Override
    public void channelReadOld(ChannelHandlerContext ctx, Object msg) {

        ByteBuf buf = (ByteBuf) msg;

        System.out.println("客户端 接收到Socks5的消息 " + ctx.channel().remoteAddress() + " " + buf.readableBytes());
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        for (byte aByte : bytes) {
            System.out.print(new Byte(aByte).intValue() + ",");
        }
        System.out.println();
        System.out.println(request);
        System.out.println();

        if (outboundChannel.isActive()) {
            // 发送给远程的信息
            ByteBuf data = Unpooled.copiedBuffer(bytes);
            outboundChannel.writeAndFlush(data);
            // outboundChannel.writeAndFlush(msg);
        } else {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        if (outboundChannel.isActive()) {
            System.out.println("channel inactive ..");
            SocksServerUtils.closeOnFlush(outboundChannel);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    /**
     * 基本信息和数据信息组合
     *
     * @param request
     * @param msg
     * @return
     */
    private ByteBuf combine(Socks5CommandRequest request, ByteBuf msg) {

        Socks5AddressType addressType = request.dstAddrType();
        String dstAddr = request.dstAddr();
        int port = request.dstPort();

        byte addressTypeByte = addressType.byteValue();
        byte[] addrBytes = dstAddr.getBytes();
        int addrBytesLen = addrBytes.length;
        // 4 个字节
        byte[] addrLenBytes = IpPortUtils.intToByteArray(addrBytesLen);
        // 端口字节数组
        byte[] portBytes = IpPortUtils.getBytesByPort(port);
        // 数据byte
        byte[] msgBytes = new byte[msg.readableBytes()];
        msg.readBytes(msgBytes);

        // 地址类型，地址长度，地址，端口
        int combineLen = 1 + addrLenBytes.length + addrBytes.length + portBytes.length + msgBytes.length;

        byte[] combineBytes = new byte[combineLen];

        // 字节 1 地址类型
        combineBytes[0] = addressTypeByte;
        // 字节 4 地址长度 int -- bytes
        System.arraycopy(addrLenBytes, 0, combineBytes, 1, addrLenBytes.length);
        // 字节  地址 转 byte[]
        System.arraycopy(addrBytes, 0, combineBytes, 1 + addrLenBytes.length, addrBytes.length);
        // 端口字节
        System.arraycopy(portBytes, 0, combineBytes, 1 + addrLenBytes.length + addrBytes.length, portBytes.length);
        // msg
        System.arraycopy(msgBytes, 0, combineBytes, 1 + addrLenBytes.length + addrBytes.length + portBytes.length, msgBytes.length);

        return Unpooled.copiedBuffer(combineBytes);
    }


    /*

       The SOCKS request is formed as follows:

        +----+-----+-------+------+----------+----------+
        |VER | CMD |  RSV  | ATYP | DST.ADDR | DST.PORT |
        +----+-----+-------+------+----------+----------+
        | 1  |  1  | X'00' |  1   | Variable |    2     |
        +----+-----+-------+------+----------+----------+

     */

}
