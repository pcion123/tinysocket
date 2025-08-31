package com.vscodelife.socketio.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ProtocolTag {
    int mainNo();

    int subNo();

    boolean cached() default false;

    boolean safed() default true;

    String describe() default "";
}
