package org.ssl.vpn4j.schedule;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.ssl.vpn4j.domain.Account;
import org.ssl.vpn4j.mapper.AccountMapper;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "vpn4j.check-expire", name = "enable", havingValue = "true")
public class DisableUserJob {

    private final AccountMapper accountMapper;

    @Scheduled(cron = "*/5 * * * * ?")  // 改为每5分钟执行一次
    @Async("scheduledExecutorService")
    public void disableUser() {
        try {
            long executeTime = Instant.now().getEpochSecond();
            // 查询已过期但还未禁用的账号
            List<Account> expiredAccounts = findExpiredAccounts(executeTime);
            if (!expiredAccounts.isEmpty()) {
                disableAccountsInBatch(expiredAccounts);
            }
        } catch (Exception e) {
            log.error("账号过期检查任务执行失败", e);
        }
    }

    /**
     * 查询已过期的账号
     */
    private List<Account> findExpiredAccounts(long currentTime) {
        LambdaQueryWrapper<Account> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper
                .isNotNull(Account::getExpireTime)
                .le(Account::getExpireTime, currentTime)
                .eq(Account::getStatus, "1");  // 状态为正常

        return accountMapper.selectList(queryWrapper);
    }

    /**
     * 批量禁用账号
     */
    private void disableAccountsInBatch(List<Account> accounts) {
        List<Long> accountIds = accounts.stream()
                .map(Account::getId)
                .collect(Collectors.toList());

        LambdaUpdateWrapper<Account> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper
                .in(Account::getId, accountIds)
                .set(Account::getStatus, "0")
                .set(Account::getUpdateTime, new Date());  // 添加更新时间

        int updatedCount = accountMapper.update(null, updateWrapper);

        if (updatedCount != accountIds.size()) {
            log.warn("批量更新数量不匹配，期望：{}，实际：{}",
                    accountIds.size(), updatedCount);
        }

        // 记录详细日志（生产环境可根据需要调整级别）
        accounts.forEach(account ->
                log.debug("已禁用过期账号，用户名：{}，过期时间：{}",
                        account.getUsername(),
                        account.getExpireTime()));

    }
}
