package com.winterfell.common.message;

import com.winterfell.common.utils.IpPortUtils;

/**
 * 信息解析
 *
 * @author winterfell
 */
public class ContentParser {

    /**
     * 解析 client -> server 的消息
     *
     * @param bytes
     * @return
     */
    public static ClientToServerContent parseC2S(byte[] bytes) {

        byte[] idLenBytes = new byte[4];
        System.arraycopy(bytes, 0,
                idLenBytes, 0, idLenBytes.length);
        int idLen = IpPortUtils.byteArrayToInt(idLenBytes);

        byte[] channelIdBytes = new byte[idLen];
        System.arraycopy(bytes, idLenBytes.length,
                channelIdBytes, 0, channelIdBytes.length);
        String channelId = new String(channelIdBytes);

        byte[] successBytes = new byte[4];
        System.arraycopy(bytes, idLenBytes.length + channelIdBytes.length,
                successBytes, 0, successBytes.length);
        int success = IpPortUtils.byteArrayToInt(successBytes);

        if (success < 0) {
            return new ClientToServerContent(channelId, success, null, -1, null);
        }

        byte[] addressLenBytes = new byte[4];
        System.arraycopy(bytes, idLenBytes.length + channelIdBytes.length + successBytes.length,
                addressLenBytes, 0, addressLenBytes.length);
        // 地址长度
        int addressLen = IpPortUtils.byteArrayToInt(addressLenBytes);

        byte[] addressBytes = new byte[addressLen];
        System.arraycopy(bytes, idLenBytes.length + channelIdBytes.length + successBytes.length + addressLenBytes.length,
                addressBytes, 0, addressBytes.length);
        String address = new String(addressBytes);

        byte[] portBytes = new byte[2];
        System.arraycopy(bytes, idLenBytes.length + channelIdBytes.length + successBytes.length + addressLenBytes.length + addressBytes.length,
                portBytes, 0, portBytes.length);
        int port = IpPortUtils.getPortByBytes(portBytes[0], portBytes[1]);

        int msgLen = bytes.length - (idLenBytes.length + channelIdBytes.length + successBytes.length + addressLenBytes.length + addressBytes.length + portBytes.length);
        byte[] msg = new byte[msgLen];
        System.arraycopy(bytes, idLenBytes.length + channelIdBytes.length + successBytes.length + addressLenBytes.length + addressBytes.length + portBytes.length,
                msg, 0, msg.length);

        return new ClientToServerContent(channelId, success, address, port, msg);
    }

    /**
     * 解析 server -> client 的消息
     *
     * @param bytes
     * @return
     */
    public static ServerToClientContent parseS2C(byte[] bytes) {

        byte[] idLenBytes = new byte[4];
        System.arraycopy(bytes, 0,
                idLenBytes, 0, idLenBytes.length);
        int idLen = IpPortUtils.byteArrayToInt(idLenBytes);

        byte[] channelIdBytes = new byte[idLen];
        System.arraycopy(bytes, idLenBytes.length,
                channelIdBytes, 0, channelIdBytes.length);
        String channelId = new String(channelIdBytes);

        byte[] successBytes = new byte[4];
        System.arraycopy(bytes, idLenBytes.length + channelIdBytes.length,
                successBytes, 0, successBytes.length);
        int success = IpPortUtils.byteArrayToInt(successBytes);

        if (success < 0) {
            return new ServerToClientContent(channelId, success, null);
        }

        int msgLen = bytes.length - (idLenBytes.length + channelIdBytes.length + successBytes.length);
        byte[] msg = new byte[msgLen];
        System.arraycopy(bytes, idLenBytes.length + channelIdBytes.length + successBytes.length,
                msg, 0, msg.length);
        return new ServerToClientContent(channelId, success, msg);
    }
}
