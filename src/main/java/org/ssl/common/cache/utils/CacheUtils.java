package org.ssl.common.cache.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.ssl.common.cache.CacheImpl.CaffeineCacheDecorator;
import org.ssl.common.core.utils.SpringUtils;
import org.ssl.vpn4j.domain.VpnService;
import org.ssl.vpn4j.enums.SystemConfigEnum;
import org.ssl.vpn4j.mapper.VpnServiceMapper;

import java.util.Map;
import java.util.Objects;

/**
 * 缓存操作工具类
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings(value = {"unchecked"})
public class CacheUtils {

    private static final CacheManager CACHE_MANAGER = SpringUtils.getBean(CacheManager.class);
    private static final VpnServiceMapper VPN_SERVICE_MAPPER = SpringUtils.getBean(VpnServiceMapper.class);

    /**
     * 获取缓存值
     *
     * @param cacheNames 缓存组名称
     * @param key        缓存key
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(String cacheNames, Object key) {
        String defaultValue = null;
        String desc = null;
        boolean isSystemConfig = false;

        // 1. 处理枚举 key 的转换
        if (key instanceof SystemConfigEnum config) {
            defaultValue = config.getDefaultValue();
            desc = config.getDesc();
            key = config.getKey();
            isSystemConfig = true;
        }

        Cache cache = Objects.requireNonNull(CACHE_MANAGER.getCache(cacheNames));
        Cache.ValueWrapper wrapper = cache.get(key);

        // 2. 缓存未命中且属于系统配置项
        if (wrapper == null && isSystemConfig) {
            String finalKey = (String) key;
            // 使用同步锁防止高并发下的缓存击穿
            synchronized (finalKey.intern()) {
                // 双重检查锁定 (DCL)
                wrapper = cache.get(finalKey);
                if (wrapper == null) {
                    return fetchFromDbAndCache(cache, finalKey, defaultValue, desc);
                }
            }
        }

        return wrapper != null ? (T) wrapper.get() : null;
    }

    private static <T> T fetchFromDbAndCache(Cache cache, String key, String defaultValue, String desc) {
        VpnService vpnService = VPN_SERVICE_MAPPER.selectById(key);

        if (vpnService == null) {
            // 数据库不存在，则插入默认值
            vpnService = new VpnService(key, defaultValue, desc);
            VPN_SERVICE_MAPPER.insert(vpnService);
        }

        // 写入缓存
        cache.put(key, vpnService.getValue1());
        return (T) vpnService.getValue1();
    }

    /**
     * 获取缓存值 默认从全局缓存获取
     *
     * @param key 缓存key
     */
    public static <T> T get(Object key) {
        return get(SystemConfigEnum.cacheName, key);
    }

    /**
     * 获取所有缓存值
     *
     * @param cacheNames 缓存组名称
     */
    public static <K, V> Map<K, V> getAll(String cacheNames) {
        Cache nativeCache = CACHE_MANAGER.getCache(cacheNames);

        // 根据实际缓存类型实现
        if (nativeCache instanceof CaffeineCacheDecorator) {
            return ((CaffeineCacheDecorator) nativeCache).getAll();
        }
        Objects.requireNonNull(nativeCache);
        throw new UnsupportedOperationException("Cannot list all entries for cache type: " + nativeCache.getClass().getName());
    }

    /**
     * 保存缓存值
     *
     * @param cacheNames 缓存组名称
     * @param key        缓存key
     * @param value      缓存值
     */
    public static void put(String cacheNames, Object key, Object value) {
        Objects.requireNonNull(CACHE_MANAGER.getCache(cacheNames)).put(key, value);
    }

    public static void put(Object key, Object value) {
        if (key instanceof SystemConfigEnum) {
            key = ((SystemConfigEnum) key).getKey();
        }
        put(SystemConfigEnum.cacheName, key, value);
    }

    /**
     * 删除缓存值
     *
     * @param cacheNames 缓存组名称
     * @param key        缓存key
     */
    public static void evict(String cacheNames, Object key) {
        Objects.requireNonNull(CACHE_MANAGER.getCache(cacheNames)).evict(key);
    }

    /**
     * 删除缓存值 效果同evict 单纯方便记忆
     *
     * @param cacheNames 缓存组名称
     * @param key        缓存key
     */
    public static void remove(String cacheNames, Object key) {
        evict(cacheNames, key);
    }

    /**
     * 清空缓存值
     *
     * @param cacheNames 缓存组名称
     */
    public static void clear(String cacheNames) {
        Objects.requireNonNull(CACHE_MANAGER.getCache(cacheNames)).clear();
    }


}

