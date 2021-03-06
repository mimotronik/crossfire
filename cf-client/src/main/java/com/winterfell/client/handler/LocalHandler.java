package com.winterfell.client.handler;

import com.winterfell.client.connect.RemoteConnectHandler;
import com.winterfell.common.message.ClientToServerContent;
import com.winterfell.common.message.Option;
import com.winterfell.common.utils.RandomUtils;
import com.winterfell.common.utils.SocksServerUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandResponse;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
import io.netty.util.ReferenceCountUtil;

/**
 * @author winterfell
 */
@ChannelHandler.Sharable
public class LocalHandler extends ChannelInboundHandlerAdapter {

    private Socks5CommandRequest request;

    public LocalHandler(Socks5CommandRequest request) {
        this.request = request;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("local socks channel connected " + ctx.channel().id().asLongText());
    }

    /**
     * 本地读取到浏览器发送的消息
     *
     * @param ctx
     * @param msg
     * @throws Exception
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        String channelId = ctx.channel().id().asLongText();
        int success = RandomUtils.getPositiveInt();
        String address = request.dstAddr();
        int port = request.dstPort();

        ByteBuf buf = (ByteBuf) msg;
        byte[] msgBytes = new byte[buf.readableBytes()];
        buf.readBytes(msgBytes);

        // 释放 防止内存溢出
        ReferenceCountUtil.release(buf);

        // 封装成 ClientToServerContent
        ClientToServerContent clientToServerContent = new ClientToServerContent(channelId, success, Option.send, address, port, msgBytes);
        // 服务器得到的消息发送给server
        RemoteConnectHandler.sendMessageToRemoteServer(clientToServerContent);

    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        //  TODO 本地的请求关闭了，需要通知远端相应关闭请求
        System.out.println("local socks channel is closed " + ctx.channel().id().asLongText());
        this.createFailContentAndSend(ctx);
        ctx.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        this.createFailContentAndSend(ctx);
        cause.printStackTrace();
        ctx.close();
    }

    /**
     * 构造一个失败的消息发送给服务端 主要是为了通知关闭连接
     *
     * @param ctx
     */
    private void createFailContentAndSend(ChannelHandlerContext ctx) {
        ClientToServerContent failContent = new ClientToServerContent(
                ctx.channel().id().asLongText(),
                RandomUtils.getNegativeInt(),
                Option.other,
                request.dstAddr(),
                request.dstPort(),
                new byte[]{0}
        );

        RemoteConnectHandler.sendMessageToRemoteServer(failContent);
    }

}
