package uk.gov.hmcts.reform.wataskmanagementapi.cft.enums;

public enum SecurityClassification {

    PUBLIC("PUBLIC"),
    PRIVATE("PRIVATE"),
    RESTRICTED("RESTRICTED");


    private final String value;

    SecurityClassification(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }
}
