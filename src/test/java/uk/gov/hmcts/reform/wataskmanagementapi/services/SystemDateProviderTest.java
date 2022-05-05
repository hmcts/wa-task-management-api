package uk.gov.hmcts.reform.wataskmanagementapi.services;


import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.SystemDateProvider.DATE_TIME_FORMAT;

class SystemDateProviderTest {

    private final SystemDateProvider systemDateProvider = new SystemDateProvider();

    @Test
    void returns_now_datetime() {
        String actualDateTime = systemDateProvider.nowWithTime();
        assertNotNull(actualDateTime);
        assertFalse(
            ZonedDateTime.parse(actualDateTime, DateTimeFormatter.ofPattern(DATE_TIME_FORMAT))
                .isAfter(ZonedDateTime.now())
        );
    }

}
