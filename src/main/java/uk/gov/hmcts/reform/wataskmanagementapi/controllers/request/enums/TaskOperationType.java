package uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums;

public enum TaskOperationType {
    MARK_TO_RECONFIGURE("mark_to_reconfigure"),
    EXECUTE_RECONFIGURE("execute_reconfigure"),
    UPDATE_SEARCH_INDEX("update_search_index");

    private final String value;

    TaskOperationType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

}
