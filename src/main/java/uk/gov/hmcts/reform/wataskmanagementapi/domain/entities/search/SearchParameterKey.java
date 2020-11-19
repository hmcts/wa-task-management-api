package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search;

import com.fasterxml.jackson.annotation.JsonValue;

public enum SearchParameterKey {

    LOCATION("location"),
    USER("user"),
    JURISDICTION("jurisdiction"),
    STATE("state"),
    TASK_TYPE("type"),
    CASE_ID("caseId");


    @JsonValue
    private final String id;

    SearchParameterKey(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return id;
    }
}
