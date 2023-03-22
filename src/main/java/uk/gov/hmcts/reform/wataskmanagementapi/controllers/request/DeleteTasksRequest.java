package uk.gov.hmcts.reform.wataskmanagementapi.controllers.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serializable;

@Schema(
        name = "DeleteTasksRequest",
        description = "Allows specifying certain completion options"
)
@EqualsAndHashCode
@ToString
public class DeleteTasksRequest implements Serializable {

    public static final long serialVersionUID = 432973322;

    private final DeleteCaseTasksAction deleteCaseTasksAction;

    @JsonCreator
    public DeleteTasksRequest(@JsonProperty("deleteCaseTasksAction") @JsonAlias("delete_case_tasks_action")
                               DeleteCaseTasksAction deleteCaseTasksAction) {
        this.deleteCaseTasksAction = deleteCaseTasksAction;
    }

    public DeleteCaseTasksAction getDeleteCaseTasksAction() {
        return deleteCaseTasksAction;
    }

}
