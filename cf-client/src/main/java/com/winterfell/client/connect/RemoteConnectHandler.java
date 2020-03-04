package com.winterfell.client.connect;

import com.winterfell.common.message.ClientToServerContent;
import com.winterfell.common.message.ContentPackage;
import com.winterfell.common.message.ContentParser;
import com.winterfell.common.message.ServerToClientContent;
import com.winterfell.common.protocol.TransferProtocol;
import com.winterfell.common.utils.RandomUtils;
import com.winterfell.common.utils.SocksServerUtils;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.util.ReferenceCountUtil;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author winterfell
 */
@ChannelHandler.Sharable
public class RemoteConnectHandler extends SimpleChannelInboundHandler<TransferProtocol> {

    /**
     * 连接远程的Channel
     */
    private static Channel connectChannel;

    /**
     * 存储本地的socks5的channel
     */
    private static Map<String, Channel> localChannelMap = new ConcurrentHashMap<>();


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("remote connect success ...");
        connectChannel = ctx.channel();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TransferProtocol msg) throws Exception {
        // 获取到远程服务器回复的消息
        byte[] receiveFromServer = msg.getContent();

        ServerToClientContent serverToClientContent = ContentParser.parseS2C(receiveFromServer);

        boolean success = serverToClientContent.getSuccess() > 0;

        // 远程服务器回复的是正确消息
        if (success) {

        } else {

        }

        sendMessageToLocal(serverToClientContent);

        ReferenceCountUtil.release(msg);
    }

    /**
     * 服务端发送过来的消息，回送给 socks 连接
     *
     * @param serverToClientContent
     */
    private static void sendMessageToLocal(ServerToClientContent serverToClientContent) {
        String channelId = serverToClientContent.getChannelId();
        Channel localChannel = localChannelMap.get(channelId);
        if (Objects.isNull(localChannel) || !localChannel.isActive()) {
            return;
        }
        boolean success = serverToClientContent.getSuccess() > 0;

        if (success) {
            if (localChannel.isActive()) {
                byte[] msg = serverToClientContent.getMsg();
                localChannel.writeAndFlush(Unpooled.copiedBuffer(msg));
            }
        } else {
            // 接收到服务端的消息是失败的消息
            SocksServerUtils.closeOnFlush(localChannel);
            removeLocalChannel(channelId);
        }

    }

    /**
     * 发送消息给外网的服务器
     *
     * @param clientToServerContent
     */
    public static boolean sendMessageToRemoteServer(ClientToServerContent clientToServerContent) {

        if (connectChannel.isActive()) {
            byte[] clientToRemoteContent = ContentPackage.pack(clientToServerContent);
            TransferProtocol transferProtocol = new TransferProtocol(
                    RandomUtils.getRandomInt(),
                    clientToRemoteContent.length,
                    clientToRemoteContent
            );
            connectChannel.writeAndFlush(transferProtocol);
            return true;
        } else {
            return false;
        }
    }

    /**
     * @param sign
     * @param localChannel
     */
    public static void addLocalChannel(String sign, Channel localChannel) {
        localChannelMap.put(sign, localChannel);
    }

    public static void removeLocalChannel(String sign) {
        localChannelMap.remove(sign);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

}
