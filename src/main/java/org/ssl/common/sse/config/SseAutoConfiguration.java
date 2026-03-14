package org.ssl.common.sse.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.ssl.common.sse.core.SseEmitterManager;

import java.util.concurrent.ScheduledExecutorService;

/**
 * SSE 自动装配
 *
  
 */
@Configuration
@ConditionalOnProperty(value = "sse.enabled", havingValue = "true")
@EnableConfigurationProperties(SseProperties.class)
public class SseAutoConfiguration {

    @Bean
    public SseEmitterManager sseEmitterManager(ScheduledExecutorService scheduledExecutorService) {
        return new SseEmitterManager(scheduledExecutorService);
    }

}
