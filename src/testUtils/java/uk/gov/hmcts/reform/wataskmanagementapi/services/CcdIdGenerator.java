package uk.gov.hmcts.reform.wataskmanagementapi.services;

import java.time.ZonedDateTime;

public class CcdIdGenerator {

    public String generate() {
        String ccdId = String.valueOf(ZonedDateTime.now().toInstant().toEpochMilli());
        return "test-" + ccdId;
    }
}
