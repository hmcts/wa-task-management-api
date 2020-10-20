package uk.gov.hmcts.reform.wataskmanagementapi.services;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariable;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.Assignee;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.CaseData;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.Location;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.Task;

import java.time.ZonedDateTime;
import java.util.Map;

import static java.util.Optional.ofNullable;

@SuppressWarnings({"PMD.DataflowAnomalyAnalysis"})
@Service
public class TaskMapper {

    public Task mapToTaskObject(CamundaTask camundaTask, Map<String, CamundaVariable> variables) {

        CaseData caseData = null;
        Location location = null;
        Assignee assignee = null;
        final String taskName = camundaTask.getName();
        final ZonedDateTime dueDate = camundaTask.getDue();
        final String state = getVariableValue(variables.get("taskState"));

        if (camundaTask.getAssignee() != null) {
            assignee = new Assignee(camundaTask.getAssignee(), "username");
        }

        String staffLocation = getVariableValue(variables.get("location"));
        if (staffLocation != null) {
            location = new Location(
                getVariableValue(variables.get("location")),
                getVariableValue(variables.get("locationName"))
            );
        }

        String caseReference = getVariableValue(variables.get("ccdId"));
        if (caseReference != null) {
            caseData = new CaseData(
                caseReference,
                getVariableValue(variables.get("caseName")),
                getVariableValue(variables.get("caseType")),
                location
            );
        }

        return new Task(
            taskName,
            state,
            dueDate,
            caseData,
            assignee
        );
    }

    private String getVariableValue(CamundaVariable variable) {
        return ofNullable(variable)
            .map(CamundaVariable::getValue)
            .orElse(null);
    }
}
