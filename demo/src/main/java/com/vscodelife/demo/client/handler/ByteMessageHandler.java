package com.vscodelife.demo.client.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vscodelife.clientsocket.Connector;
import com.vscodelife.demo.client.ByteUserHeader;
import com.vscodelife.socketio.buffer.ByteArrayBuffer;
import com.vscodelife.socketio.message.ByteMessage;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class ByteMessageHandler extends SimpleChannelInboundHandler<ByteMessage<ByteUserHeader>> {
    private static final Logger logger = LoggerFactory.getLogger(ByteMessageHandler.class);

    private final Connector<ByteUserHeader, ByteMessage<ByteUserHeader>, ByteArrayBuffer> connector;

    public ByteMessageHandler(Connector<ByteUserHeader, ByteMessage<ByteUserHeader>, ByteArrayBuffer> connector) {
        this.connector = connector;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteMessage<ByteUserHeader> msg) throws Exception {
        if (connector != null && connector.isConnected()) {
            connector.onReceiveMessage(ctx, msg);
        }
    }
}
