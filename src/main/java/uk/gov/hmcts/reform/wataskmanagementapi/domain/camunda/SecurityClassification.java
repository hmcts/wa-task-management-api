package uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda;

public enum SecurityClassification {
    PUBLIC("PUBLIC"),
    PRIVATE("PRIVATE"),
    RESTRICTED("RESTRICTED");


    private final String classificationLevel;

    SecurityClassification(String classificationLevel) {
        this.classificationLevel = classificationLevel;
    }

    public String getSecurityClassification() {
        return this.classificationLevel;
    }
}
