package com.vscodelife.serversocket;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import org.slf4j.Logger;

import com.vscodelife.serversocket.component.ProtocolCatcher;
import com.vscodelife.serversocket.component.ProtocolRegister;
import com.vscodelife.serversocket.component.RateLimiter;
import com.vscodelife.socketio.connection.IConnection;
import com.vscodelife.socketio.message.base.CacheBase;
import com.vscodelife.socketio.message.base.HeaderBase;
import com.vscodelife.socketio.message.base.MessageBase;
import com.vscodelife.socketio.message.base.ProtocolKey;
import com.vscodelife.socketio.util.DateUtil;
import com.vscodelife.socketio.util.profiler.ProfilerUtil;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

/**
 * Socket 基底抽象類別
 * 
 * @param <H> Header 型別，必須繼承 HeaderBase
 * @param <C> Connection 型別，必須實現 IConnection 且使用相同的 Buffer 型別
 * @param <M> Message 型別，必須繼承 MessageBase 且使用相同的 Buffer 型別
 * @param <B> Buffer 型別，用於數據傳輸
 */
public abstract class SocketBase<H extends HeaderBase, C extends IConnection<B>, M extends MessageBase<H, B>, B>
        implements Runnable {

    private static final long DEFAULT_WARN_RCV_TIMESTAMP = 1000L;
    private static final long DEFAULT_ABANDON_RCV_TIMESTAMP = 5000L;

    protected static final int DEFAULT_UPDATE_CONNECTION_INTERVAL = 60;
    protected static final int DEFAULT_UPDATE_CACHE_MANAGER_INTERVAL = 60;

    protected final Logger logger;
    protected final Class<? extends ChannelInitializer<SocketChannel>> initializerClazz;
    protected final Map<Long, C> connectionMap = new ConcurrentHashMap<Long, C>();
    protected final int port;

    protected final int limitConnect;
    protected final AtomicInteger maxConnect = new AtomicInteger(0);
    protected final AtomicInteger nowConnect = new AtomicInteger(0);
    protected final AtomicLong sessionId = new AtomicLong(0);
    protected final AtomicBoolean running = new AtomicBoolean(true);

    protected final Queue<M> messageQueue = new LinkedBlockingQueue<>();

    protected EventLoopGroup bossGroup;
    protected EventLoopGroup workerGroup;
    protected ServerBootstrap bootStrap;
    protected Channel channel;

    protected final RateLimiter rateLimiter = new RateLimiter();
    protected final CacheBase<M, B> cacheManager;
    protected final ProtocolRegister<H, C, M, B> protocolRegister;

    protected SocketBase(Logger logger, int port, int limitConnect,
            Class<? extends ChannelInitializer<SocketChannel>> initializerClazz) {
        // 參數驗證
        if (logger == null) {
            throw new IllegalArgumentException("Logger cannot be null");
        }
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("Port must be between 1 and 65535, got: " + port);
        }
        if (limitConnect <= 0) {
            throw new IllegalArgumentException("Limit connect must be positive, got: " + limitConnect);
        }
        if (initializerClazz == null) {
            throw new IllegalArgumentException("Initializer class cannot be null");
        }

        this.logger = logger;
        this.initializerClazz = initializerClazz;
        this.limitConnect = limitConnect;
        this.port = port;

        this.cacheManager = createCacheInstance();

        // 初始化協議註冊器（明確指定泛型類型）
        this.protocolRegister = new ProtocolRegister<H, C, M, B>(
                this.cacheManager,
                this::getConnection, // 連接提供者
                handler -> catchException(handler::accept) // 異常處理器適配器
        );

        logger.info("Initialized SocketBase with port={}, limitConnect={}, initializer={}",
                port, limitConnect, initializerClazz.getSimpleName());
    }

    public abstract Class<? extends SocketBase<H, C, M, B>> getSocketClazz();

    public boolean isBinding() {
        return channel != null ? channel.isActive() : false;
    }

    public long genSessionId() {
        if (sessionId.longValue() > Long.MAX_VALUE) {
            sessionId.set(0);
        }
        return sessionId.incrementAndGet();
    }

    // ==================== 基本抽象方法 ====================

    /**
     * 獲取版本號
     * 
     * @return 版本字串
     */
    public abstract String getVersion();

    /**
     * 獲取 Connection 的具體類型
     * 
     * @return Connection 類型的 Class 對象
     */
    protected abstract Class<C> getConnectionClass();

    public int getLimitConnect() {
        return limitConnect;
    }

    public int getMaxConnect() {
        return maxConnect != null ? maxConnect.intValue() : 0;
    }

    public int getNowConnect() {
        return nowConnect != null ? nowConnect.intValue() : 0;
    }

    public int getPort() {
        return port;
    }

    protected abstract CacheBase<M, B> createCacheInstance() throws RuntimeException;

    protected ChannelInitializer<SocketChannel> createInitializer(
            Class<? extends ChannelInitializer<SocketChannel>> initializerClazz) throws Exception {
        ChannelInitializer<SocketChannel> handler = null;
        if (initializerClazz != null) {
            Constructor<?> ctor = initializerClazz.getDeclaredConstructor(getSocketClazz());
            ctor.setAccessible(true);
            handler = initializerClazz.cast(ctor.newInstance(this));
        } else {
            throw new Exception("initializer class can not be null");
        }
        return handler;
    }

    /**
     * 使用反射創建連接實例（預設實現）
     */
    protected C createConnectionInstance()
            throws InstantiationException, IllegalAccessException,
            NoSuchMethodException, java.lang.reflect.InvocationTargetException {
        Class<C> connectionClass = getConnectionClass();
        if (connectionClass != null) {
            return connectionClass.getDeclaredConstructor().newInstance();
        }
        return null;
    }

    /**
     * 重載方法：支援工廠方法創建連接實例
     * 子類可以覆寫此方法以使用自定義的工廠邏輯
     */
    protected C createConnectionInstance(
            java.util.function.Supplier<C> factory) {
        if (factory != null) {
            return factory.get();
        }
        try {
            return createConnectionInstance();
        } catch (Exception e) {
            logger.error("Failed to create connection instance using reflection: {}", e.getMessage(), e);
            return null;
        }
    }

    public abstract void bind();

    public abstract void close();

    public abstract void onConnect(long sessionId);

    public abstract void onDisconnect(long sessionId);

    public CacheBase<M, B> getCacheBase() {
        return cacheManager;
    }

    public <T> T getProperty(Class<T> clazz, Channel channel, String property) {
        AttributeKey<T> k = AttributeKey.valueOf(property);
        if (!channel.hasAttr(k)) {
            logger.error("channel key is not exist => {}", channel.id());
            return null;
        }
        Attribute<T> v = channel.attr(k);
        return v.get();
    }

    public <T> void setProperty(Class<T> clazz, Channel channel, String property, T value) {
        AttributeKey<T> k = AttributeKey.valueOf(property);
        Attribute<T> v = channel.attr(k);
        v.set(value);
    }

    /**
     * 獲取連接陣列（保持向後兼容）
     */
    @SuppressWarnings("unchecked")
    public C[] getConnections() {
        return (C[]) connectionMap.values().toArray(IConnection[]::new);
    }

    public C getConnection(long sessionId) {
        if (!connectionMap.containsKey(sessionId)) {
            return null;
        }
        return connectionMap.get(sessionId);
    }

    public C getConnection(Channel channel) {
        C connection = null;
        if (channel != null) {
            long sessionId = getProperty(long.class, channel, "sessionId");
            if (!connectionMap.containsKey(sessionId)) {
                logger.error("connection key is not exist => {}", sessionId);
            } else {
                connection = connectionMap.get(sessionId);
            }
        }
        return connection;
    }

    public boolean putConnection(Channel channel) {
        boolean success = false;
        if (channel != null) {
            // 提前驗證連接類型
            Class<C> connectionClass = getConnectionClass();
            if (connectionClass == null) {
                logger.error("Connection class is null, cannot create connection instance");
                return false;
            }

            String version = getVersion();
            long sessionId = genSessionId();
            long connectTime = DateUtil.getCurrentTimestamp();
            String connectionClassName = connectionClass.getSimpleName();
            C connection = null;

            // 只在反射創建實例時捕獲異常
            try {
                connection = createConnectionInstance();
            } catch (InstantiationException e) {
                logger.error("Failed to instantiate connection class {}: {}",
                        connectionClassName, e.getMessage(), e);
            } catch (IllegalAccessException e) {
                logger.error("Cannot access constructor of connection class {}: {}",
                        connectionClassName, e.getMessage(), e);
            } catch (NoSuchMethodException e) {
                logger.error("No default constructor found for connection class {}: {}",
                        connectionClassName, e.getMessage(), e);
            } catch (java.lang.reflect.InvocationTargetException e) {
                // 安全處理 getCause() 可能為 null 的情況
                Throwable cause = e.getCause();
                String causeMessage = cause != null ? cause.getMessage() : "Unknown cause";
                logger.error("Constructor of connection class {} threw an exception: {}",
                        connectionClassName, causeMessage, cause != null ? cause : e);
            } catch (Exception e) {
                logger.error("Unexpected error creating connection instance of class {}: {}",
                        connectionClassName, e.getMessage(), e);
            }

            // 分離連接設置邏輯，避免異常被意外捕獲
            if (connection != null) {
                try {
                    connection.setChannel(channel);
                    connection.setVersion(version);
                    connection.setSessionId(sessionId);
                    connection.setConnectTime(connectTime);

                    // 設置 channel 屬性
                    setProperty(Long.class, channel, "sessionId", sessionId);
                } catch (Exception e) {
                    logger.error("Failed to configure connection instance: {}", e.getMessage(), e);
                    connection = null; // 設置失敗時重置連接為 null
                }
            }

            if (connection == null) {
                logger.error("connection can not create instance => {}", sessionId);
                return false;
            }
            if (connectionMap.containsKey(sessionId)) {
                logger.error("connection key has already exist => {}", sessionId);
                return false;
            }
            connectionMap.put(sessionId, connection);
            int nowValue = nowConnect.incrementAndGet();
            int maxValue = maxConnect.intValue();
            if (maxValue < nowValue) {
                maxConnect.set(nowValue);
            }
            onConnect(sessionId);
            logger.info("add connection id={} sessionId={} address={}", channel.id(), sessionId,
                    channel.remoteAddress());
            success = true;
        }
        return success;
    }

    /**
     * 使用工廠方法的 putConnection 重載
     */
    public boolean putConnection(Channel channel,
            java.util.function.Supplier<C> connectionFactory) {
        boolean success = false;
        if (channel != null) {
            // 提前驗證連接類型
            Class<C> connectionClass = getConnectionClass();
            if (connectionClass == null) {
                logger.error("Connection class is null, cannot create connection instance");
                return false;
            }

            String version = getVersion();
            long sessionId = genSessionId();
            long connectTime = DateUtil.getCurrentTimestamp();
            String connectionClassName = connectionClass.getSimpleName();
            C connection = null;

            // 使用工廠方法創建實例（已內部處理異常）
            try {
                connection = createConnectionInstance(connectionFactory);
            } catch (Exception e) {
                logger.error("Unexpected error creating connection instance with factory for class {}: {}",
                        connectionClassName, e.getMessage(), e);
            }

            // 分離連接設置邏輯，避免異常被意外捕獲
            if (connection != null) {
                try {
                    connection.setChannel(channel);
                    connection.setVersion(version);
                    connection.setSessionId(sessionId);
                    connection.setConnectTime(connectTime);

                    // 設置 channel 屬性
                    setProperty(Long.class, channel, "sessionId", sessionId);
                } catch (Exception e) {
                    logger.error("Failed to configure connection instance: {}", e.getMessage(), e);
                    connection = null; // 設置失敗時重置連接為 null
                }
            }

            if (connection == null) {
                logger.error("connection can not create instance => {}", sessionId);
                return false;
            }
            if (connectionMap.containsKey(sessionId)) {
                logger.error("connection key has already exist => {}", sessionId);
                return false;
            }
            connectionMap.put(sessionId, connection);
            int nowValue = nowConnect.incrementAndGet();
            int maxValue = maxConnect.intValue();
            if (maxValue < nowValue) {
                maxConnect.set(nowValue);
            }
            onConnect(sessionId);
            logger.info("add connection (factory) id={} sessionId={} address={}", channel.id(), sessionId,
                    channel.remoteAddress());
            success = true;
        }
        return success;
    }

    public C removeConnection(Channel channel) {
        C connection = null;
        if (channel != null) {
            long sessionId = getProperty(long.class, channel, "sessionId");
            if (!connectionMap.containsKey(sessionId)) {
                logger.error("connection key is not exist => {}", sessionId);
            } else {
                connection = connectionMap.get(sessionId);
                connectionMap.remove(sessionId);
                nowConnect.decrementAndGet();

                onDisconnect(sessionId);

                logger.info("remove connection id={} sessionId={} address={}", channel.id(),
                        sessionId, channel.remoteAddress());
            }
        }
        return connection;
    }

    protected void process() {
        try {
            while (!messageQueue.isEmpty()) {
                M message = popMessage();
                try {
                    dispatcher(message);
                } catch (Exception ee) {
                    logger.error(String.format("socket process protocol has error => %s", ee.getMessage()), ee);
                }
            }
        } catch (Exception e) {
            logger.error(String.format("socket process has unknown error => %s", e.getMessage()), e);
        }
    }

    public void putMessage(M message) {
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
        if (rateLimiter != null && !rateLimiter.pass()) {
            try {
                IConnection<B> connection = getConnection(sessionId);
                if (connection != null) {
                    connection.sendServerBusyMessage(mainNo, subNo, requestId);
                }
            } catch (Exception e) {
                logger.error(String.format("process busy message sessionId=%d requestId=%d protocol-%d-%d has error=%s",
                        sessionId,
                        requestId, mainNo, subNo, e.getMessage()), e);
            }
        } else {
            Consumer<M> processor = protocolRegister.getProtocolHandler(key);
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
                if (DEFAULT_WARN_RCV_TIMESTAMP < diff) {
                    logger.warn("protocol-{}-{} sessionId={} requestId={} has too long delay -> {} ms",
                            mainNo, subNo, sessionId, requestId, diff);

                } else if (DEFAULT_ABANDON_RCV_TIMESTAMP < diff) {
                    C connection = getConnection(sessionId);
                    if (connection != null) {
                        connection.sendServerBusyMessage(mainNo, subNo, requestId);
                    }
                    logger.error("protocol-{}-{} sessionId={} requestId={} has too long delay -> {} ms, abandon it",
                            mainNo, subNo, sessionId, requestId, diff);
                } else {
                    event.accept(message);
                }
            } catch (Exception e) {
                logger.error("protocol-{}-{} sessionId={} requestId={} has error => {}",
                        mainNo, subNo, sessionId, requestId, e.getMessage());
            }
        };
    }

    public void broadcast(ProtocolKey protocol, B buffer) {
        IConnection<B>[] connections = getConnections();
        if (connections != null) {
            for (IConnection<B> connection : connections) {
                connection.send(protocol, buffer);
            }
        }
    }

    public void send(long sessionId, ProtocolKey protocol, B buffer) {
        send(sessionId, protocol, 0, buffer);
    }

    public void send(long sessionId, ProtocolKey protocol, long requestId, B buffer) {
        IConnection<B> connection = getConnection(sessionId);
        if (connection != null) {
            connection.send(protocol, requestId, buffer);
        }
    }

    public void send(Channel channel, ProtocolKey protocol, B buffer) {
        send(channel, protocol, 0, buffer);
    }

    public void send(Channel channel, ProtocolKey protocol, long requestId, B buffer) {
        IConnection<B> connection = getConnection(channel);
        if (connection != null) {
            connection.send(protocol, requestId, buffer);
        }
    }

    public void broadcast(int mainNo, int subNo, B buffer) {
        IConnection<B>[] connections = getConnections();
        if (connections != null) {
            for (IConnection<B> connection : connections) {
                connection.send(mainNo, subNo, buffer);
            }
        }
    }

    public void send(long sessionId, int mainNo, int subNo, B buffer) {
        send(sessionId, mainNo, subNo, 0, buffer);
    }

    public void send(long sessionId, int mainNo, int subNo, long requestId, B buffer) {
        IConnection<B> connection = getConnection(sessionId);
        if (connection != null) {
            connection.send(mainNo, subNo, requestId, buffer);
        }
    }

    public void send(Channel channel, int mainNo, int subNo, B buffer) {
        send(channel, mainNo, subNo, 0, buffer);
    }

    public void send(Channel channel, int mainNo, int subNo, long requestId, B buffer) {
        IConnection<B> connection = getConnection(channel);
        if (connection != null) {
            connection.send(mainNo, subNo, requestId, buffer);
        }
    }

    protected class UpdateConnection implements Runnable {
        private final Runnable logic;

        public UpdateConnection(Runnable logic) {
            this.logic = logic;
        }

        @Override
        public void run() {
            try {
                if (running.get() && logic != null) {
                    logic.run();
                }
            } catch (Exception e) {
                logger.error(String.format("update connection has error=%s", e.getMessage()), e);
            }
        }
    }

    protected class UpdateCacheManager implements Runnable {
        @Override
        public void run() {
            try {
                if (running.get()) {
                    cacheManager.update();
                }
            } catch (Exception e) {
                logger.error(String.format("update cache manager has error=%s", e.getMessage()), e);
            }
        }
    }
}
