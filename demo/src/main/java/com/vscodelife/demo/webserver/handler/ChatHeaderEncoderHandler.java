package com.vscodelife.demo.webserver.handler;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vscodelife.demo.webserver.ChatUserHeader;
import com.vscodelife.demo.webserver.ChatWebServer;
import com.vscodelife.socketio.buffer.JsonMapBuffer;
import com.vscodelife.socketio.message.JsonMessage;
import com.vscodelife.socketio.message.base.CacheBase;
import com.vscodelife.socketio.util.JsonUtil;
import com.vscodelife.socketio.util.StrUtil;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

/**
 * 聊天消息編碼處理器 - WebSocket版本
 * 負責將JsonMessage對象編碼為WebSocketFrame發送
 */
public class ChatHeaderEncoderHandler extends MessageToMessageEncoder<JsonMessage<ChatUserHeader>> {
    private static final Logger logger = LoggerFactory.getLogger(ChatHeaderEncoderHandler.class);

    private final ChatWebServer socket;
    private final CacheBase<JsonMessage<ChatUserHeader>, JsonMapBuffer> cacheManager;

    public ChatHeaderEncoderHandler(ChatWebServer socket) {
        this.socket = socket;
        this.cacheManager = socket.getCacheBase();
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, JsonMessage<ChatUserHeader> message, List<Object> out)
            throws Exception {
        if (message == null) {
            throw new IllegalArgumentException("Socket or message cannot be null");
        }
        try {
            // 取出header
            ChatUserHeader header = message.getHeader();
            if (header == null) {
                throw new IllegalArgumentException("Header cannot be null");
            }
            // 將消息對象轉換為JSON字符串
            String json = JsonUtil.toJson(message);
            // 編碼為文本WebSocket幀
            TextWebSocketFrame frame = new TextWebSocketFrame(json);
            out.add(frame);
            // 用戶ID為空視為訪客
            if (StrUtil.isEmpty(header.getUserId())) {
                header.setUserId("guest");
            }
            // 放入快取
            if (cacheManager.isEnabled() && cacheManager.isIncluded(message)) {
                cacheManager.putMessage(header.getUserId(), message);
            }

            logger.debug(
                    "Encoded message to client {} -> mainNo={} subNo={} requestId={}",
                    ctx.channel().id(), header.getMainNo(), header.getSubNo(), header.getRequestId());

        } catch (Exception e) {
            logger.error("Error encoding byte message: {}", e.getMessage(), e);
            throw e;
        }
    }
}
