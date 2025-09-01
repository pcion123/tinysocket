package com.vscodelife.clientsocket;

import java.lang.reflect.Constructor;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vscodelife.socketio.message.base.HeaderBase;
import com.vscodelife.socketio.message.base.MessageBase;
import com.vscodelife.socketio.util.ExecutorUtil;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

public class Connector<H extends HeaderBase, M extends MessageBase<H, B>, B> implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(Connector.class);

    private final ExecutorService MAIN_THREAD = Executors
            .newSingleThreadExecutor(ExecutorUtil.makeName("clientconnector"));

    private final long id;
    private final String hostname;
    private final int port;
    private final IClient<H, M, B> client;

    private EventLoopGroup bossGroup;
    private Bootstrap bootStrap;
    private Channel channel;

    protected final AtomicLong sessionId = new AtomicLong(0);
    protected final AtomicBoolean running = new AtomicBoolean(true);
    protected final AtomicBoolean connecting = new AtomicBoolean(false);

    public Connector(long id, String hostname, int port, IClient<H, M, B> client) {
        this.id = id;
        this.hostname = hostname;
        this.port = port;
        this.client = client;
    }

    private ChannelInitializer<SocketChannel> createInitializer() throws Exception {
        ChannelInitializer<SocketChannel> handler = null;
        Constructor<? extends ChannelInitializer<SocketChannel>> ctor = client.getInitializerClazz()
                .getDeclaredConstructor(Connector.class);
        ctor.setAccessible(true);
        handler = ctor.newInstance(this);
        return handler;
    }

    public void onConnected(ChannelHandlerContext ctx) {
        connecting.set(false);

        if (client != null) {
            client.onConnected(id, ctx);
        }
    }

    public void onDisconnected(ChannelHandlerContext ctx) {
        if (client != null) {
            client.onDisconnected(id, ctx);
        }
    }

    public void onSendMessage(ChannelHandlerContext ctx, M message) {
        if (client != null) {
            client.onSendMessage(id, ctx, message);
        }
    }

    public void onReceiveMessage(ChannelHandlerContext ctx, M message) {
        if (client != null) {
            client.onReceiveMessage(id, ctx, message);
        }
    }

    public void onException(ChannelHandlerContext ctx, Throwable cause) {
        if (client != null) {
            client.onException(id, ctx, cause);
        }
    }

    @Override
    public void run() {
        try {
            // 設置連接狀態
            connecting.set(true);

            ChannelInitializer<SocketChannel> handler = createInitializer();
            bossGroup = new NioEventLoopGroup();
            bootStrap = new Bootstrap();
            bootStrap.group(bossGroup).channel(NioSocketChannel.class).handler(handler);

            logger.info("connector is open");

            ChannelFuture f = bootStrap.connect(hostname, port).sync();
            channel = f.channel();
            while (running.get()) {
                client.process();
                Thread.sleep(100);
            }
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            logger.warn("Connector thread was interrupted: {}", e.getMessage());
            Thread.currentThread().interrupt(); // 恢復中斷狀態
            connecting.set(false);
        } catch (Exception e) {
            logger.error("Connector error: {}", e.getMessage(), e);
            onException(null, e);
            connecting.set(false);
        } finally {
            running.set(false);
            connecting.set(false);

            // 按順序清理資源
            if (channel != null && channel.isOpen()) {
                try {
                    channel.close().sync();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                channel = null;
            }
            if (bossGroup != null) {
                bossGroup.shutdownGracefully();
                bossGroup = null;
            }
            logger.info("connector is close");
        }
    }

    public void connect() {
        running.set(true);

        MAIN_THREAD.execute(this);
    }

    public void disconnect() {
        running.set(false);
    }

    public void close() {
        running.set(false);

        MAIN_THREAD.shutdown();
    }

    public boolean isRunning() {
        return running.get();
    }

    public boolean isConnected() {
        return channel != null && channel.isActive();
    }

    public boolean isConnecting() {
        return connecting.get();
    }

    public long getId() {
        return id;
    }

    public String getHostname() {
        return hostname;
    }

    public int getPort() {
        return port;
    }

    public long getSessionId() {
        return sessionId.get();
    }

    public void setSessionId(long sessionId) {
        this.sessionId.set(sessionId);
    }

    public <T> T getProperty(Class<T> clazz, String property) {
        if (channel == null) {
            logger.warn("Cannot get property '{}': channel is null", property);
            return null;
        }
        AttributeKey<T> k = AttributeKey.valueOf(property);
        if (!channel.hasAttr(k)) {
            logger.error("channel key is not exist => {}", channel.id());
            return null;
        }
        Attribute<T> v = channel.attr(k);
        return v.get();
    }

    public <T> void setProperty(Class<T> clazz, String property, T value) {
        if (channel == null) {
            logger.warn("Cannot set property '{}': channel is null", property);
            return;
        }
        AttributeKey<T> k = AttributeKey.valueOf(property);
        Attribute<T> v = channel.attr(k);
        v.set(value);
    }

    public void send(M message) {
        try {
            if (channel != null && channel.isActive()) {
                channel.writeAndFlush(message);
            } else {
                throw new SocketException("Cannot send message: channel is not active or null");
            }
        } catch (Exception e) {
            logger.error("Failed to send message: {}", e.getMessage(), e);
            onException(null, e);
        }
    }
}
