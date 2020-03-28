package com.tiny.socket.clientsocket;

import java.lang.reflect.Constructor;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.tiny.socket.socketio.buffer.ByteArrayBuffer;
import com.tiny.socket.socketio.header.HeaderBase;
import com.tiny.socket.socketio.message.base.MessageBase;
import com.tiny.socket.socketio.util.ExecutorUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

public abstract class ClientSocket implements Runnable {
    protected static Logger logger = LoggerFactory.getLogger(ClientSocket.class);

    protected Class<? extends ChannelInitializer<SocketChannel>> initializerClass;

    protected boolean running = true;

    protected ExecutorService mainThread =
            Executors.newSingleThreadExecutor(ExecutorUtil.makeName("clientsocketmainpool"));
    protected ScheduledExecutorService scheduledThread = Executors
            .newSingleThreadScheduledExecutor(ExecutorUtil.makeName("clientsocketschedulepool"));

    @SuppressWarnings("rawtypes")
    protected Queue<MessageBase> messageQueue = new LinkedBlockingQueue<>();
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected Consumer<MessageBase>[][] processes = new Consumer[128][128];

    private Consumer<Object> connectEvt;
    private Consumer<Object> disconnectEvt;
    private Consumer<Object> receiveEvt;
    private Consumer<Object> sendEvt;

    protected long sessionId;
    protected String hostname;
    protected int port;

    private EventLoopGroup bossGroup;
    private Bootstrap bootStrap;
    protected Channel channel;

    protected ClientSocket(String hostname, int port,
            Class<? extends ChannelInitializer<SocketChannel>> initializerClass) {
        this.hostname = hostname;
        this.port = port;
        this.initializerClass = initializerClass;
    }

    public boolean isConnected() {
        return channel != null ? channel.isActive() : false;
    }

    public abstract short getVersion();

    public long getSessionId() {
        return sessionId;
    }

    public void setSessionId(long sessionId) {
        this.sessionId = sessionId;
    }

    public String getHostname() {
        return hostname;
    }

    public int getPort() {
        return port;
    }

    public void setConnectEvt(Consumer<Object> connectEvt) {
        this.connectEvt = connectEvt;
    }

    public void setDisconnectEvt(Consumer<Object> disconnectEvt) {
        this.disconnectEvt = disconnectEvt;
    }

    public void setReceiveEvt(Consumer<Object> receiveEvt) {
        this.receiveEvt = receiveEvt;
    }

    public void setSendEvt(Consumer<Object> sendEvt) {
        this.sendEvt = sendEvt;
    }

    @SuppressWarnings({"rawtypes", "static-access"})
    @Override
    public void run() {
        try {
            ChannelInitializer<SocketChannel> handler = null;
            if (initializerClass != null) {
                Constructor ctor = initializerClass.getDeclaredConstructor(ClientSocket.class);
                ctor.setAccessible(true);
                handler = initializerClass.cast(ctor.newInstance(this));
            } else {
                throw new Exception("initializer class can not be null");
            }
            bossGroup = new NioEventLoopGroup();
            bootStrap = new Bootstrap();
            bootStrap.group(bossGroup).channel(NioSocketChannel.class).handler(handler);

            logger.info("client is open");

            ChannelFuture f = bootStrap.connect(hostname, port).sync();
            channel = f.channel();
            while (running) {
                process();
                Thread.currentThread().sleep(100);
            }
            f.channel().closeFuture();
        } catch (Exception e) {
            logger.error(e.getMessage());
        } finally {
            running = false;
            bossGroup.shutdownGracefully();
            logger.info("client is close");
        }
    }

    public void connect() {
        synchronized (this) {
            running = true;
        }
        mainThread.execute(this);
    }

    public void disconnect() {
        synchronized (this) {
            running = false;
        }
    }

    public void close() {
        synchronized (this) {
            running = false;
        }
        mainThread.shutdown();
        scheduledThread.shutdown();
    }

    public void onConnect(Object value) {
        if (connectEvt != null)
            connectEvt.accept(value);
    }

    public void onDisconnect(Object value) {
        if (disconnectEvt != null)
            disconnectEvt.accept(value);
    }

    public void onReceive(Object value) {
        if (receiveEvt != null)
            receiveEvt.accept(value);
    }

    public void onSend(Object value) {
        if (sendEvt != null)
            sendEvt.accept(value);
    }

    public <T> T getProperty(Class<T> clazz, String property) {
        return getProperty(clazz, channel, property);
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

    public <T> void setProperty(Class<T> clazz, String property, T value) {
        setProperty(clazz, property, value);
    }

    public <T> void setProperty(Class<T> clazz, Channel channel, String property, T value) {
        AttributeKey<T> k = AttributeKey.valueOf(property);
        Attribute<T> v = channel.attr(k);
        v.set(value);
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
                logger.info("handle clientsocket dispatcher protocol-[{},{}] too long = {}", mainNo,
                        subNo, timestamp2 - timestamp1);
        } else {
            logger.info("process[{}][{}] is not create", mainNo, subNo);
        }
    }

    public void send(int mainNo, int subNo, ByteArrayBuffer buffer) {
        send(mainNo, subNo, buffer, true);
    }

    public void send(int mainNo, int subNo, ByteArrayBuffer buffer, boolean release) {
        send((byte) mainNo, (byte) subNo, buffer, release);
    }

    protected abstract void send(byte mainNo, byte subNo, ByteArrayBuffer buffer, boolean release);
}
