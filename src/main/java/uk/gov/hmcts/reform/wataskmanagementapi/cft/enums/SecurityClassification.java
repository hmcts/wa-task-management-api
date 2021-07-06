package uk.gov.hmcts.reform.wataskmanagementapi.cft.enums;

public enum SecurityClassification {

    PUBLIC("PUBLIC"),
    PRIVATE("PRIVATE"),
    RESTRICTED("RESTRICTED");


    private final String securityClassification;

    SecurityClassification(String securityClassification) {
        this.securityClassification = securityClassification;
    }

    public String  getSecurityClassification() {
        return this.securityClassification;
    }
}
