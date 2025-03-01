package uk.gov.hmcts.reform.wataskmanagementapi.services.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;

@Slf4j
public final class ResponseEntityBuilder {

    private ResponseEntityBuilder() {
    }

    public static ResponseEntity<Void> buildErrorResponseEntityAndLogError(final int httpStatus,
                                                                           final Exception exception) {

        log.error("deleteTasks API call failed due to error - {}",
            exception.getMessage(),
            exception
        );

        return ResponseEntity
            .status(httpStatus)
            .cacheControl(CacheControl.noCache()).build();
    }
}