package uk.gov.hmcts.reform.wataskmanagementapi.services;

public class CcdIdGenerator {

    long time = System.currentTimeMillis();

    public String generate() {
        time++;
        String ccdId = String.valueOf(time);

        return "test-" + ccdId;
    }
}
