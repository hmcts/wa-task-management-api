package uk.gov.hmcts.reform.wataskmanagementapi.config.features;

public enum FeatureFlag {

    WA_TASK_SEARCH_GIN_INDEX("wa-task-search-gin-index"),

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
