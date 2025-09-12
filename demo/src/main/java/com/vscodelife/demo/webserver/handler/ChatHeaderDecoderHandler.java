package com.vscodelife.demo.webserver.handler;

import java.net.SocketException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson2.JSONObject;
import com.vscodelife.demo.webserver.ChatUserConnection;
import com.vscodelife.demo.webserver.ChatUserHeader;
import com.vscodelife.demo.webserver.ChatWebServer;
import com.vscodelife.demo.webserver.util.TokenUtil;
import com.vscodelife.socketio.buffer.JsonMapBuffer;
import com.vscodelife.socketio.constant.ProtocolId;
import com.vscodelife.socketio.message.JsonMessage;
import com.vscodelife.socketio.message.base.ProtocolKey;
import com.vscodelife.socketio.util.JsonUtil;

import io.jsonwebtoken.Claims;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

/**
 * 聊天消息解碼處理器 - WebSocket版本
 * 負責將WebSocketFrame解碼為JsonMessage對象
 */
public class ChatHeaderDecoderHandler extends MessageToMessageDecoder<TextWebSocketFrame> {
    private static final Logger logger = LoggerFactory.getLogger(ChatHeaderDecoderHandler.class);

    private final ChatWebServer socket;

    public ChatHeaderDecoderHandler(ChatWebServer socket) {
        this.socket = socket;
    }

    protected void decode(ChannelHandlerContext ctx, TextWebSocketFrame frame, List<Object> out) throws Exception {
        // 取得JSON字串
        String json = frame.text();
        // 解析整個JSON
        JSONObject jsonObj = JsonUtil.parseObject(json);
        // 解析header內容
        ChatUserHeader header = jsonObj.getJSONObject("header").toJavaObject(ChatUserHeader.class);
        // 取得buffer內容
        JsonMapBuffer buffer = new JsonMapBuffer(jsonObj.getString("buffer"));

        JsonMessage<ChatUserHeader> message = new JsonMessage<ChatUserHeader>(header, buffer);

        long requestId = header.getRequestId();
        // 取得協議鍵
        ProtocolKey protocolKey = header.getProtocolKey();
        logger.info("requestId={} header={}", requestId, jsonObj.getJSONObject("header").toString());
        logger.debug("Protocol comparison: received={}, AUTH={}, equals={}",
                protocolKey, ProtocolId.AUTH, ProtocolId.AUTH.equals(protocolKey));

        // 取得用戶連接
        ChatUserConnection connection = socket.getConnection(ctx.channel());
        if (connection != null && message != null) {

            if (!connection.isAuthed() && ProtocolId.AUTH.equals(protocolKey)) {
                // 尚未認證但是認證請求，允許通過
                logger.debug("sessionId={} requestId={} userId={} unauthenticated auth request allowed",
                        header.getSessionId(), header.getRequestId(), header.getUserId());

            } else if (!connection.isAuthed() && !ProtocolId.AUTH.equals(protocolKey)) {
                // 尚未認證且不是認證請求，拒絕
                logger.warn("sessionId={} requestId={} userId={} unauthenticated non-auth request rejected",
                        header.getSessionId(), header.getRequestId(), header.getUserId());

                JsonMapBuffer response = new JsonMapBuffer();
                response.put("code", 401);
                response.put("message", "Authentication required");
                connection.send(header.getProtocolKey(), header.getRequestId(), response);
                connection.disconnect();
                return;

            } else if (connection.isAuthed()) {
                String token = header.getToken();
                try {
                    // 驗證 token 是否合法
                    validateUserToken(connection, header, token);
                } catch (SocketException e) {
                    logger.error("Token validation failed, closing connection: {}", e.getMessage());

                    // 發送錯誤結果給客戶端
                    try {
                        JsonMapBuffer response = new JsonMapBuffer();
                        response.put("code", 401); // Unauthorized
                        response.put("message", e.getMessage());
                        connection.send(header.getProtocolKey(), header.getRequestId(), response);
                    } catch (Exception sendException) {
                        logger.error("Failed to send error response to client: {}", sendException.getMessage());
                    }
                    connection.disconnect();
                    return; // 連接已斷開，不需要繼續傳遞消息
                }
            }
        }

        out.add(message);
    }

    /**
     * 驗證用戶 token 是否合法
     * 
     * @param connection 用戶連接
     * @param header     消息頭
     * @param token      JWT token
     * @throws SocketException 當 token 驗證失敗時拋出
     */
    private void validateUserToken(ChatUserConnection connection, ChatUserHeader header, String token)
            throws SocketException {
        if (token == null || token.trim().isEmpty()) {
            logger.warn("Empty or null token for user: {}", header.getUserId());
            throw new SocketException("Token is empty or null for user: " + header.getUserId());
        }

        try {
            if (!TokenUtil.validateToken(token)) {
                throw new SocketException("Token validation failed");
            }
            // 解析 token 獲取聲明
            Claims claims = TokenUtil.getClaims(token);
            // 從聲明中獲取 userId
            String userId = claims.getSubject();
            // 檢查 token 中的 userId 是否與 header 中的 userId 一致
            if (userId != null && userId.equals(header.getUserId())) {
                logger.debug("Token validation successful for user: {}", userId);
            } else {
                logger.warn("Token validation failed: userId mismatch. Token userId: {}, Header userId: {}",
                        userId, header.getUserId());
                throw new SocketException("Token validation failed: userId mismatch for user: " + header.getUserId());
            }
        } catch (SocketException e) {
            // 重新拋出 SocketException
            throw e;
        } catch (Exception e) {
            logger.error("Token validation failed for user {}: {}", header.getUserId(), e.getMessage());
            throw new SocketException(
                    "Token validation failed for user: " + header.getUserId() + ", reason: " + e.getMessage());
        }
    }
}
