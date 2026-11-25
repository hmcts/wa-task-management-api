package uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.options;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(
    name = "CompletionOptions",
    description = "Completion mode options"
)
@EqualsAndHashCode
@ToString
public class CompletionOptions {

    @Schema(name = "assign_and_complete")
    @JsonProperty("assign_and_complete")
    @JsonAlias("assignAndComplete")
    private final boolean assignAndComplete;

    @JsonCreator
    public CompletionOptions(
        @JsonProperty("assign_and_complete") @JsonAlias("assignAndComplete") boolean assignAndComplete
    ) {
        this.assignAndComplete = assignAndComplete;
    }

    public boolean isAssignAndComplete() {
        return assignAndComplete;
    }
}
