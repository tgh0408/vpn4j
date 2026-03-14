package org.ssl.vpn4j.event.listener;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.ssl.common.cache.utils.CacheUtils;
import org.ssl.vpn4j.cache.VpnOnlineCache;
import org.ssl.vpn4j.cache.constant.VpnConstant;
import org.ssl.vpn4j.domain.Account;
import org.ssl.vpn4j.domain.Black;
import org.ssl.vpn4j.enums.BlackTypeEnum;
import org.ssl.vpn4j.event.OfflineUserEvent;
import org.ssl.vpn4j.event.VpnStatusInfo;
import org.ssl.vpn4j.mapper.AccountMapper;
import org.ssl.vpn4j.mapper.BlackMapper;
import org.ssl.vpn4j.runner.ManageRunner;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 用户下线监听器
 */

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "vpn4j.manager", name = "enable", havingValue = "true") // 核心修改：条件注入
public class VpnOfflineEventListener {
    final ManageRunner manageRunner;
    final AccountMapper accountMapper;
    final BlackMapper blackMapper;

    // TODO 离线需要结算流量 包含主动下线与强制下线
    @EventListener
    @Transactional(rollbackFor = Exception.class)
    public void onVpnOffline(OfflineUserEvent event) {
        Set<String> offlineUsers = event.getOfflineUsers();
        if (offlineUsers.isEmpty()) {
            return;
        }
        // 判断是否是强制下线,强制下线则需要踢掉用户
        for (String username : offlineUsers){
            if (!event.isForce()){
                //主动下线无需发送强制命令
                break;
            }
            VpnStatusInfo onlineCacheInfo = VpnOnlineCache.getOnlineCacheInfo(username);
            if (onlineCacheInfo != null && onlineCacheInfo.getPoolRemoteIp() != null){
                if (manageRunner == null){
                    log.error("VPN用户 [{}] 强制下线失败, 无法获取管理器", username);
                    continue;
                }
                String comment = "kill " + onlineCacheInfo.getUsername();
                manageRunner.getManageWriter().println(comment);
                log.info("VPN用户 [{}] 强制下线, 向管理器发送命令: {}", username, comment);
                //将用户加入黑名单五分钟,防止用户重新连接
                Black black = new Black();
                black.setType(BlackTypeEnum.USERNAME.getType());
                black.setData1(username);
                black.setDuration(5 * 60);
                black.setReleaseTime(LocalDateTime.now().plusMinutes(5));
                black.setNotes("管理员强制下线临时冻结");
                blackMapper.insert(black);
                CacheUtils.put(VpnConstant.VPN_BLACK_NAME, username, black);
            }
        }
        if (event.isForce()){
            //强制下线不需要结算,只需要踢掉用户，轮询任务会处理
            return;
        }
        Account account = new Account();
        account.setOnline("0");
        LambdaQueryWrapper<Account> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Account::getUsername, offlineUsers);
        int update = accountMapper.update(account, wrapper);
        if (update == 0){
            log.warn("VPN用户离线更新状态失败: {}", offlineUsers);
        }
        log.warn("VPN用户离线: {}", offlineUsers);
        VpnOnlineCache.settlement(offlineUsers);

    }
}
