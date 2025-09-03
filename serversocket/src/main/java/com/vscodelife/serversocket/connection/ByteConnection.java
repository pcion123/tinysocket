package com.vscodelife.serversocket.connection;

import java.net.InetSocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vscodelife.socketio.buffer.ByteArrayBuffer;
import com.vscodelife.socketio.connection.IConnection;
import com.vscodelife.socketio.constant.ProtocolId;
import com.vscodelife.socketio.util.DateUtil;

import io.netty.channel.Channel;
import io.netty.channel.ChannelId;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

public abstract class ByteConnection implements IConnection<ByteArrayBuffer> {
    protected static final Logger logger = LoggerFactory.getLogger(ByteConnection.class);

    protected Channel channel;
    protected String version;
    protected long sessionId;
    protected long connectTime;

    protected ByteConnection() {
        this(null, "0.0.1", 0L, 0L);
    }

    protected ByteConnection(Channel channel, String version, long sessionId, long connectTime) {
        this.channel = channel;
        this.version = version;
        this.sessionId = sessionId;
        this.connectTime = connectTime;
    }

    @Override
    public String toString() {
        String connectTimeStr = DateUtil.toTimeStr(getConnectTime());
        return String.format("Version=%s Id=%s SessionId=%d Address=%s ConnectTime=%s",
                getVersion(), getId(), getSessionId(), getAddress(), connectTimeStr);
    }

    @Override
    public <T> T getProperty(Class<T> clazz, Channel channel, String property) {
        AttributeKey<T> k = AttributeKey.valueOf(property);
        if (!channel.hasAttr(k)) {
            return null;
        }
        Attribute<T> v = channel.attr(k);
        return v.get();
    }

    @Override
    public <T> void setProperty(Class<T> clazz, Channel channel, String property, T value) {
        AttributeKey<T> k = AttributeKey.valueOf(property);
        Attribute<T> v = channel.attr(k);
        v.set(value);
    }

    @Override
    public Channel getChannel() {
        return channel;
    }

    @Override
    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public void setVersion(String version) {
        this.version = version;

        if (channel != null) {
            setProperty(String.class, channel, "version", version);
        }
    }

    @Override
    public long getSessionId() {
        return sessionId;
    }

    @Override
    public void setSessionId(long sessionId) {
        this.sessionId = sessionId;

        if (channel != null) {
            setProperty(long.class, channel, "sessionId", sessionId);
        }
    }

    @Override
    public long getConnectTime() {
        return connectTime;
    }

    @Override
    public void setConnectTime(long connectTime) {
        this.connectTime = connectTime;
    }

    @Override
    public ChannelId getId() {
        return channel != null ? channel.id() : null;
    }

    @Override
    public String getAddress() {
        String address = null;
        if (channel != null) {
            InetSocketAddress insocket = (InetSocketAddress) channel.remoteAddress();
            String ip = insocket.getAddress().toString().split("/")[1];
            int port = insocket.getPort();
            address = String.format("%s:%d", ip, port);
        }
        return address;
    }

    @Override
    public String getIp() {
        String ip = null;
        if (channel != null) {
            InetSocketAddress insocket = (InetSocketAddress) channel.remoteAddress();
            ip = insocket.getAddress().toString().split("/")[1];
        }
        return ip;
    }

    @Override
    public int getPort() {
        int port = 0;
        if (channel != null) {
            InetSocketAddress insocket = (InetSocketAddress) channel.remoteAddress();
            port = insocket.getPort();
        }
        return port;
    }

    @Override
    public void disconnect() {
        send(ProtocolId.DISCONNECT, new ByteArrayBuffer());

        if (channel != null) {
            channel.close();
        }
    }

    @Override
    public void destroy() {
        logger.debug("sessionId={} connection is destroyed", sessionId);
    }

    @Override
    public void sendServerBusyMessage(int mainNo, int subNo) {
        sendServerBusyMessage(mainNo, subNo, 0L);
    }

    @Override
    public void sendServerBusyMessage(int mainNo, int subNo, long requestId) {
        ByteArrayBuffer buffer = new ByteArrayBuffer();
        buffer.writeInt(503);
        buffer.writeString("server is busy");
        send(mainNo, subNo, requestId, buffer);
    }

    @Override
    public void send(int mainNo, int subNo, ByteArrayBuffer buffer) {
        send(mainNo, subNo, 0L, buffer);
    }
}
