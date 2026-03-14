package org.ssl.vpn4j.event.listener;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.ssl.common.core.utils.SpringUtils;
import org.ssl.common.event.SseConnectedEvent;
import org.ssl.vpn4j.event.SsePushEvent;

/**
 * sse连接成功监听器
 */
@Component
public class SseConnectListener {

    @EventListener
    @Async
    public void handleSseConnectEvent(SseConnectedEvent event) {
        // 处理sse连接成功事件 用户上线立马推送一次状态
        SpringUtils.context().publishEvent(new SsePushEvent());
    }
}
