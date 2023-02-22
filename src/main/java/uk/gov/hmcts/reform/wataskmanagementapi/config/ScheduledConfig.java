package uk.gov.hmcts.reform.wataskmanagementapi.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ComponentScan("uk.gov.hmcts.reform.wataskmanagementapi.schedulers")
public class ScheduledConfig {
}
