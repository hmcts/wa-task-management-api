package uk.gov.hmcts.reform.wataskmanagementapi.schedulers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.services.MIReportingService;

/**
 * This scheduler checks if logical replication is in place.
 */
@Slf4j
@Component
@Profile("replica | preview")
public class LogicalReplicationCreatorScheduler {
    private static final int FIXED_DELAY_CONST = 30_000;
    private final MIReportingService miReportingService;

    @Autowired
    public LogicalReplicationCreatorScheduler(MIReportingService miReportingService) {
        this.miReportingService = miReportingService;
    }

    @Scheduled(fixedDelay = FIXED_DELAY_CONST)
    public void scheduled() {
        log.debug("Postgresql logical replication scheduler executed");
        miReportingService.logicalReplicationCheck();
    }
}
