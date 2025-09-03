package com.vscodelife.serversocket;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;

import com.vscodelife.socketio.connection.IConnection;
import com.vscodelife.socketio.constant.ProtocolId;
import com.vscodelife.socketio.message.JsonCache;
import com.vscodelife.socketio.message.JsonMessage;
import com.vscodelife.socketio.message.base.HeaderBase;
import com.vscodelife.socketio.util.ExecutorUtil;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public abstract class JsonSocket<H extends HeaderBase, C extends IConnection<String>>
        extends SocketBase<H, C, JsonMessage<H>, String> {
    protected final ExecutorService mainThread = Executors
            .newSingleThreadExecutor(ExecutorUtil.makeName("jsonsocketmainpool"));
    protected final ScheduledExecutorService scheduledThread = Executors
            .newSingleThreadScheduledExecutor(ExecutorUtil.makeName("jsonsocketschedulepool"));

    protected JsonSocket(Logger logger, int port, int limitConnect,
            Class<? extends ChannelInitializer<SocketChannel>> initializerClazz) {
        super(logger, port, limitConnect, initializerClazz);

        protocolRegister.registerProtocol(ProtocolId.PING, catchException(message -> ping(message)));
    }

    @Override
    protected JsonCache<H> createCacheInstance() throws RuntimeException {
        return new JsonCache<>();
    }

    @SuppressWarnings({ "static-access" })
    @Override
    public void run() {
        try {
            ChannelInitializer<SocketChannel> handler = createInitializer(initializerClazz);
            bossGroup = new NioEventLoopGroup();
            workerGroup = new NioEventLoopGroup();
            bootStrap = new ServerBootstrap();
            bootStrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .childOption(ChannelOption.SO_KEEPALIVE, true).childHandler(handler);

            logger.info("json server is open");

            ChannelFuture f = bootStrap.bind(port).sync();
            channel = f.channel();
            while (running.get()) {
                process();
                Thread.currentThread().sleep(100);
            }
            channel.closeFuture().sync();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            running.set(false);
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
            scheduledThread.shutdown();
            logger.info("json server is close");
        }
    }

    @Override
    public void bind() {
        // 啟動主執行緒
        mainThread.execute(this);
        // 註冊定時檢查連線逾時更新檢查
        scheduledThread.scheduleAtFixedRate(new UpdateConnection(() -> {
            updateConnections();
        }), DEFAULT_UPDATE_CONNECTION_INTERVAL, DEFAULT_UPDATE_CONNECTION_INTERVAL, TimeUnit.SECONDS);
        // 註冊定時更新快取管理
        scheduledThread.scheduleAtFixedRate(new UpdateCacheManager(), DEFAULT_UPDATE_CACHE_MANAGER_INTERVAL,
                DEFAULT_UPDATE_CACHE_MANAGER_INTERVAL,
                TimeUnit.SECONDS);
    }

    @Override
    public void close() {
        running.set(false);

        // 優雅地關閉執行緒池並等待完成
        mainThread.shutdown();
        scheduledThread.shutdown();

        try {
            // 等待主執行緒完成清理
            if (!mainThread.awaitTermination(3, TimeUnit.SECONDS)) {
                logger.warn("Main thread did not terminate gracefully, forcing shutdown");
                mainThread.shutdownNow();
            }

            // 等待排程執行緒完成清理
            if (!scheduledThread.awaitTermination(3, TimeUnit.SECONDS)) {
                logger.warn("Scheduled thread did not terminate gracefully, forcing shutdown");
                scheduledThread.shutdownNow();
            }
        } catch (InterruptedException e) {
            logger.warn("Interrupted while waiting for threads to terminate");
            mainThread.shutdownNow();
            scheduledThread.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    protected void updateConnections() {
        C[] connections = getConnections();
        for (@SuppressWarnings("unused")
        C connection : connections) {
            // 實作檢查連線是否逾時處理邏輯
        }
    }

    protected void ping(JsonMessage<H> message) {
        H header = message.getHeader();
        int mainNo = header.getMainNo();
        int subNo = header.getSubNo();
        long sessionId = header.getSessionId();
        long requestId = header.getRequestId();
        logger.info("sessionId={} requestId={} rcv client ask ping request", sessionId, requestId);
        send(sessionId, mainNo, subNo, requestId, "pong");
    }
}
