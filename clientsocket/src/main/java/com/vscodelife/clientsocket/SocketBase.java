package com.vscodelife.clientsocket;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import org.slf4j.Logger;

import com.vscodelife.clientsocket.component.ProtocolCatcher;
import com.vscodelife.socketio.message.base.HeaderBase;
import com.vscodelife.socketio.message.base.MessageBase;
import com.vscodelife.socketio.message.base.ProtocolKey;
import com.vscodelife.socketio.util.SnowflakeUtil;
import com.vscodelife.socketio.util.SnowflakeUtil.SnowflakeGenerator;
import com.vscodelife.socketio.util.profiler.ProfilerUtil;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;

public abstract class SocketBase<H extends HeaderBase, M extends MessageBase<H, B>, B>
        implements IClient<H, M, B> {

    private static final long DEFAULT_WARN_RCV_TIMESTAMP = 1000L;
    private static final long DEFAULT_ABANDON_RCV_TIMESTAMP = 5000L;

    protected final Logger logger;
    protected final Class<? extends ChannelInitializer<SocketChannel>> initializerClazz;

    protected final AtomicLong sessionId = new AtomicLong(0);
    protected final AtomicBoolean running = new AtomicBoolean(true);

    protected final Queue<M> messageQueue = new LinkedBlockingQueue<>();
    protected final Map<ProtocolKey, Consumer<M>> processMap = new ConcurrentHashMap<>();

    protected Connector<H, M, B> connector;

    protected final SnowflakeGenerator idGenerator = SnowflakeUtil.createGenerator(0);

    protected SocketBase(Logger logger,
            Class<? extends ChannelInitializer<SocketChannel>> initializerClazz) {
        this.logger = logger;
        this.initializerClazz = initializerClazz;
    }

    public abstract String getVersion();

    public abstract Class<? extends SocketBase<H, M, B>> getSocketClazz();

    @Override
    public Class<? extends ChannelInitializer<SocketChannel>> getInitializerClazz() {
        return initializerClazz;
    }

    public long getSessionId() {
        return sessionId.get();
    }

    public void setSessionId(long id) {
        sessionId.set(id);
    }

    public String getHostname() {
        String hostname = null;
        Connector<H, M, B> c = null;
        synchronized (this) {
            c = connector;
        }
        if (c != null) {
            hostname = c.getHostname();
        }
        return hostname;
    }

    public int getPort() {
        int port = -1;
        Connector<H, M, B> c = null;
        synchronized (this) {
            c = connector;
        }
        if (c != null) {
            port = c.getPort();
        }
        return port;
    }

    public boolean isRunning() {
        Connector<H, M, B> c = null;
        synchronized (this) {
            c = connector;
        }
        return c != null && c.isRunning();
    }

    public boolean isConnected() {
        Connector<H, M, B> c = null;
        synchronized (this) {
            c = connector;
        }
        return c != null && c.isConnected();
    }

    public boolean isConnecting() {
        Connector<H, M, B> c = null;
        synchronized (this) {
            c = connector;
        }
        return c != null && c.isConnecting();
    }

    public synchronized void reconnect(String hostname, int port) {
        if (connector != null) {
            connector.close();
            connector = null;
        }
        connector = new Connector<H, M, B>(nextId(), hostname, port, this);
        connector.connect();
    }

    public synchronized void connect(String hostname, int port) {
        if (connector != null) {
            connector.close();
            connector = null;
        }
        connector = new Connector<H, M, B>(nextId(), hostname, port, this);
        connector.connect();
    }

    public synchronized void disconnect() {
        if (connector != null) {
            connector.close();
            connector = null;
        }
    }

    public synchronized void shutdown() {
        running.set(false);

        if (connector != null) {
            connector.close();
            connector = null;
        }
    }

    @Override
    public void onConnected(long connectorId, ChannelHandlerContext ctx) {
        logger.info("connector={} connect to server", connectorId);
    }

    @Override
    public void onDisconnected(long connectorId, ChannelHandlerContext ctx) {
        logger.info("connector={} disconnect from server", connectorId);
    }

    @Override
    public void onSendMessage(long connectorId, ChannelHandlerContext ctx, M message) {
        logger.info("connector={} send message: {}", connectorId, message);
    }

    @Override
    public void onReceiveMessage(long connectorId, ChannelHandlerContext ctx, M message) {
        logger.info("connector={} receive message: {}", connectorId, message);

        putMessage(message);
    }

    @Override
    public void onException(long connectorId, ChannelHandlerContext ctx, Throwable cause) {
        logger.error("connector={} exception occurred: {}", connectorId, cause.getMessage(), cause);
    }

    public long nextId() {
        return idGenerator.nextId();
    }

    public long genRequestId() {
        return nextId();
    }

    protected void registerProtocol(int mainNo, int subNo, Consumer<M> handler) {
        registerProtocol(new ProtocolKey(mainNo, subNo), handler);
    }

    protected void registerProtocol(ProtocolKey key, Consumer<M> handler) {
        processMap.put(key, handler);
        logger.debug("Registered protocol {}-{}", key.getMainNo(), key.getSubNo());
    }

    @Override
    public void process() {
        try {
            while (!messageQueue.isEmpty()) {
                M message = popMessage();
                try {
                    dispatcher(message);
                } catch (Exception ee) {
                    logger.error(String.format("socket process protocol has error => %s", ee.getMessage()), ee);
                    onException(0, null, ee);
                }
            }
        } catch (Exception e) {
            logger.error(String.format("socket process has unknown error => %s", e.getMessage()), e);
            onException(0, null, e);
        }
    }

    private void putMessage(M message) {
        // 取得消息頭
        H header = message.getHeader();
        if (header == null) {
            logger.warn("Message has null header, it could not happen!");
            return;
        }
        // 設置接收時間戳
        header.setRcvTimestamp(System.currentTimeMillis());
        // 放入消息佇列
        messageQueue.add(message);
    }

    protected M popMessage() {
        return messageQueue.size() > 0 ? messageQueue.poll() : null;
    }

    /**
     * 安全的協議分發（增強錯誤處理）
     * 
     * @param message 消息
     */
    protected void dispatcherSafe(M message) {
        if (message == null) {
            logger.warn("Received null message, skipping dispatch");
            return;
        }

        HeaderBase header = message.getHeader();
        if (header == null) {
            logger.warn("Message has null header, skipping dispatch");
            return;
        }

        dispatcher(message);
    }

    protected void dispatcher(M message) {
        HeaderBase header = message.getHeader();
        long sessionId = header.getSessionId();
        long requestId = header.getRequestId();
        int mainNo = header.getMainNo();
        int subNo = header.getSubNo();
        ProtocolKey key = header.getProtocolKey();
        //
        Consumer<M> processor = processMap.get(key);
        if (processor != null) {
            String profilerName = "socket-dispatcher";
            String executeName = ProfilerUtil.executeStart(profilerName);
            try {
                processor.accept(message);
            } catch (Exception e) {
                logger.error(String.format("process message sessionId=%d requestId=%d protocol-%d-%d has error=%s",
                        sessionId, requestId, mainNo, subNo, e.getMessage()), e);
            } finally {
                if (ProfilerUtil.executeEnd(profilerName, executeName, 1000, true)) {
                    logger.info("handle serversocket dispatcher protocol-{}-{} too long", mainNo, subNo);
                }
            }
        } else {
            logger.info("protocol-{}-{} is not create", mainNo, subNo);
        }
    }

    protected Consumer<M> catchException(ProtocolCatcher<M, Exception> event) {
        return message -> {
            HeaderBase header = message.getHeader();
            long sessionId = header.getSessionId();
            long requestId = header.getRequestId();
            int mainNo = header.getMainNo();
            int subNo = header.getSubNo();
            long rcvTimestamp = header.getRcvTimestamp();
            try {
                long diff = System.currentTimeMillis() - rcvTimestamp;
                if (DEFAULT_ABANDON_RCV_TIMESTAMP < diff) {
                    logger.error("protocol-{}-{} sessionId={} requestId={} has too long delay -> {} ms, abandon it",
                            mainNo, subNo, sessionId, requestId, diff);
                } else if (DEFAULT_WARN_RCV_TIMESTAMP < diff) {
                    logger.warn("protocol-{}-{} sessionId={} requestId={} has too long delay -> {} ms",
                            mainNo, subNo, sessionId, requestId, diff);
                    event.accept(message);
                } else {
                    event.accept(message);
                }
            } catch (Exception e) {
                logger.error("protocol-{}-{} sessionId={} requestId={} has error => {}",
                        mainNo, subNo, sessionId, requestId, e.getMessage());
            }
        };
    }

    protected abstract M pack(String version, int mainNo, int subNo, long sessionId, long requestId, B buffer);

    protected void send(ProtocolKey protocol, B buffer) {
        send(protocol.getMainNo(), protocol.getSubNo(), genRequestId(), buffer);
    }

    protected void send(ProtocolKey protocol, long requestId, B buffer) {
        send(protocol.getMainNo(), protocol.getSubNo(), requestId, buffer);
    }

    protected void send(int mainNo, int subNo, B buffer) {
        send(mainNo, subNo, genRequestId(), buffer);
    }

    protected void send(int mainNo, int subNo, long requestId, B buffer) {
        String version = getVersion();
        try {
            if (connector != null && connector.isConnected()) {
                connector.send(
                        pack(version, mainNo, subNo, sessionId.get(), requestId, buffer));
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            onException(0, null, e);
        }
    }
}
