package com.vscodelife.serversocket.socket.component;

@FunctionalInterface
public interface ProtocolCatcher <T, E extends Exception> {
    void accept(T t) throws E;
}
