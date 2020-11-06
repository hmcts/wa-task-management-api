package uk.gov.hmcts.reform.wataskmanagementapi.utils;

import io.restassured.http.Headers;
import uk.gov.hmcts.reform.wataskmanagementapi.config.GivensBuilder;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaProcessVariables;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariable;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CaseIdGenerator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.time.ZonedDateTime.now;
import static org.junit.jupiter.api.Assertions.fail;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaProcessVariables.ProcessVariablesBuilder.processVariables;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTime.CAMUNDA_DATA_TIME_FORMATTER;

public class Common {

    private final CaseIdGenerator caseIdGenerator;

    private final GivensBuilder given;

    public Common(CaseIdGenerator caseIdGenerator, GivensBuilder given) {
        this.caseIdGenerator = caseIdGenerator;
        this.given = given;
    }

    public Map<String, String> setupTaskAndRetrieveIdsWithCustomVariable(CamundaVariableDefinition key, String value) {
        String caseId = caseIdGenerator.generate();
        Map<String, CamundaValue<?>> processVariables = given.createDefaultTaskVariables(caseId);
        processVariables.put(key.value(), new CamundaValue<>(value, "String"));

        List<CamundaTask> response = given
            .iCreateATaskWithCustomVariables(processVariables)
            .and()
            .iRetrieveATaskWithProcessVariableFilter("ccdId", caseId);

        if (response.size() > 1) {
            fail("Search was not an exact match and returned more than one task:" + "used:" + caseId);
        }

        new HashMap<>();

        return Map.of(
            "caseId", caseId,
            "taskId", response.get(0).getId()
        );

    }

    public Map<String, String> setupTaskAndRetrieveIds() {
        String caseId = caseIdGenerator.generate();

        List<CamundaTask> response = given
            .iCreateATaskWithCaseId(caseId)
            .and()
            .iRetrieveATaskWithProcessVariableFilter("ccdId", caseId);

        if (response.size() > 1) {
            fail("Search was not an exact match and returned more than one task used:" + caseId);
        }

        new HashMap<>();

        return Map.of(
            "caseId", caseId,
            "taskId", response.get(0).getId()
        );

    }

    public Map<String, String> setupTaskWithRoleAssignmentAndRetrieveIds(Headers headers, String roleName) {
        Map<String, String> task = setupTaskAndRetrieveIds();

        given.iAllocateACaseToUserAs(headers, roleName, task.get("caseId"));

        return task;
    }

}
