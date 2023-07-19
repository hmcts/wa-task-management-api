package uk.gov.hmcts.reform.wataskmanagementapi.services.utils;

import org.slf4j.Logger;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.TaskActionsController;

import static org.slf4j.LoggerFactory.getLogger;

public final class ResponseEntityBuilder {

    private static final Logger LOG = getLogger(TaskActionsController.class);

    private ResponseEntityBuilder() {
    }

    public static ResponseEntity<Void> buildErrorResponseEntityAndLogError(final int httpStatus,
                                                                           final Exception exception) {

        LOG.error("deleteTasks API call failed due to error - {}",
                exception.getMessage(),
                exception
        );

        return ResponseEntity
                .status(httpStatus)
                .cacheControl(CacheControl.noCache()).build();
    }
}