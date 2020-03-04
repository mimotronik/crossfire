package com.winterfell.client.handler;

import io.netty.channel.Channel;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;

/**
 * @author winterfell
 */
public class LocalChannelInfo {
    private Channel channel;
    private Socks5CommandRequest request;

    public LocalChannelInfo(Channel channel, Socks5CommandRequest request) {
        this.channel = channel;
        this.request = request;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public Socks5CommandRequest getRequest() {
        return request;
    }

    public void setRequest(Socks5CommandRequest request) {
        this.request = request;
    }
}
