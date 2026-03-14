package org.ssl.common.cache.manager;


import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.ssl.common.cache.CacheImpl.CaffeineCacheDecorator;
import org.ssl.common.cache.config.properties.CacheProperties;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 全局缓存管理器
 */

public class LoaclCacheManager implements CacheManager {

    private final CacheProperties cacheProperties;
    private final Map<String, Cache> instanceMap = new ConcurrentHashMap<>();

    public LoaclCacheManager(CacheProperties cacheProperties) {
        this.cacheProperties = cacheProperties;
    }

    @Override
    public @Nullable Cache getCache(@NonNull String name) {
        Cache cache = instanceMap.get(name);
        if (cache == null) {
            cache = new CaffeineCacheDecorator(name, cacheProperties);
            instanceMap.put(name, cache);
        }
        return cache;
    }

    @Override
    @NullMarked
    public Collection<String> getCacheNames() {
        return instanceMap.keySet();
    }

}
