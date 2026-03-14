package org.ssl.vpn4j.event.listener;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.ssl.vpn4j.cache.VpnOnlineCache;
import org.ssl.vpn4j.domain.Account;
import org.ssl.vpn4j.event.RecoverStatusEvent;
import org.ssl.vpn4j.mapper.AccountMapper;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class RecoverStatusEventListener {
    final AccountMapper accountMapper;

    @EventListener
    public void handleRecoverStatusEvent(RecoverStatusEvent event) {
        // 解决由于程序退出缓存数据未保存的问题 恢复用户数据库状态
        LambdaQueryWrapper<Account> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Account::getOnline, "1");
        List<Account> accountList = accountMapper.selectList(queryWrapper);
        VpnOnlineCache.recover(accountList);
    }
}
