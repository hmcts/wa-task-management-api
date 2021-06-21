package uk.gov.hmcts.reform.wataskmanagementapi.config.features;

public enum FeatureFlag {

    //Features
    PRIVILEGED_ACCESS_FEATURE("wa-task-management-privileged-access-feature"),

    //The following keys are used for testing purposes only.
    TEST_KEY("tester"),
    NON_EXISTENT_KEY("non-existent");

    private final String key;

    FeatureFlag(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
