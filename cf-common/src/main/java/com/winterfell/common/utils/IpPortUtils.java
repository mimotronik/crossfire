package com.winterfell.common.utils;

/**
 * @author winterfell
 */
public class IpPortUtils {

    private IpPortUtils() {

    }

    /**
     * 根据字节数字获取到端口
     *
     * @param a
     * @param b
     * @return
     */
    public static int getPortByBytes(byte a, byte b) {
        return (a & 0xff) << 8 | (b & 0xff);
    }

    /**
     * 根据端口获取到字节数组
     *
     * @param port
     * @return
     */
    public static byte[] getBytesByPort(int port) {
        byte[] bytes = new byte[4];
        bytes[0] = (byte) ((port >> 24) & 0xff);
        bytes[1] = (byte) ((port >> 16) & 0xff);
        bytes[2] = (byte) ((port >> 8) & 0xff);
        bytes[3] = (byte) (port & 0xff);
        byte[] portBytes = new byte[2];
        portBytes[0] = bytes[2];
        portBytes[1] = bytes[3];
        return portBytes;
    }


    //byte 数组与 int 的相互转换
    public static int byteArrayToInt(byte[] b) {
        return b[3] & 0xFF |
                (b[2] & 0xFF) << 8 |
                (b[1] & 0xFF) << 16 |
                (b[0] & 0xFF) << 24;
    }

    public static byte[] intToByteArray(int a) {
        return new byte[]{
                (byte) ((a >> 24) & 0xFF),
                (byte) ((a >> 16) & 0xFF),
                (byte) ((a >> 8) & 0xFF),
                (byte) (a & 0xFF)
        };
    }


    public static void main(String[] args) {

        int port = 9001;

        System.out.println(byteArrayToInt(intToByteArray(port)));


    }

}
