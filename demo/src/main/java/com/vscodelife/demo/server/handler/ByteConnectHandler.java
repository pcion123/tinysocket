package com.vscodelife.demo.server.handler;

import java.io.IOException;
import java.net.SocketException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vscodelife.demo.server.ByteUserConnection;
import com.vscodelife.demo.server.TestByteServer;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;

public class ByteConnectHandler extends ChannelDuplexHandler {
    private static final Logger logger = LoggerFactory.getLogger(ByteConnectHandler.class);

    private final TestByteServer socket;

    public ByteConnectHandler(TestByteServer socket) {
        this.socket = socket;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // 取得當前連線的channel
        Channel incoming = ctx.channel();
        // 將連線加入管理
        socket.putConnection(incoming);
        logger.info("client: {} is online channelId={}", incoming.remoteAddress(), incoming.id());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // 取得當前連線的channel
        Channel incoming = ctx.channel();
        // 將連線從管理中移除
        ByteUserConnection connection = socket.removeConnection(incoming);
        if (connection != null) {
            connection.destroy();
        }
        logger.info("client: {} is offline channelId={}", incoming.remoteAddress(), incoming.id());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
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
