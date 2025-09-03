package com.vscodelife.demo.constant;

import com.vscodelife.socketio.message.base.ProtocolKey;

public class ProtocolId extends com.vscodelife.socketio.constant.ProtocolId {
    public static final ProtocolKey ONLINE = new ProtocolKey(1, 1);
    public static final ProtocolKey OFFLINE = new ProtocolKey(1, 2);
    public static final ProtocolKey GET_USER_LIST = new ProtocolKey(1, 3);
    public static final ProtocolKey SAY = new ProtocolKey(1, 4);
    public static final ProtocolKey MESSAGE = new ProtocolKey(1, 5);
}
