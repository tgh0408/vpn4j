package org.ssl.vpn4j.cache;


import org.ssl.common.core.utils.SpringUtils;
import org.ssl.vpn4j.domain.Account;
import org.ssl.vpn4j.event.SettlementEvent;
import org.ssl.vpn4j.event.VpnStatusInfo;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * vpn在线用户缓存
 */

public class VpnOnlineCache {

    private static final Map<String, VpnStatusInfo> ONLINE_CACHE_INFO = new ConcurrentHashMap<>();

    /**
     * 等待结算
     * 主要是为了解决用户断线重连时，流量统计的准确性
     * 当用户断线重连时，会将用户之前断线期间的流量统计进行结算 也有可能存在多个未结算的流量统计
     * 相当于作了一个快照,秒断重联vpn会清除之前的流量统计，我们的流量结算又存在延迟，如果这时候用户重新连接,会出现3秒前的数据大于当前数据导致流量变为负数
     */
    //发送结算事件
    public static void settlement(String username) {
        VpnStatusInfo vpnStatusInfo = ONLINE_CACHE_INFO.get(username);
        SpringUtils.context().publishEvent(new SettlementEvent(Map.of(username, vpnStatusInfo)));
        remove(username);
    }

    public static void settlement(Set<String> usernames) {
        Map<String, VpnStatusInfo> settlementMap = usernames.stream()
                .filter(ONLINE_CACHE_INFO::containsKey)
                .collect(Collectors.toMap(
                        key -> key,
                        ONLINE_CACHE_INFO::get
                ));
        SpringUtils.context().publishEvent(new SettlementEvent(settlementMap));
        removeAll(settlementMap.keySet());
    }

    public static void setIfAbsent(String username, VpnStatusInfo vpnStatusInfo) {
        ONLINE_CACHE_INFO.putIfAbsent(username, vpnStatusInfo);
    }

    public static void setOnlineCacheInfo(String username, VpnStatusInfo vpnStatusInfo) {
        ONLINE_CACHE_INFO.put(username, vpnStatusInfo);
    }

    public static VpnStatusInfo getOnlineCacheInfo(String username) {
        return ONLINE_CACHE_INFO.get(username);
    }

    public static void remove(String username) {
        ONLINE_CACHE_INFO.remove(username);
    }

    public static void removeAll() {
        ONLINE_CACHE_INFO.clear();
    }

    public static void removeAll(Collection<String> usernames) {
        ONLINE_CACHE_INFO.keySet().removeAll(usernames);
    }

    public static Set<String> keySet() {
        return ONLINE_CACHE_INFO.keySet();
    }

    public static Collection<VpnStatusInfo> getOnlineAll() {
        return ONLINE_CACHE_INFO.values();
    }

    //从数据库恢复状态
    public static void recover(List<Account> accounts) {
        accounts.forEach(account -> {
            VpnStatusInfo vpnStatusInfo = ONLINE_CACHE_INFO.get(account.getUsername());
            if (vpnStatusInfo == null) {
                VpnStatusInfo statusInfo = new VpnStatusInfo();
                statusInfo.setUsername(account.getUsername());
                ONLINE_CACHE_INFO.put(account.getUsername(), statusInfo);
            }
        });
    }

}
