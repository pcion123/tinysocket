package com.vscodelife.demo.server.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vscodelife.demo.server.ByteUserConnection;
import com.vscodelife.demo.server.ByteUserHeader;
import com.vscodelife.demo.server.TestByteServer;
import com.vscodelife.socketio.buffer.ByteArrayBuffer;
import com.vscodelife.socketio.message.ByteMessage;
import com.vscodelife.socketio.message.base.CacheBase;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class ByteMessageHandler extends SimpleChannelInboundHandler<ByteMessage<ByteUserHeader>> {
    private static final Logger logger = LoggerFactory.getLogger(ByteMessageHandler.class);

    private final TestByteServer socket;
    private final CacheBase<ByteMessage<ByteUserHeader>, ByteArrayBuffer> cacheManager;

    public ByteMessageHandler(TestByteServer socket) {
        this.socket = socket;
        this.cacheManager = socket.getCacheBase();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteMessage<ByteUserHeader> msg) throws Exception {
        ByteUserConnection connection = socket.getConnection(ctx.channel());
        if (connection != null) {
            // 設置更新時間
            connection.setTimeout(System.currentTimeMillis() + 5 * 60 * 1000);
            // 設定檔頭
            ByteUserHeader header = msg.getHeader();
            int mainNo = header.getMainNo();
            int subNo = header.getSubNo();
            long requestId = header.getRequestId();
            String userId = header.getUserId();
            String token = header.getToken();
            // 檢查是否有快取消息
            if (cacheManager.isEnabled() && cacheManager.isIncluded(msg)) {
                ByteMessage<ByteUserHeader> preMessage = cacheManager.peekMessage(userId, mainNo, subNo, requestId);
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
