package com.jimbean.shenyu.client.core.annotation;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * @author zhangjb <br/>
 * @date 2022-08-08 16:16 <br/>
 * @email: <a href="mailto:zhangjb03@c5game.com">zhangjb</a> <br/>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Inherited
public @interface GateWay {

    /**
     * Value string.
     *
     * @return the string
     */
    @AliasFor(attribute = "contextPath")
    String value() default "";

    /**
     * Path string.
     *
     * @return the string
     */
    @AliasFor(attribute = "value")
    String contextPath() default "";
}
