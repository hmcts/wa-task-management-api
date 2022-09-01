package uk.gov.hmcts.reform.wataskmanagementapi.config.features;

public enum FeatureFlag {

    GRANULAR_PERMISSION_FEATURE("wa-task-management-granular-permission-feature"),
    //Features
    PRIVILEGED_ACCESS_FEATURE("wa-task-management-privileged-access-feature"),

    //Release 2 Features
    RELEASE_2_TASK_QUERY("wa-r2-endpoints-task-query"),

    //Release 2 endpoints
    RELEASE_2_ENDPOINTS_FEATURE("wa-r2-endpoints"),

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
