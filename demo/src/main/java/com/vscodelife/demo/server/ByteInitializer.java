package com.vscodelife.demo.server;

import java.net.SocketException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vscodelife.demo.server.handler.ByteAuthenticationHandler;
import com.vscodelife.demo.server.handler.ByteConnectHandler;
import com.vscodelife.demo.server.handler.ByteHeaderDecoderHandler;
import com.vscodelife.demo.server.handler.ByteHeaderEncoderHandler;
import com.vscodelife.demo.server.handler.ByteMessageHandler;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;

public class ByteInitializer extends ChannelInitializer<SocketChannel> {
    private static Logger logger = LoggerFactory.getLogger(ByteInitializer.class);

    private final TestByteServer socket;
    private final ScheduledExecutorService authScheduler;

    public ByteInitializer(TestByteServer socket) {
        this.socket = socket;
        // 創建專用於認證的調度器
        this.authScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "auth-scheduler");
            t.setDaemon(true);
            return t;
        });
    }

    @Override
    public void initChannel(SocketChannel channel) throws Exception {
        int rConnect = 100;
        int cConnect = socket.getLimitConnect();
        int limitConnect = 0;
        int nowConnect = socket.getNowConnect();
        limitConnect = Math.max(cConnect, rConnect);
        if (nowConnect >= limitConnect) {
            throw new SocketException("over connected");
        }
        ChannelPipeline pipeline = channel.pipeline();

        // 正確的 pipeline 順序：
        // 1. decoder: 將原始 ByteBuf 解碼為 ByteMessage<ByteUserHeader>
        pipeline.addLast("decoder", new ByteHeaderDecoderHandler(socket));
        // 2. auth: 處理已解碼的認證消息 (必須在 decoder 之後，其他業務 handler 之前)
        pipeline.addLast("auth", new ByteAuthenticationHandler(socket, authScheduler));
        // 3. message: 處理業務消息
        pipeline.addLast("message", new ByteMessageHandler(socket));
        // 4. connect: 處理連接相關邏輯
        pipeline.addLast("connect", new ByteConnectHandler(socket));
        // 5. encoder: 編碼出站消息 (處理出站消息，順序在這裡)
        pipeline.addLast("encoder", new ByteHeaderEncoderHandler(socket));
        logger.info("client {} is connected", channel.remoteAddress());
    }
}
