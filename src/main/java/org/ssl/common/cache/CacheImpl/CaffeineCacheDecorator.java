package org.ssl.common.cache.CacheImpl;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.springframework.cache.Cache;
import org.springframework.cache.support.SimpleValueWrapper;
import org.ssl.common.cache.config.properties.CacheProperties;

import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Cache 装饰器模式(用于扩展 Caffeine 一级缓存)
 *
 */
public class CaffeineCacheDecorator implements Cache {
    private final com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache;

    private final String name;

    // 建议：每个实例拥有独立的本地缓存引用，或者在外部统一管理

    public CaffeineCacheDecorator(String name, CacheProperties cacheProperties) {
        this.name = name;
        this.nativeCache = Caffeine.newBuilder()
                // 设置最后一次写入或访问后经过固定时间过期
                .expireAfterWrite(cacheProperties.getExpire(), cacheProperties.getExpireUnit())
                // 初始的缓存空间大小
                .initialCapacity(cacheProperties.getInitialCapacity())
                // 缓存的最大条数
                .maximumSize(cacheProperties.getMaxCapacity()).build();
    }

    @Override
    @NullMarked
    public String getName() {
        return name;
    }

    @Override
    @NullMarked
    public Object getNativeCache() {
        return nativeCache;
    }

    /**
     * Spring Cache 需要返回 ValueWrapper 包装类，而不是直接返回原始对象
     */
    @Override
    public ValueWrapper get(@NonNull Object key) {
        Object value = nativeCache.getIfPresent(key);
        return (value != null ? new SimpleValueWrapper(value) : null);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(@NonNull Object key, Class<T> type) {
        Object value = nativeCache.getIfPresent(key);
        if (value != null && type != null && !type.isInstance(value)) {
            throw new IllegalStateException("Cached value is not of required type [" + type.getName() + "]: " + value);
        }
        return (T) value;
    }

    @Override
    public void put(@NonNull Object key, Object value) {
        nativeCache.put(key, value);
    }

    @Override
    public ValueWrapper putIfAbsent(@NonNull Object key, Object value) {
        Object existing = nativeCache.asMap().putIfAbsent(key, value);
        return (existing != null ? new SimpleValueWrapper(existing) : null);
    }

    @Override
    public void evict(@NonNull Object key) {
        nativeCache.invalidate(key);
    }

    @Override
    public boolean evictIfPresent(@NonNull Object key) {
        if (nativeCache.asMap().containsKey(key)) {
            nativeCache.invalidate(key);
            return true;
        }
        return false;
    }

    @Override
    public void clear() {
        nativeCache.invalidateAll();
    }

    @Override
    public boolean invalidate() {
        clear();
        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(@NonNull Object key, @NonNull Callable<T> valueLoader) {
        // 使用 Caffeine 的原子 load 特性
        return (T) nativeCache.get(key, _ -> {
            try {
                return valueLoader.call();
            } catch (Exception e) {
                throw new ValueRetrievalException(key, valueLoader, e);
            }
        });
    }

    @SuppressWarnings("unchecked")
    public <K, V> Map<K, V> getAll() {
        return (Map<K, V>) nativeCache.asMap();
    }
}
