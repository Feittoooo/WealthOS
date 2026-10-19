package com.wealthos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

@Configuration
@EnableScheduling
public class AppConfig {

    // injected wherever "today" matters, so tests can pin the date
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
