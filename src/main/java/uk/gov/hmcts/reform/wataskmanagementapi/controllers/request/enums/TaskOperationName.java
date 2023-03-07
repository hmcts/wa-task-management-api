package uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums;

public enum TaskOperationName {
    MARK_TO_RECONFIGURE("mark_to_reconfigure"),
    EXECUTE_RECONFIGURE("execute_reconfigure"),
    CLEANUP_SENSITIVE_LOG_ENTRIES("cleanup_sensitive_log_entries");

    private final String value;

    TaskOperationName(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
