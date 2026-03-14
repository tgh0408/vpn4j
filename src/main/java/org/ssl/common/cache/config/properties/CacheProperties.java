package org.ssl.common.cache.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Data
@Component
@ConfigurationProperties(prefix = "cache")
public class CacheProperties {
    /** 过期时间（秒） */
    private long expire = 3600;

    private TimeUnit expireUnit = TimeUnit.SECONDS;

    /** 初始容量 */
    private int initialCapacity = 100;

    /** 最大容量 */
    private int maxCapacity = 1024;
}