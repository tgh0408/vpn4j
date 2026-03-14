package org.ssl.vpn4j.event.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.ssl.vpn4j.domain.Black;
import org.ssl.vpn4j.event.BlackReleaseEvent;
import org.ssl.vpn4j.mapper.BlackMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class BlackReleaseEventListener {
    final BlackMapper blackMapper;

    @EventListener
    @Async
    public void onApplicationEvent(BlackReleaseEvent event) {
        Black release = event.getRelease();
        blackMapper.deleteById(release.getId());
        log.info("临时冻结已到期，已自动解冻：{}", release.getData1());
    }
}
