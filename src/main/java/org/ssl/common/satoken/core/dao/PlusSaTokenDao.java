package org.ssl.common.satoken.core.dao;

import cn.dev33.satoken.dao.auto.SaTokenDaoBySessionFollowObject;
import cn.dev33.satoken.util.SaFoxUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Sa-Token 持久层实现 (Caffeine 本地缓存增强版)
 * 适配 Caffeine 3.2.3，支持动态 TTL、类型安全获取及数据搜索
 */
public class PlusSaTokenDao implements SaTokenDaoBySessionFollowObject {

    // 缓存容器：存储包装后的对象
    private static final Cache<String, TokenValueWrapper> CAFFEINE = Caffeine.newBuilder()
            .initialCapacity(100)
            .maximumSize(10000)
            .build();

    /**
     * 数据包装类：手动管理到期时间戳
     *
     * @param expireTime 绝对到期毫秒时间戳，-1表示永不过期
     */
        private record TokenValueWrapper(Object data, long expireTime) {
            private TokenValueWrapper(Object data, long expireTime) {
                this.data = data;
                this.expireTime = (expireTime == -1)
                        ? -1
                        : System.currentTimeMillis() + (expireTime * 1000);
            }

            boolean isExpired() {
                return expireTime != -1 && System.currentTimeMillis() > expireTime;
            }

            long getRemainingSeconds() {
                if (expireTime == -1) return -1;
                long res = (expireTime - System.currentTimeMillis()) / 1000;
                return res > 0 ? res : -2;
            }
        }

    // --- String 类型读写 (全部转发至 Object 方法) ---

    @Override
    public String get(String key) {
        return (String) getObject(key);
    }

    @Override
    public void set(String key, String value, long timeout) {
        setObject(key, value, timeout);
    }

    @Override
    public void update(String key, String value) {
        updateObject(key, value);
    }

    @Override
    public void delete(String key) {
        deleteObject(key);
    }

    @Override
    public long getTimeout(String key) {
        return getObjectTimeout(key);
    }

    @Override
    public void updateTimeout(String key, long timeout) {
        updateObjectTimeout(key, timeout);
    }

    // --- Object 类型读写 (核心逻辑) ---

    @Override
    public Object getObject(String key) {
        TokenValueWrapper wrapper = CAFFEINE.getIfPresent(key);
        if (wrapper == null) return null;
        if (wrapper.isExpired()) {
            CAFFEINE.invalidate(key); // 发现过期立即清理
            return null;
        }
        return wrapper.data;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getObject(String key, Class<T> classType) {
        Object obj = getObject(key);
        if (classType.isInstance(obj)) {
            return (T) obj;
        }
        return null;
    }

    @Override
    public void setObject(String key, Object object, long timeout) {
        if (timeout == 0 || timeout <= -2) return;
        CAFFEINE.put(key, new TokenValueWrapper(object, timeout));
    }

    @Override
    public void updateObject(String key, Object object) {
        long expire = getObjectTimeout(key);
        if (expire != -2) { // 只有没过期的才更新内容
            setObject(key, object, expire);
        }
    }

    @Override
    public void deleteObject(String key) {
        CAFFEINE.invalidate(key);
    }

    @Override
    public long getObjectTimeout(String key) {
        TokenValueWrapper wrapper = CAFFEINE.getIfPresent(key);
        if (wrapper == null) return -2;
        return wrapper.getRemainingSeconds();
    }

    @Override
    public void updateObjectTimeout(String key, long timeout) {
        Object obj = getObject(key);
        if (obj != null) {
            setObject(key, obj, timeout);
        }
    }

    // --- 搜索功能 ---

    @Override
    public List<String> searchData(String prefix, String keyword, int start, int size, boolean sortType) {
        List<String> list = new ArrayList<>();
        // 这里的 asMap() 返回当前内存快照
        for (Map.Entry<String, TokenValueWrapper> entry : CAFFEINE.asMap().entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(prefix) && key.contains(keyword)) {
                if (!entry.getValue().isExpired()) {
                    list.add(key);
                }
            }
        }
        return SaFoxUtil.searchList(list, start, size, sortType);
    }
}