package org.ssl.vpn4j.schedule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;
import org.ssl.common.core.exception.ServiceException;
import org.ssl.vpn4j.schedule.cron.VpnSchedule;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

/**
 * 动态任务调度器
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicTaskScheduler implements ApplicationRunner {

    private final TaskScheduler taskScheduler;
    private final Map<String, ScheduledFuture<?>> scheduledFuture = new HashMap<>();

    final List<VpnSchedule> cronScheduler;

    /**
     * 动态更新任务
     */
    public synchronized void updateTask(VpnSchedule taskSchedule, boolean open) {
        if (taskSchedule == null) {
            throw new ServiceException("task not found: {} 任务不存在");
        }
        // 校验表达式
        if (!checkCron(taskSchedule.getCron())) {
            throw new ServiceException("Cron expression is invalid: {} 校验失败", taskSchedule.getCron());
        }
        // 1. 立即停止当前调度
        ScheduledFuture<?> future = scheduledFuture.get(taskSchedule.getScheduleName());
        if (future != null) {
            future.cancel(false);
            scheduledFuture.remove(taskSchedule.getScheduleName());
        }
        if (!open) {
            return;
        }
        // 2. 重新启动
        // 这里的 Lambda 表达式会在虚拟线程中执行
        scheduledFuture.put(taskSchedule.getScheduleName(),
                taskScheduler.schedule(taskSchedule::run, new CronTrigger(taskSchedule.getCron())));

        log.info("Updated Cron task: {}:{} -> {}", taskSchedule.getScheduleName(), taskSchedule.getScheduleDescription(), taskSchedule.getCron());
    }

    @Override
    public void run(@NonNull ApplicationArguments args) {
        for (VpnSchedule cron : cronScheduler) {
            updateTask(cron, true);
        }
    }

    public boolean checkCron(String cronExpression) {
        // 直接调用，返回 true 或 false
        return CronExpression.isValidExpression(cronExpression);
    }
}