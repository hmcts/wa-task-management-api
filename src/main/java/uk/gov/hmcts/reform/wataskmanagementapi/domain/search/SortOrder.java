package uk.gov.hmcts.reform.wataskmanagementapi.domain.search;

import com.fasterxml.jackson.annotation.JsonValue;

public enum SortOrder {

    ASCENDANT("asc"),
    DESCENDANT("desc");

    @JsonValue
    private final String id;

    SortOrder(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return id;
    }
}
