package org.ssl.vpn4j.schedule;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.ssl.common.sse.utils.SseMessageUtils;
import org.ssl.vpn4j.event.SseCpuPushEvent;
import org.ssl.vpn4j.event.SsePushEvent;

@Component
@RequiredArgsConstructor
public class SystemInfoPushJob {
    final ApplicationEventPublisher applicationEventPublisher;

    @Scheduled(cron = "0/3 * * * * ?")
    @Async("scheduledExecutorService")
    public void push() {
        /*
          网页无人在线则无需计算
         */
        if (!SseMessageUtils.isEmpty()){
            applicationEventPublisher.publishEvent(new SsePushEvent());
        }
    }

    @Scheduled(cron = "0/3 * * * * ?")
    @Async("scheduledExecutorService")
    public void cpuPush() {
        /*
          网页无人在线则无需计算
         */
        if (!SseMessageUtils.isEmpty()){
            applicationEventPublisher.publishEvent(new SseCpuPushEvent());
        }
    }
}
