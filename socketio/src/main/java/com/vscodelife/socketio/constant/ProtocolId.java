package com.vscodelife.socketio.constant;

import com.vscodelife.socketio.message.base.ProtocolKey;

public class ProtocolId {
    public static final ProtocolKey PING = new ProtocolKey(0, 0);
    public static final ProtocolKey AUTH = new ProtocolKey(0, 1);
    public static final ProtocolKey AUTH_RESULT = new ProtocolKey(0, 2);
    public static final ProtocolKey NOTIFY_SESSION_ID = new ProtocolKey(0, 126);
    public static final ProtocolKey DISCONNECT = new ProtocolKey(0, 127);
}
