package com.vscodelife.demo.webserver;

import java.net.SocketException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vscodelife.demo.webserver.handler.ChatAuthenticationHandler;
import com.vscodelife.demo.webserver.handler.ChatConnectHandler;
import com.vscodelife.demo.webserver.handler.ChatHeaderDecoderHandler;
import com.vscodelife.demo.webserver.handler.ChatHeaderEncoderHandler;
import com.vscodelife.demo.webserver.handler.ChatMessageHandler;
import com.vscodelife.demo.webserver.handler.ChatWebSocketHandshakeHandler;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;

/**
 * 聊天服務器的Netty管道初始化器 - WebSocket版本
 */
public class ChatInitializer extends ChannelInitializer<SocketChannel> {
    private static final Logger logger = LoggerFactory.getLogger(ChatInitializer.class);

    private final ChatWebServer socket;
    private final ScheduledExecutorService authScheduler;

    public ChatInitializer(ChatWebServer socket) {
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

        // 配置 WebSocket 處理器管道
        // 1. HTTP 編解碼器
        pipeline.addLast("http-codec", new HttpServerCodec());

        // 2. HTTP 消息聚合器 (將 HTTP 消息聚合為 FullHttpRequest)
        pipeline.addLast("http-aggregator", new HttpObjectAggregator(65536));

        // 3. WebSocket 握手處理器
        pipeline.addLast("websocket-handshake", new ChatWebSocketHandshakeHandler(socket));

        // 4. WebSocket frame decoder: 將 WebSocketFrame 解碼為自定義消息格式
        pipeline.addLast("websocket-decoder", new ChatHeaderDecoderHandler(socket));

        // 5. connect: 處理連接相關邏輯
        pipeline.addLast("connect", new ChatConnectHandler(socket));

        // 6. auth: 處理已解碼的認證消息 (必須在 decoder 之後，其他業務 handler 之前)
        pipeline.addLast("auth", new ChatAuthenticationHandler(socket, authScheduler));

        // 7. message: 處理業務消息
        pipeline.addLast("message", new ChatMessageHandler(socket));

        // 8. WebSocket frame encoder: 編碼出站消息為 WebSocketFrame
        pipeline.addLast("websocket-encoder", new ChatHeaderEncoderHandler(socket));

        logger.info("client {} is connected", channel.remoteAddress());
    }
}
