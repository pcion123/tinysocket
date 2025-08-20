package com.vscodelife.socketio.connection;

import io.netty.channel.Channel;
import io.netty.channel.ChannelId;

public interface IConnection<B> {
    <T> T getProperty(Class<T> clazz, Channel channel, String property);

    <T> void setProperty(Class<T> clazz, Channel channel, String property, T value);

    Channel getChannel();

    void setChannel(Channel channel);

    int getVersion();

    void setVersion(int version);

    long getSessionId();

    void setSessionId(long sessionId);

    long getConnectTime();

    void setConnectTime(long connectTime);

    ChannelId getId();

    String getAddress();

    String getIp();

    int getPort();

    void disconnect();

    void send(int mainNo, int subNo, B buffer);
}
