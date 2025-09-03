package com.vscodelife.socketio.connection;

import com.vscodelife.socketio.message.base.ProtocolKey;

import io.netty.channel.Channel;
import io.netty.channel.ChannelId;

public interface IConnection<B> {
    <T> T getProperty(Class<T> clazz, Channel channel, String property);

    <T> void setProperty(Class<T> clazz, Channel channel, String property, T value);

    Channel getChannel();

    void setChannel(Channel channel);

    String getVersion();

    void setVersion(String version);

    long getSessionId();

    void setSessionId(long sessionId);

    long getConnectTime();

    void setConnectTime(long connectTime);

    ChannelId getId();

    String getAddress();

    String getIp();

    int getPort();

    void disconnect();

    void destroy();

    default void sendServerBusyMessage(ProtocolKey protocol) {
        sendServerBusyMessage(protocol.getMainNo(), protocol.getSubNo());
    }

    default void sendServerBusyMessage(ProtocolKey protocol, long requestId) {
        sendServerBusyMessage(protocol.getMainNo(), protocol.getSubNo(), requestId);
    }

    void sendServerBusyMessage(int mainNo, int subNo);

    void sendServerBusyMessage(int mainNo, int subNo, long requestId);

    default void send(ProtocolKey protocol, B buffer) {
        send(protocol.getMainNo(), protocol.getSubNo(), buffer);
    }

    default void send(ProtocolKey protocol, long requestId, B buffer) {
        send(protocol.getMainNo(), protocol.getSubNo(), requestId, buffer);
    }

    void send(int mainNo, int subNo, B buffer);

    void send(int mainNo, int subNo, long requestId, B buffer);
}
