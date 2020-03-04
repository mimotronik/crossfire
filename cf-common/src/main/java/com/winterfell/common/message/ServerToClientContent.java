package com.winterfell.common.message;

/**
 * server -> client
 *
 * @author winterfell
 */
public class ServerToClientContent {
    //    // 4个字节
//    private int idLen;
    // idLen个字节
    private String channelId;
    // >0 success <0 fail
    private int success;
    private byte option;
    private byte[] msg;

    public ServerToClientContent(String channelId, int success, byte option, byte[] msg) {
        this.channelId = channelId;
        this.success = success;
        this.option = option;
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

    public byte[] getMsg() {
        return msg;
    }

    public void setMsg(byte[] msg) {
        this.msg = msg;
    }
}
