package com.vscodelife.demo.webserver.handler;

import java.io.IOException;
import java.net.SocketException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vscodelife.demo.constant.ProtocolId;
import com.vscodelife.demo.webserver.ChatUserConnection;
import com.vscodelife.demo.webserver.ChatWebServer;
import com.vscodelife.socketio.buffer.JsonMapBuffer;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * 聊天連接處理器 - WebSocket版本
 * 負責處理連接和斷開連接事件
 */
public class ChatConnectHandler extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(ChatConnectHandler.class);

    private final ChatWebServer socket;

    public ChatConnectHandler(ChatWebServer socket) {
        this.socket = socket;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // 取得當前連線的channel
        Channel incoming = ctx.channel();
        // 將連線加入管理
        if (socket.putConnection(incoming)) {
            ChatUserConnection connection = socket.getConnection(incoming);
            if (connection != null) {
                JsonMapBuffer response = new JsonMapBuffer();
                response.put("sessionId", connection.getSessionId());
                connection.send(ProtocolId.NOTIFY_SESSION_ID, response);
            }
        }

        logger.info("client: {} is online channelId={}", incoming.remoteAddress(), incoming.id());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // 取得當前連線的channel
        Channel incoming = ctx.channel();
        // 將連線從管理中移除
        ChatUserConnection connection = socket.removeConnection(incoming);
        if (connection != null) {
            connection.destroy();
        }
        logger.info("client: {} is offline channelId={}", incoming.remoteAddress(), incoming.id());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        Channel incoming = ctx.channel();
        if (cause instanceof SocketException) {
            logger.error(String.format("client: %s has exception = %s",
                    incoming.remoteAddress().toString(), cause.getMessage()));
        } else {
            if (cause instanceof IOException) {
                if (cause.getMessage().equals("Connection reset by peer")) {
                    // 不做反應
                } else if (cause.getMessage().equals("連線被對方重設")) {
                    // 不做反應
                } else if (cause.getMessage().equals("遠端主機已強制關閉一個現存的連線。")) {
                    // 不做反應
                } else {
                    logger.error(
                            String.format("client: %s has exception = %s",
                                    incoming.remoteAddress().toString(), cause.getMessage()),
                            cause);
                }
            } else {
                logger.error(String.format("client: %s has exception = %s",
                        incoming.remoteAddress().toString(), cause.getMessage()), cause);
            }
        }
        ctx.close();
    }
}
