package com.vscodelife.clientsocket;

import com.vscodelife.socketio.message.base.HeaderBase;
import com.vscodelife.socketio.message.base.MessageBase;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;

public interface IClient<H extends HeaderBase, M extends MessageBase<H, B>, B> {
    Class<? extends SocketBase<H, M, B>> getSocketClazz();

    Class<? extends ChannelInitializer<SocketChannel>> getInitializerClazz();

    void process();

    void onConnected(long connectorId, ChannelHandlerContext ctx);

    void onDisconnected(long connectorId, ChannelHandlerContext ctx);

    void onSendMessage(long connectorId, ChannelHandlerContext ctx, M message);

    void onReceiveMessage(long connectorId, ChannelHandlerContext ctx, M message);

    void onException(long connectorId, ChannelHandlerContext ctx, Throwable cause);
}
