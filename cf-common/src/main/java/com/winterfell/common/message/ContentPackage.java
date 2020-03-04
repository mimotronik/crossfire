package com.winterfell.common.message;

import com.winterfell.common.utils.IpPortUtils;

/**
 * 信息包装
 *
 * @author winterfell
 */
public class ContentPackage {

    /**
     * 信息封装
     *
     * @param clientToServerContent
     * @return
     */
    public static byte[] pack(ClientToServerContent clientToServerContent) {

        String channelId = clientToServerContent.getChannelId();
        int success = clientToServerContent.getSuccess();
        String address = clientToServerContent.getAddress();
        int port = clientToServerContent.getPort();
        byte[] msg = clientToServerContent.getMsg();


        byte[] channelIdBytes = channelId.getBytes();
        int idLen = channelId.length();
        byte[] idLenBytes = IpPortUtils.intToByteArray(idLen);

        byte[] successBytes = IpPortUtils.intToByteArray(success);

        byte[] addressBytes = address.getBytes();
        int addressLen = addressBytes.length;
        byte[] addressLenBytes = IpPortUtils.intToByteArray(addressLen);

        byte[] portBytes = IpPortUtils.getBytesByPort(port);

        byte[] res = new byte[
                idLenBytes.length
                        + channelIdBytes.length
                        + successBytes.length
                        + addressLenBytes.length
                        + addressBytes.length
                        + portBytes.length
                        + msg.length
                ];
        System.arraycopy(idLenBytes, 0,
                res, 0,
                idLenBytes.length);

        System.arraycopy(channelIdBytes, 0,
                res, idLenBytes.length,
                channelIdBytes.length);

        System.arraycopy(successBytes, 0,
                res, idLenBytes.length + channelIdBytes.length
                , successBytes.length);

        System.arraycopy(addressLenBytes, 0,
                res, idLenBytes.length + channelIdBytes.length + successBytes.length,
                addressLenBytes.length);

        System.arraycopy(addressBytes, 0,
                res, idLenBytes.length + channelIdBytes.length + successBytes.length + addressLenBytes.length,
                addressBytes.length);

        System.arraycopy(portBytes, 0,
                res, idLenBytes.length + channelIdBytes.length + successBytes.length + addressLenBytes.length + addressBytes.length,
                portBytes.length);

        System.arraycopy(msg, 0,
                res, idLenBytes.length + channelIdBytes.length + successBytes.length + addressLenBytes.length + addressBytes.length + portBytes.length,
                msg.length);
        return res;
    }


    public static byte[] pack(ServerToClientContent serverToClientContent) {
        String channelId = serverToClientContent.getChannelId();
        int success = serverToClientContent.getSuccess();
        byte[] msg = serverToClientContent.getMsg();

        byte[] channelIdBytes = channelId.getBytes();
        int idLen = channelId.length();
        byte[] idLenBytes = IpPortUtils.intToByteArray(idLen);

        byte[] successBytes = IpPortUtils.intToByteArray(success);


        byte[] packBytes = new byte[
                idLenBytes.length
                        + channelIdBytes.length
                        + successBytes.length
                        + msg.length
                ];

        System.arraycopy(
                idLenBytes, 0,
                packBytes, 0,
                idLenBytes.length
        );

        System.arraycopy(
                channelIdBytes, 0,
                packBytes, idLenBytes.length,
                channelIdBytes.length
        );

        System.arraycopy(
                successBytes, 0,
                packBytes, idLenBytes.length + channelIdBytes.length,
                successBytes.length
        );

        System.arraycopy(
                msg, 0,
                packBytes, idLenBytes.length + channelIdBytes.length + successBytes.length,
                msg.length
        );

        return packBytes;
    }
}
