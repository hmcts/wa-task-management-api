package uk.gov.hmcts.reform.wataskmanagementapi.services;


import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SystemDateProviderTest {


    private final SystemDateProvider systemDateProvider = new SystemDateProvider();

    @Test
    void returns_now_datetime() {
        String actualDateTime = systemDateProvider.nowWithTime();
        assertNotNull(actualDateTime);
        assertFalse(LocalDateTime.parse(actualDateTime).isAfter(LocalDateTime.now()));
    }

}
