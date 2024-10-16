package uk.gov.hmcts.reform.wataskmanagementapi.schedulers;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.services.MIReportingService;

/**
 * This scheduler checks if logical replication is in place.
 */
@Component
@Profile("replica | preview")
public class LogicalReplicationCreatorScheduler {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(LogicalReplicationCreatorScheduler.class);
    private static final int FIXED_DELAY_CONST = 30_000;

    @Autowired
    private MIReportingService miReportingService;

    @Scheduled(fixedDelay = FIXED_DELAY_CONST)
    public void scheduled() {
        LOGGER.debug("Postgresql logical replication scheduler executed");
        miReportingService.logicalReplicationCheck();
    }
}
