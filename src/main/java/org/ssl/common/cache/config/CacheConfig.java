package org.ssl.common.cache.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.ssl.common.cache.config.properties.CacheProperties;
import org.ssl.common.cache.manager.LoaclCacheManager;


@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * 自定义缓存管理器 整合spring-cache
     */
    @Bean
    public CacheManager cacheManager(CacheProperties cacheProperties) {
        return new LoaclCacheManager(cacheProperties);
    }

}

