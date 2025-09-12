package com.vscodelife.demo.webserver.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vscodelife.demo.webserver.ChatWebServer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.util.CharsetUtil;

/**
 * WebSocket 握手處理器
 * 負責處理 HTTP 請求並升級為 WebSocket 連接
 */
public class ChatWebSocketHandshakeHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private static final Logger logger = LoggerFactory.getLogger(ChatWebSocketHandshakeHandler.class);

    private final ChatWebServer server;
    private WebSocketServerHandshaker handshaker;

    public ChatWebSocketHandshakeHandler(ChatWebServer server) {
        this.server = server;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {
        // 檢查是否為有效的 HTTP 請求
        if (!req.decoderResult().isSuccess()) {
            sendHttpResponse(ctx, req, new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
            return;
        }

        // 處理 WebSocket 握手
        WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(
                getWebSocketLocation(req), null, false);
        handshaker = wsFactory.newHandshaker(req);

        if (handshaker == null) {
            // 不支持的 WebSocket 版本
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        } else {
            // 執行握手
            ChannelFuture handshakeFuture = handshaker.handshake(ctx.channel(), req);
            handshakeFuture.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (future.isSuccess()) {
                        logger.info("WebSocket handshake completed for {}", ctx.channel().remoteAddress());

                        // 握手成功後，移除此處理器，因為後續不再需要處理 HTTP
                        ctx.pipeline().remove(ChatWebSocketHandshakeHandler.this);
                    } else {
                        logger.error("WebSocket handshake failed for {}", ctx.channel().remoteAddress());
                    }
                }
            });
        }
    }

    /**
     * 發送 HTTP 響應
     */
    private void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse res) {
        // 生成錯誤頁面
        if (res.status().code() != 200) {
            ByteBuf buf = Unpooled.copiedBuffer(res.status().toString(), CharsetUtil.UTF_8);
            res.content().writeBytes(buf);
            buf.release();
            HttpUtil.setContentLength(res, res.content().readableBytes());
        }

        // 發送響應並關閉連接
        ChannelFuture f = ctx.channel().writeAndFlush(res);
        if (!HttpUtil.isKeepAlive(req) || res.status().code() != 200) {
            f.addListener(ChannelFutureListener.CLOSE);
        }
    }

    /**
     * 獲取 WebSocket 位置
     */
    private String getWebSocketLocation(FullHttpRequest req) {
        String location = req.headers().get("Host") + "/websocket";
        return "ws://" + location;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("Exception in WebSocket handshake", cause);
        ctx.close();
    }
}
