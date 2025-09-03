package com.vscodelife.clientsocket;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;

import com.vscodelife.socketio.constant.ProtocolId;
import com.vscodelife.socketio.message.JsonMessage;
import com.vscodelife.socketio.message.base.HeaderBase;
import com.vscodelife.socketio.util.ExecutorUtil;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;

public abstract class JsonSocket<H extends HeaderBase> extends SocketBase<H, JsonMessage<H>, String> {
    protected final ScheduledExecutorService scheduledThread = Executors
            .newSingleThreadScheduledExecutor(ExecutorUtil.makeName("clientsocketschedulepool"));

    private static final int DEFAULT_MAX_RECONNECT_ATTEMPTS = 5;
    private static final int DEFAULT_RECONNECT_INTERVAL = 5;
    private static final int DEFAULT_PING_INTERVAL = 30;

    // 重連配置
    protected volatile boolean autoReconnect = false;
    protected volatile int maxReconnectAttempts = DEFAULT_MAX_RECONNECT_ATTEMPTS;
    protected volatile long reconnectInterval = DEFAULT_RECONNECT_INTERVAL;
    protected volatile int currentReconnectAttempts = 0;
    protected ScheduledFuture<?> autoScheduledFuture;

    private long pingSend;
    private long pingRcv;
    private long pingValue;
    private ScheduledFuture<?> pingScheduledFuture;

    protected JsonSocket(Logger logger,
            Class<? extends ChannelInitializer<SocketChannel>> initializerClazz) {
        super(logger, initializerClazz);

        protocolRegister.registerProtocol(ProtocolId.PING, catchException(message -> ping(message)));
        protocolRegister.registerProtocol(ProtocolId.DISCONNECT, catchException(message -> disconnected(message)));
    }

    @Override
    public synchronized void connect(String hostname, int port) {
        super.connect(hostname, port);

        if (autoReconnect) {
            if (autoScheduledFuture != null) {
                autoScheduledFuture.cancel(true);
                autoScheduledFuture = null;
            }
            autoScheduledFuture = scheduledThread.scheduleAtFixedRate(new AutoReconnect(),
                    reconnectInterval,
                    reconnectInterval,
                    TimeUnit.SECONDS);
        }
        if (pingScheduledFuture != null) {
            pingScheduledFuture.cancel(true);
            pingScheduledFuture = null;
        }
    }

    @Override
    public synchronized void disconnect() {
        super.disconnect();

        if (autoReconnect && autoScheduledFuture != null) {
            autoScheduledFuture.cancel(true);
            autoScheduledFuture = null;
        }
        if (pingScheduledFuture != null) {
            pingScheduledFuture.cancel(true);
            pingScheduledFuture = null;
        }
    }

    @Override
    public synchronized void shutdown() {
        super.shutdown();

        if (autoReconnect && autoScheduledFuture != null) {
            autoScheduledFuture.cancel(true);
            autoScheduledFuture = null;
        }
        if (pingScheduledFuture != null) {
            pingScheduledFuture.cancel(true);
            pingScheduledFuture = null;
        }

        scheduledThread.shutdown();
    }

    @Override
    public void onDisconnected(long connectorId, ChannelHandlerContext ctx) {
        super.onDisconnected(connectorId, ctx);

        if (pingScheduledFuture != null) {
            pingScheduledFuture.cancel(true);
            pingScheduledFuture = null;
        }
    }

    public boolean isAutoReconnect() {
        return autoReconnect;
    }

    public long getPing() {
        return pingValue;
    }

    public void ping() {
        if (isConnected()) {
            if (pingScheduledFuture != null) {
                pingScheduledFuture.cancel(true);
                pingScheduledFuture = null;
            }
            pingScheduledFuture = scheduledThread.scheduleAtFixedRate(new Ping(),
                    DEFAULT_PING_INTERVAL,
                    DEFAULT_PING_INTERVAL,
                    TimeUnit.SECONDS);
        }
    }

    protected void ping(JsonMessage<H> message) {
        pingRcv = System.currentTimeMillis();
        pingValue = pingRcv - pingSend;
    }

    protected void disconnected(JsonMessage<H> message) {
        long sessionId = message.getSessionId();
        long requestId = message.getRequestId();
        logger.info("sessionId={} requestId={} rcv server notify disconnect", sessionId, requestId);
    }

    protected class AutoReconnect implements Runnable {
        @Override
        public void run() {
            try {
                if (autoReconnect && !isConnected()) {
                    if (maxReconnectAttempts <= 0 || currentReconnectAttempts < maxReconnectAttempts) {
                        currentReconnectAttempts++;
                        logger.info("Auto reconnect attempt {}/{}", currentReconnectAttempts, maxReconnectAttempts);

                        String hostname = getHostname();
                        int port = getPort();
                        reconnect(hostname, port);
                    } else {
                        logger.warn("Max reconnect attempts reached. Stopping auto-reconnect.");
                        autoReconnect = false;
                        if (autoScheduledFuture != null) {
                            autoScheduledFuture.cancel(true);
                            autoScheduledFuture = null;
                        }
                    }
                } else if (isConnected()) {
                    currentReconnectAttempts = 0; // Reset on successful connection
                }
            } catch (Exception e) {
                logger.error("Error during auto-reconnect: {}", e.getMessage());
            }
        }
    }

    protected class Ping implements Runnable {
        @Override
        public void run() {
            try {
                if (isConnected()) {
                    pingSend = System.currentTimeMillis();
                    send(ProtocolId.PING, "");
                }
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }
    }
}
