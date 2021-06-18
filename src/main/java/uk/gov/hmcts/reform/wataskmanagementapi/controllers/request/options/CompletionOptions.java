package uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.options;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.validation.constraints.NotNull;

@ApiModel(
    value = "CompletionOptions",
    description = "Completion mode options"
)
@EqualsAndHashCode
@ToString
public class CompletionOptions {

    private final boolean assignAndComplete;

    @JsonCreator
    public CompletionOptions(boolean assignAndComplete) {
        this.assignAndComplete = assignAndComplete;
    }

    public boolean isAssignAndComplete() {
        return assignAndComplete;
    }
}
