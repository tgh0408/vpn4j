package org.ssl.vpn4j.schedule;

import org.springframework.stereotype.Component;
import org.ssl.common.core.utils.SpringUtils;
import org.ssl.common.sse.utils.SseMessageUtils;
import org.ssl.vpn4j.event.SseCpuPushEvent;
import org.ssl.vpn4j.schedule.cron.VpnSchedule;

@Component
public class CpuPushJob implements VpnSchedule {
    @Override
    public String getScheduleName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public String getScheduleDescription() {
        return "CPU Push Job";
    }

    @Override
    public String getCron() {
        return "0/3 * * * * ?";
    }

    @Override
    public void run() {
        /*
          网页无人在线则无需计算
         */
        if (!SseMessageUtils.isEmpty()){
            SpringUtils.context().publishEvent(new SseCpuPushEvent());
        }
    }
}
