package com.winterfell.server.handler;

import com.winterfell.common.utils.IpPortUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;

/**
 * @author winterfell
 */
@ChannelHandler.Sharable
public class ReceiveHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println(ctx.channel().remoteAddress() + "connected ...");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println(ctx.channel().remoteAddress() + "disconnected ...");
        ctx.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println(ctx.channel().remoteAddress() + "disconnected ...");
        cause.printStackTrace();
        ctx.close();
    }

    //    @Override
    public void channelReadNew(ChannelHandlerContext ctx, Object msg) throws Exception {

        final Channel receiveChannel = ctx.channel();

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


        // 数据解密完得到解密后的消息


        // 连接Google
        Bootstrap visit = new Bootstrap();

        visit.group(receiveChannel.eventLoop())
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new VisitHandler(receiveChannel, Unpooled.copiedBuffer(transferDataBytes)));

        visit.connect(addr, port).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (!future.isSuccess()) {
                    ctx.close();
                }
            }
        });
    }

    //    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        final Channel receiveChannel = ctx.channel();

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

        /*
        byte[] transferDataBytes = new byte[dataBytes.length
                - (1 + addrLenBytes.length + addrBytes.length + portBytes.length)];
        System.arraycopy(dataBytes, 1 + addrLenBytes.length + addrBytes.length + portBytes.length,
                transferDataBytes, 0, transferDataBytes.length);
         */

        Promise<Channel> redirectChannelPromise = ctx.executor().newPromise();

        redirectChannelPromise.addListener(new FutureListener<Channel>() {

            @Override
            public void operationComplete(Future<Channel> future) throws Exception {
                final Channel redirectChannel = future.getNow();
                if (future.isSuccess()) {


                    // 这句话有很大的问题
                    receiveChannel.pipeline().remove(ReceiveHandler.this);

                    // 客户端连接发过来的消息，解密后，让与 google 连接的 channel 转发一下
                    receiveChannel.pipeline().addLast(new TransferHandler(redirectChannel, addr, port));

                    redirectChannel.pipeline().addLast(new BackToClientHandler(receiveChannel, addr, port));

                    // 转发消息到google
                    receiveChannel.pipeline()
                            .fireChannelRead(Unpooled.copiedBuffer(dataBytes));

                } else {
                    receiveChannel.close();
                }
            }
        });

        System.out.println("create link to " + addr);

        Bootstrap visit = new Bootstrap();

        visit.group(receiveChannel.eventLoop())
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new DirectHandler(redirectChannelPromise));


        visit.connect(addr, port).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (!future.isSuccess()) {
                    // 访问不到资源 直接关闭 内外网的连接
                    ctx.close();
                }
            }
        });

    }

}
