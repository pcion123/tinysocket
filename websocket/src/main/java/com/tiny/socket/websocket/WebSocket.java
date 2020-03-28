package com.tiny.socket.websocket;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.tiny.socket.socketio.header.HeaderBase;
import com.tiny.socket.socketio.message.base.MessageBase;
import com.tiny.socket.socketio.util.DateUtil;
import com.tiny.socket.socketio.util.ExecutorUtil;
import com.tiny.socket.websocket.connection.ConnectionBase;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

public abstract class WebSocket implements Runnable {
    protected static Logger logger = LoggerFactory.getLogger(WebSocket.class);

    protected Class<? extends ChannelInitializer<SocketChannel>> initializerClass;
    @SuppressWarnings("rawtypes")
    protected Map connectionMap;
    protected int limitConnect;
    protected AtomicInteger maxConnect = new AtomicInteger(0);
    protected AtomicInteger nowConnect = new AtomicInteger(0);

    protected boolean running = true;

    protected ExecutorService mainThread =
            Executors.newSingleThreadExecutor(ExecutorUtil.makeName("websocketmainpool"));
    protected ScheduledExecutorService scheduledThread = Executors
            .newSingleThreadScheduledExecutor(ExecutorUtil.makeName("websocketschedulepool"));

    @SuppressWarnings("rawtypes")
    protected Queue<MessageBase> messageQueue = new LinkedBlockingQueue<>();
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected Consumer<MessageBase>[][] processes = new Consumer[128][128];

    private AtomicLong sessionId = new AtomicLong(0);
    private int port;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ServerBootstrap bootStrap;
    private Channel channel;

    protected WebSocket(int port, int limitConnect,
            Class<? extends ChannelInitializer<SocketChannel>> initializerClass) {
        this.limitConnect = limitConnect;
        this.port = port;
        this.initializerClass = initializerClass;
    }

    public boolean isBinding() {
        return channel != null ? channel.isActive() : false;
    }

    public long genSessionId() {
        if (sessionId.longValue() >= Long.MAX_VALUE)
            sessionId.set(0);

        return sessionId.incrementAndGet();
    }

    public abstract short getVersion();

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

    @SuppressWarnings({"rawtypes", "static-access"})
    @Override
    public void run() {
        try {
            ChannelInitializer<SocketChannel> handler = null;
            if (initializerClass != null) {
                Constructor ctor = initializerClass.getDeclaredConstructor(WebSocket.class);
                ctor.setAccessible(true);
                handler = initializerClass.cast(ctor.newInstance(this));
            } else {
                throw new Exception("initializer class can not be null");
            }
            bossGroup = new NioEventLoopGroup();
            workerGroup = new NioEventLoopGroup();
            bootStrap = new ServerBootstrap();
            bootStrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .childOption(ChannelOption.SO_KEEPALIVE, true).childHandler(handler);

            logger.info("web server is open");

            ChannelFuture f = bootStrap.bind(port).sync();
            channel = f.channel();
            while (running) {
                process();
                Thread.currentThread().sleep(100);
            }
            f.channel().closeFuture();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            running = false;
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
            scheduledThread.shutdown();
            logger.info("server is close");
        }
    }

    public void bind() {
        mainThread.execute(this);
    }

    public void close() {
        synchronized (this) {
            running = false;
        }
        mainThread.shutdown();
        scheduledThread.shutdown();
    }

    public abstract void onConnect(long sessionId);

    public abstract void onDisconnect(long sessionId);

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

    @SuppressWarnings("unchecked")
    public <T extends ConnectionBase> T[] getConnections(Class<T> clazz) {
        T[] array = (T[]) Array.newInstance(clazz, connectionMap.size());
        connectionMap.values().toArray(array);
        return array;
    }

    public <T extends ConnectionBase> T getConnection(Class<T> clazz, long sessionId) {
        if (!connectionMap.containsKey(sessionId))
            return null;

        return clazz.cast(connectionMap.get(sessionId));
    }

    public <T extends ConnectionBase> T getConnection(Class<T> clazz, Channel channel) {
        if (channel == null)
            return null;

        long sessionId = getProperty(long.class, channel, "sessionId");
        if (!connectionMap.containsKey(sessionId)) {
            logger.error("connection key is not exist => {}", sessionId);
            return null;
        }
        return clazz.cast(connectionMap.get(sessionId));
    }

    @SuppressWarnings({"unchecked", "deprecation"})
    public <T extends ConnectionBase> boolean putConnection(Class<T> clazz, Channel channel) {
        if (channel == null)
            return false;

        short version = getVersion();
        long sessionId = genSessionId();
        long connectTime = DateUtil.getCurrentTimestamp();
        T connection = null;
        try {
            connection = clazz.newInstance();
            connection.setChannel(channel);
            connection.setVersion(version);
            connection.setSessionId(sessionId);
            connection.setConnectTime(connectTime);
        } catch (InstantiationException e) {
            logger.error(e.getMessage(), e);
        } catch (IllegalAccessException e) {
            logger.error(e.getMessage(), e);
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
        if (maxValue < nowValue)
            maxConnect.set(nowValue);

        onConnect(sessionId);

        logger.info("add connection id={} sessionId={} address={}", channel.id(), sessionId,
                channel.remoteAddress());

        return true;
    }

    public <T extends ConnectionBase> T removeConnection(Class<T> clazz, Channel channel) {
        if (channel == null)
            return null;

        long sessionId = getProperty(long.class, channel, "sessionId");
        if (!connectionMap.containsKey(sessionId)) {
            logger.error("connection key is not exist => {}", sessionId);
            return null;
        }

        T connection = clazz.cast(connectionMap.get(sessionId));
        connectionMap.remove(sessionId);
        nowConnect.decrementAndGet();

        onDisconnect(sessionId);

        logger.info("remove connection id={} sessionId={} address={}", channel.id(), sessionId,
                channel.remoteAddress());

        return connection;
    }

    @SuppressWarnings("rawtypes")
    public void registerProtocol(int mainNo, int subNo, Consumer<MessageBase> event) {
        registerProtocol((byte) mainNo, (byte) subNo, event);
    }

    @SuppressWarnings("rawtypes")
    private void registerProtocol(byte mainNo, byte subNo, Consumer<MessageBase> event) {
        processes[mainNo][subNo] = event;
    }

    public void unregisterProtocol(int mainNo, int subNo) {
        unregisterProtocol((byte) mainNo, (byte) subNo);
    }

    private void unregisterProtocol(byte mainNo, byte subNo) {
        processes[mainNo][subNo] = null;
    }

    @SuppressWarnings("rawtypes")
    protected void process() {
        try {
            while (!messageQueue.isEmpty()) {
                MessageBase message = popMessage();
                try {
                    dispatcher(message);
                } catch (Exception e) {
                    logger.error(String.format("protocol has error => %s", e.getMessage()), e);
                } finally {
                    if (message != null)
                        message.release();
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    @SuppressWarnings("rawtypes")
    public void putMessage(MessageBase message) {
        messageQueue.add(message);
    }

    @SuppressWarnings("rawtypes")
    protected MessageBase popMessage() {
        return messageQueue.size() > 0 ? messageQueue.poll() : null;
    }

    @SuppressWarnings("rawtypes")
    public void dispatcher(MessageBase message) {
        HeaderBase header = message.getHeader();
        byte mainNo = header.getMainNo();
        byte subNo = header.getSubNo();
        if (processes[mainNo][subNo] != null) {
            long timestamp1 = System.currentTimeMillis();
            processes[mainNo][subNo].accept(message);
            long timestamp2 = System.currentTimeMillis();
            if (timestamp2 - timestamp1 > 1000)
                logger.info("handle serversocket dispatcher protocol-[{},{}] too long = {}", mainNo,
                        subNo, timestamp2 - timestamp1);
        } else {
            logger.info("process[{}][{}] is not create", mainNo, subNo);
        }
    }

    protected abstract void broadcast(byte mainNo, byte subNo, String buffer);

    protected abstract void send(long sessionId, byte mainNo, byte subNo, String buffer);

    protected abstract void send(Channel channel, byte mainNo, byte subNo, String buffer);
}
