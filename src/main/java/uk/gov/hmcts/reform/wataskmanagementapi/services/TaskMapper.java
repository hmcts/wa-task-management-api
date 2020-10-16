package uk.gov.hmcts.reform.wataskmanagementapi.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaObjectMapper;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariable;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.Assignee;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.CaseData;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.Location;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.Task;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;

@SuppressWarnings({"PMD.DataflowAnomalyAnalysis"})
@Service
public class TaskMapper {

    private final CamundaObjectMapper camundaObjectMapper;

    @Autowired
    public TaskMapper(CamundaObjectMapper camundaObjectMapper) {
        this.camundaObjectMapper = camundaObjectMapper;
    }

    public Task mapToTaskObject(CamundaTask camundaTask, Map<String, CamundaVariable> variables) {

        CaseData caseData = null;
        Location location = null;
        Assignee assignee = null;
        final String taskName = camundaTask.getName();
        final ZonedDateTime dueDate = camundaTask.getDue();
        final String state = getVariableValue(variables.get("taskState"), String.class);

        if (camundaTask.getAssignee() != null) {
            assignee = new Assignee(camundaTask.getAssignee(), "username");
        }

        String staffLocation = getVariableValue(variables.get("location"), String.class);
        if (staffLocation != null) {
            location = new Location(
                staffLocation,
                getVariableValue(variables.get("locationName"), String.class)
            );
        }

        String caseReference = getVariableValue(variables.get("ccdId"), String.class);
        if (caseReference != null) {
            caseData = new CaseData(
                caseReference,
                getVariableValue(variables.get("jurisdiction"), String.class),
                getVariableValue(variables.get("caseName"), String.class),
                getVariableValue(variables.get("caseType"), String.class),
                location
            );
        }

        String title = getVariableValue(variables.get("title"));

        return new Task(
            taskName,
            state,
            dueDate,
            caseData,
            assignee,
            title
        );
    }

    private <T> T getVariableValue(CamundaVariable variable, Class<T> type) {
        Optional<T> value = camundaObjectMapper.read(variable, type);
        return value.orElse(null);
    }
}
