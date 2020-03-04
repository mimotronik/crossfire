package com.winterfell.common.protocol;

/**
 * 协议包
 *
 * @author winterfell
 */
public class TransferProtocol {

    private int random;

    private int len;

    private byte[] content;



    public TransferProtocol(int random, int len, byte[] content) {
        this.random = random;
        this.len = len;
        this.content = content;
    }

    public int getLen() {
        return len;
    }

    public void setLen(int len) {
        this.len = len;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }
}
