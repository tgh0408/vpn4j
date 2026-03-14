package org.ssl.common.ratelimiter.annotation;

import org.ssl.common.ratelimiter.enums.LimitType;

import java.lang.annotation.*;

/**
 * 限流注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimiter {
    /**
     * 限流key,支持使用Spring el表达式来动态获取方法上的参数值
     * 格式类似于  #code.id #{#code}
     */
    String key() default "";

    /**
     * 是否拼接URL 作为限流key
     * 如果为true 则多个key会互相影响，适用于多个接口共用一个限流key
     */
    boolean shareKey() default false;

    /**
     * 限流时间,单位秒
     */
    int time() default 60;

    /**
     * 限流次数
     */
    int count() default 100;

    /**
     * 限流类型
     */
    LimitType limitType() default LimitType.DEFAULT;

    /**
     * 提示消息 支持国际化 格式为 {code}
     */
    String message() default "{rate.limiter.message}";

    /**
     * 限流策略超时时间 默认一天(策略存活时间 会清除已存在的策略数据)
     */
    int timeout() default 86400;

}
