package uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.options;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ApiModel(
    value = "CompletionOptions",
    description = "Completion mode options"
)
@EqualsAndHashCode
@ToString
public class CompletionOptions {

    private final boolean assignAndComplete;

    @JsonCreator
    public CompletionOptions(@JsonProperty("assignAndComplete")
                                 @JsonAlias("assign_and_complete") boolean assignAndComplete) {
        this.assignAndComplete = assignAndComplete;
    }

    public boolean isAssignAndComplete() {
        return assignAndComplete;
    }
}
