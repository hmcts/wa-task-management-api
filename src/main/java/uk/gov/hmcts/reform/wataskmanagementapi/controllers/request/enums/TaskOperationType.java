package uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums;

public enum TaskOperationType {
    MARK_TO_RECONFIGURE("mark_to_reconfigure"),
    EXECUTE_RECONFIGURE("execute_reconfigure"),
    EXECUTE_RECONFIGURE_FAILURES("execute_reconfigure_failures"),
    UPDATE_SEARCH_INDEX("update_search_index"),
    CLEANUP_SENSITIVE_LOG_ENTRIES("cleanup_sensitive_log_entries"),
    PERFORM_REPLICATION_CHECK("perform_replication_check");

    private final String value;

    TaskOperationType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

}
