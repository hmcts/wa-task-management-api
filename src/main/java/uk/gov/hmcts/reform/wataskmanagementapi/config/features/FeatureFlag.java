package uk.gov.hmcts.reform.wataskmanagementapi.config.features;

public enum FeatureFlag {

    WA_TASK_SEARCH_GIN_INDEX("wa-task-search-gin-index"),

    WA_DELETE_TASK_BY_CASE_ID("wa-delete-task-by-case-id"),

    WA_MANDATORY_FIELD_CHECK("wa-mandatory-field-check"),

    WA_COMPLETION_PROCESS_UPDATE("wa-completion-process-update"),


    //The following keys are used for testing purposes only.
    TEST_KEY("tester"),
    NON_EXISTENT_KEY("non-existent"),
    MANDATORY_FIELDS_KEY("jurisdictions");

    private final String key;

    FeatureFlag(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
