package uk.gov.hmcts.reform.wataskmanagementapi.controllers.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.options.CompletionOptions;

@Schema(
    name = "CompleteTaskRequest",
    description = "Allows specifying certain completion options"
)
@EqualsAndHashCode
@ToString
public class CompleteTaskRequest {

    private final CompletionOptions completionOptions;

    @JsonCreator
    public CompleteTaskRequest(@JsonProperty("completionOptions") @JsonAlias("completion_options")
                                       CompletionOptions completionOptions) {
        this.completionOptions = completionOptions;
    }

    public CompletionOptions getCompletionOptions() {
        return completionOptions;
    }
}
