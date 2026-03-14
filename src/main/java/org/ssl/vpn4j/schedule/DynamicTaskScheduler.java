package org.ssl.vpn4j.schedule;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.scheduling.support.CronTrigger;
import org.ssl.common.core.exception.ServiceException;
import org.ssl.vpn4j.enums.SystemConfigEnum;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

/**
 * 动态任务调度器
 */

//@Component
@RequiredArgsConstructor
public class DynamicTaskScheduler implements ApplicationRunner {

    private final TaskScheduler taskScheduler;
    private final Map<SystemConfigEnum, ScheduledFuture<?>> scheduledFuture = new HashMap<>();

    /**
     * 动态更新任务
     */
    public synchronized void updateTask(SystemConfigEnum systemConfigEnum, String cron) {
        // 校验表达式
        if (!checkCron(cron)) {
            throw new ServiceException("Cron expression is invalid: {} 校验失败", cron);
        }
        // 1. 立即停止当前调度
        ScheduledFuture<?> future = scheduledFuture.get(systemConfigEnum);
        if (future != null) {
            future.cancel(false);
        } else {
            return;
        }

        // 2. 重新启动
        // 这里的 Lambda 表达式会在虚拟线程中执行
        scheduledFuture.put(systemConfigEnum,
                taskScheduler.schedule(() -> {
//            SpringUtils.context().publishEvent(new MySignalEvent(this));
                }, new CronTrigger(cron)));

        System.out.println("虚拟线程调度器已更新 Cron: " + cron);
    }

    @Override
    public void run(@NonNull ApplicationArguments args) {

    }

    public boolean checkCron(String cronExpression) {
        // 直接调用，返回 true 或 false
        return CronExpression.isValidExpression(cronExpression);
    }
}