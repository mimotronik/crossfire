package com.winterfell.client.config;

/**
 * @author winterfell
 */
public class RemoteServerConfig {
    private String addr;
    private int port;

    public RemoteServerConfig(String addr, int port) {
        this.addr = addr;
        this.port = port;
    }

    public RemoteServerConfig() {
    }

    public String getAddr() {
        return addr;
    }

    public void setAddr(String addr) {
        this.addr = addr;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
