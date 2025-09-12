package com.vscodelife.demo.webserver.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vscodelife.demo.webserver.ChatUserConnection;
import com.vscodelife.demo.webserver.ChatUserHeader;
import com.vscodelife.demo.webserver.ChatWebServer;
import com.vscodelife.socketio.buffer.JsonMapBuffer;
import com.vscodelife.socketio.message.JsonMessage;
import com.vscodelife.socketio.message.base.CacheBase;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * 聊天消息處理器 - WebSocket版本
 * 負責處理業務邏輯消息
 */
public class ChatMessageHandler extends SimpleChannelInboundHandler<JsonMessage<ChatUserHeader>> {
    private static final Logger logger = LoggerFactory.getLogger(ChatMessageHandler.class);

    private final ChatWebServer socket;
    private final CacheBase<JsonMessage<ChatUserHeader>, JsonMapBuffer> cacheManager;

    public ChatMessageHandler(ChatWebServer socket) {
        this.socket = socket;
        this.cacheManager = socket.getCacheBase();
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, JsonMessage<ChatUserHeader> msg) throws Exception {
        ChatUserConnection connection = socket.getConnection(ctx.channel());
        if (connection != null) {
            // 設置更新時間
            connection.setTimeout(System.currentTimeMillis() + 5 * 60 * 1000);
            // 設定檔頭
            ChatUserHeader header = msg.getHeader();
            int mainNo = header.getMainNo();
            int subNo = header.getSubNo();
            long requestId = header.getRequestId();
            String userId = header.getUserId();
            String token = header.getToken();
            // 檢查是否有快取消息
            if (cacheManager.isEnabled() && cacheManager.isIncluded(msg)) {
                JsonMessage<ChatUserHeader> preMessage = cacheManager.peekMessage(userId, mainNo, subNo, requestId);
                if (preMessage != null) {
                    ctx.channel().writeAndFlush(preMessage);
                    return;
                }
            }
            // 校正用戶編號
            header.setUserId(connection.getUserId());
            // 校正session編號
            header.setSessionId(connection.getSessionId());
            // 校正IP地址
            header.setIp(connection.getIp());
            // 放入協定佇列
            socket.putMessage(msg);
            logger.debug("put message - [{},{}] - {} - {} - {} - {} - {}", mainNo,
                    subNo, userId, connection.getAddress(), connection.getSessionId(),
                    requestId, token);
        }
    }
}
