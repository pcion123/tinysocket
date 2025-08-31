package com.vscodelife.serversocket.component;

@FunctionalInterface
public interface ProtocolCatcher<T, E extends Exception> {
    void accept(T t) throws E;
}
