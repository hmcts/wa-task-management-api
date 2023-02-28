package uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums;

public enum TaskOperationType {
    MARK_TO_RECONFIGURE("mark_to_reconfigure", false),
    EXECUTE_RECONFIGURE("execute_reconfigure", true);

    private final String value;

    private final boolean indexed;

    TaskOperationType(String value, boolean indexed) {
        this.value = value;
        this.indexed = indexed;
    }

    public String value() {
        return value;
    }

    public boolean isIndexed() {
        return indexed;
    }
}
