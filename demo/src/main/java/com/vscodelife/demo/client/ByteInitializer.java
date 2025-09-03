package com.vscodelife.demo.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vscodelife.clientsocket.Connector;
import com.vscodelife.demo.client.handler.ByteConnectHandler;
import com.vscodelife.demo.client.handler.ByteHeaderDecoderHandler;
import com.vscodelife.demo.client.handler.ByteHeaderEncoderHandler;
import com.vscodelife.demo.client.handler.ByteMessageHandler;
import com.vscodelife.socketio.buffer.ByteArrayBuffer;
import com.vscodelife.socketio.message.ByteMessage;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;

public class ByteInitializer extends ChannelInitializer<SocketChannel> {
    private static Logger logger = LoggerFactory.getLogger(ByteInitializer.class);

    private final Connector<ByteUserHeader, ByteMessage<ByteUserHeader>, ByteArrayBuffer> connector;

    public ByteInitializer(Connector<ByteUserHeader, ByteMessage<ByteUserHeader>, ByteArrayBuffer> connector) {
        this.connector = connector;
    }

    @Override
    public void initChannel(SocketChannel channel) throws Exception {
        ChannelPipeline pipeline = channel.pipeline();
        pipeline.addLast("encoder", new ByteHeaderEncoderHandler(connector));
        pipeline.addLast("decoder", new ByteHeaderDecoderHandler(connector));
        pipeline.addLast("message", new ByteMessageHandler(connector));
        pipeline.addLast("connect", new ByteConnectHandler(connector));
        logger.debug("client {} is connected", channel.remoteAddress());
    }
}
