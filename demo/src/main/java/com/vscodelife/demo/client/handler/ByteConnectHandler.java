package com.vscodelife.demo.client.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vscodelife.clientsocket.Connector;
import com.vscodelife.demo.client.ByteUserHeader;
import com.vscodelife.socketio.buffer.ByteArrayBuffer;
import com.vscodelife.socketio.message.ByteMessage;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;

public class ByteConnectHandler extends ChannelDuplexHandler {
    private static final Logger logger = LoggerFactory.getLogger(ByteConnectHandler.class);

    private final Connector<ByteUserHeader, ByteMessage<ByteUserHeader>, ByteArrayBuffer> connector;

    public ByteConnectHandler(Connector<ByteUserHeader, ByteMessage<ByteUserHeader>, ByteArrayBuffer> connector) {
        this.connector = connector;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        connector.onConnected(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        connector.onDisconnected(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        connector.onException(ctx, cause);
    }
}
