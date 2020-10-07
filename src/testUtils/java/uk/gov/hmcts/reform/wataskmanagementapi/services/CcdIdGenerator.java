package uk.gov.hmcts.reform.wataskmanagementapi.services;

public class CcdIdGenerator {

    public String generate() {
        String ccdId = String.valueOf(System.currentTimeMillis());
        return "test-" + ccdId;
    }
}
