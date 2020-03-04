package com.winterfell.client.connect;

import com.winterfell.client.handler.LocalChannelInfo;
import com.winterfell.client.handler.LocalHandler;
import com.winterfell.client.handler.SocksServerConnectHandler;
import com.winterfell.common.message.*;
import com.winterfell.common.protocol.TransferProtocol;
import com.winterfell.common.utils.RandomUtils;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.Promise;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author winterfell
 */
@ChannelHandler.Sharable
public class RemoteConnectHandler extends SimpleChannelInboundHandler<TransferProtocol> {

    /**
     * 连接远程的Channel 理论上只有一个 软件层面实现多路复用
     */
    private static Channel connectChannel;

    /**
     * 本地的包
     */
    private static Map<String, LocalChannelInfo> localSocksChannelInfos = new ConcurrentHashMap<>();

    private static Map<String, Promise<Object>> promiseConnectMap = new ConcurrentHashMap<>();


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("connect remote server success ...");
        connectChannel = ctx.channel();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TransferProtocol msg) throws Exception {
        byte[] receiveFromServer = msg.getContent();
        ServerToClientContent serverToClientContent = ContentParser.parseS2C(receiveFromServer);
        ReferenceCountUtil.release(msg);
        // 获取到远程服务器传回的消息并处理转发给本地
        sendMessageToLocal(serverToClientContent);
    }


    /**
     * 服务端发送过来的消息，回送给 socks 连接
     *
     * @param serverToClientContent
     */
    private static void sendMessageToLocal(ServerToClientContent serverToClientContent) {
        String channelId = serverToClientContent.getChannelId();
        LocalChannelInfo localChannelInfo = getLocalChannelInfo(channelId);
        Channel localSocksChannel = localChannelInfo.getChannel();

        boolean success = serverToClientContent.getSuccess() > 0;

        if (success && localSocksChannel.isActive()) {
            byte option = serverToClientContent.getOption();
            if (option == Option.connect) {
                // 消息是 服务端建立连接成功的消息
                // 接下来通知socks的连接可以发送内容了
                Promise<Object> promiseConnect = promiseConnectMap.remove(channelId);
                promiseConnect.setSuccess(null);
                return;
            } else if (option == Option.send) {
                byte[] msg = serverToClientContent.getMsg();
                localSocksChannel.writeAndFlush(Unpooled.copiedBuffer(msg));
            }
        } else {
            // 接收到服务端的消息是失败的消息
            if (localSocksChannel.isActive()) {
                SocksServerConnectHandler.failureSocks5CommandResponse(localSocksChannel, localChannelInfo.getRequest());
            }
            removeLocalChannelInfo(channelId);
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
     * 期望远程创建连接
     *
     * @return
     */
    public static void sendPromiseConnectToRemoteServer(String channelId,
                                                        ClientToServerContent promiseContent,
                                                        Promise<Object> promiseConnect) {
        if (connectChannel.isActive()) {
            byte[] pack = ContentPackage.pack(promiseContent);
            TransferProtocol transferProtocol = new TransferProtocol(
                    RandomUtils.getRandomInt(),
                    pack.length,
                    pack
            );

            promiseConnectMap.put(channelId, promiseConnect);

            connectChannel.writeAndFlush(transferProtocol);
        }
    }

    public static void removeLocalChannelInfo(String channelId) {
        localSocksChannelInfos.remove(channelId);
    }

    public static void putLocalChannelInfo(String channelId, LocalChannelInfo localChannelInfo) {
        localSocksChannelInfos.put(channelId, localChannelInfo);
    }

    public static LocalChannelInfo getLocalChannelInfo(String channelId) {
        return localSocksChannelInfos.get(channelId);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

}
