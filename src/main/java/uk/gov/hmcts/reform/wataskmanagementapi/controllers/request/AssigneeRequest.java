package uk.gov.hmcts.reform.wataskmanagementapi.controllers.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode
@JsonInclude(JsonInclude.Include.NON_NULL)
@ToString
public final class AssigneeRequest {
    private final String userId;

    @JsonCreator
    public AssigneeRequest(@JsonProperty("userId") String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }

}
