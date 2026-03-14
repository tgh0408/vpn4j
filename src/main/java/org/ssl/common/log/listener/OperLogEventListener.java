package org.ssl.common.log.listener;

import cn.hutool.core.bean.BeanUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.ssl.common.log.event.OperLogEvent;
import org.ssl.vpn4j.domain.Syslog;
import org.ssl.vpn4j.mapper.SyslogMapper;

@Component
@RequiredArgsConstructor
public class OperLogEventListener {
    final SyslogMapper syslogMapper;

    @EventListener
    public void onApplicationEvent(OperLogEvent event) {
        Syslog syslog = BeanUtil.copyProperties(event, Syslog.class);
        syslogMapper.insert(syslog);
    }
}
