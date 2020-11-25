package uk.gov.hmcts.reform.wataskmanagementapi.services;

public class CaseIdGenerator {

    long time = System.currentTimeMillis();

    /*
     * Case Ids are 16 digits by using system millis we ensure that they are unique currentTimeMillis() will
     * always return a 13 digit number until Nov 20 2286 at 17:46:39.999
     */
    public String generate() {
        time++;
        String timestamp = String.valueOf(time);
        return "000" + timestamp;
    }
}
