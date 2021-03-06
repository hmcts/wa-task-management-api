package uk.gov.hmcts.reform.wataskmanagementapi.controllers.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.options.CompletionOptions;

@ApiModel(
    value = "CompleteTaskRequest",
    description = "Allows specifying certain completion options"
)
@EqualsAndHashCode
@ToString
public class CompleteTaskRequest {

    private final CompletionOptions completionOptions;

    @JsonCreator
    public CompleteTaskRequest(CompletionOptions completionOptions) {
        this.completionOptions = completionOptions;
    }

    public CompletionOptions getCompletionOptions() {
        return completionOptions;
    }
}
