package com.winterfell.common.message;

import io.netty.buffer.ByteBuf;

/**
 * 定义client -> server 传输的消息结构
 *
 * @author winterfell
 */
public class ClientToServerContent {
    //    // 4个字节
//    private int idLen;
    // idLen个字节
    private String channelId;
    // >0 success <0 fail
    private int success;
    private byte option;
    //    private int addressLength;
    private String address;
    private int port;
    private byte[] msg;

    public ClientToServerContent(String channelId, int success, byte option, String address, int port, byte[] msg) {
        this.channelId = channelId;
        this.success = success;
        this.option = option;
        this.address = address;
        this.port = port;
        this.msg = msg;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public int getSuccess() {
        return success;
    }

    public void setSuccess(int success) {
        this.success = success;
    }

    public byte getOption() {
        return option;
    }

    public void setOption(byte option) {
        this.option = option;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public byte[] getMsg() {
        return msg;
    }

    public void setMsg(byte[] msg) {
        this.msg = msg;
    }
}
