package uk.gov.hmcts.reform.wataskmanagementapi.controllers.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serializable;

@Schema(
        name = "DeleteCaseTasksAction",
        description = "Delete task case id"
)
@EqualsAndHashCode
@ToString
public class DeleteCaseTasksAction implements Serializable {

    public static final long serialVersionUID = 432973322;

    private final String caseId;

    @JsonCreator
    public DeleteCaseTasksAction(final String caseId) {
        this.caseId = caseId;
    }

    public String getCaseId() {
        return caseId;
    }
}