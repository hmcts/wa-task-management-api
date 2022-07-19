package uk.gov.hmcts.reform.wataskmanagementapi.wataskconfigurationapi.utils;

import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CreateTaskMessage;
import uk.gov.hmcts.reform.wataskmanagementapi.wataskconfigurationapi.controllers.PostConfigureTaskTest;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import static java.time.ZonedDateTime.now;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue.stringValue;

public class CreateTaskMessageBuilder {
    private String messageName;
    private Map<String, CamundaValue<?>> processVariables;

    public CreateTaskMessageBuilder withMessageName(String messageName) {
        this.messageName = messageName;
        return this;
    }

    public CreateTaskMessageBuilder withProcessVariables(Map<String, CamundaValue<?>> processVariables) {
        this.processVariables = processVariables;
        return this;
    }

    public CreateTaskMessageBuilder withCaseId(String caseId) {
        processVariables.put("caseId", stringValue(caseId));
        processVariables.put("taskId", stringValue("wa-task-configuration-api-task"));
        return this;
    }

    public CreateTaskMessage build() {
        return new CreateTaskMessage(messageName, processVariables);
    }

    public static CreateTaskMessageBuilder createMessageForTask(String taskTypeId, String caseId) {
        HashMap<String, CamundaValue<?>> processVariables = new HashMap<>();
        processVariables.put("caseId", stringValue(caseId));
        processVariables.put("hasWarnings", new CamundaValue<>(false, "boolean"));
        processVariables.put("taskId", stringValue(taskTypeId));
        processVariables.put(
            "dueDate",
            stringValue(now().plusDays(2).format(PostConfigureTaskTest.CAMUNDA_DATA_TIME_FORMATTER))
        );
        processVariables.put("name", stringValue("task name"));
        processVariables.put(
            "delayUntil",
            stringValue(ZonedDateTime.now().minusHours(1)
                            .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
        );
        return new CreateTaskMessageBuilder()
            .withMessageName("createTaskMessage")
            .withProcessVariables(processVariables);
    }
}
