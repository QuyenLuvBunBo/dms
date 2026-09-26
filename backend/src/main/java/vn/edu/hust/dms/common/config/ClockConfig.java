package vn.edu.hust.dms.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * The single source of "now" for the application. Every time-based rule injects {@link Clock};
 * tests replace this bean with a mutable clock.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock(@Value("${dms.clock.zone:Asia/Ho_Chi_Minh}") String zone) {
        return Clock.system(ZoneId.of(zone));
    }
}
