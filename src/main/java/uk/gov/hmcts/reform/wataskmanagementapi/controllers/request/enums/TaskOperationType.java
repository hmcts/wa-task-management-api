package uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums;

public enum TaskOperationType {
    MARK_TO_RECONFIGURE("mark_to_reconfigure", false),
    EXECUTE_RECONFIGURE("execute_reconfigure", true);

    private final String value;

    //Should this operation requires a re-indexing of tasks and task_role signature search_index
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
