package uk.gov.hmcts.reform.wataskmanagementapi.controllers.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@JsonInclude(JsonInclude.Include.NON_NULL)
@EqualsAndHashCode
@ToString
public class AssignTaskRequest {

    @Schema(name = "user_id")
    private String userId;

    @JsonCreator
    public AssignTaskRequest(@JsonProperty("userId") @JsonAlias("user_id") String userId) {
        this.userId = userId;
    }

    @JsonCreator
    public AssignTaskRequest() {
        //assignee user id is optional
    }

    public String getUserId() {
        return userId;
    }
}
