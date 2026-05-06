package uk.gov.hmcts.reform.wataskmanagementapi.services;

// TEMPORARY SONAR FIX

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.CamundaTaskInitiationRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.InitiateTaskRequestMap;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariable;

import java.time.ZonedDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.InitiateTaskOperation.INITIATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.ASSIGNEE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.CREATED;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.DESCRIPTION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.DUE_DATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.HAS_WARNINGS;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.TASK_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.TASK_NAME;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.TASK_TYPE;

class CamundaTaskInitiationRequestMapperTest {

    private final CamundaTaskInitiationRequestMapper mapper =
        new CamundaTaskInitiationRequestMapper(new ObjectMapper());

    @Test
    void should_map_request_attributes() {
        ZonedDateTime created = ZonedDateTime.parse("2026-05-06T10:15:30Z");
        ZonedDateTime due = ZonedDateTime.parse("2026-05-07T10:15:30Z");
        CamundaTaskInitiationRequest request = new CamundaTaskInitiationRequest(
            "Task name",
            "user-1",
            created,
            due,
            "description",
            "process-1",
            Map.of(
                TASK_TYPE.value(), new CamundaVariable("processApplication", "String"),
                CASE_ID.value(), new CamundaVariable("12345", "String"),
                HAS_WARNINGS.value(), new CamundaVariable(true, "Boolean")
            )
        );

        InitiateTaskRequestMap result = mapper.map("task-1", request);

        assertEquals(INITIATION, result.getOperation());
        assertEquals("processApplication", result.getTaskAttributes().get(TASK_TYPE.value()));
        assertEquals("Task name", result.getTaskAttributes().get(TASK_NAME.value()));
        assertEquals("user-1", result.getTaskAttributes().get(ASSIGNEE.value()));
        assertEquals("description", result.getTaskAttributes().get(DESCRIPTION.value()));
        assertEquals("12345", result.getTaskAttributes().get(CASE_ID.value()));
        assertEquals(true, result.getTaskAttributes().get(HAS_WARNINGS.value()));
        assertTrue(result.getTaskAttributes().containsKey(DUE_DATE.value()));
        assertTrue(result.getTaskAttributes().containsKey(CREATED.value()));
    }

    @Test
    void should_fallback_to_task_id_when_task_type_missing() {
        CamundaTaskInitiationRequest request = new CamundaTaskInitiationRequest(
            "Task name",
            null,
            ZonedDateTime.parse("2026-05-06T10:15:30Z"),
            ZonedDateTime.parse("2026-05-07T10:15:30Z"),
            null,
            "process-1",
            Map.of(TASK_ID.value(), new CamundaVariable("fallback-task-type", "String"))
        );

        InitiateTaskRequestMap result = mapper.map("task-1", request);

        assertEquals("fallback-task-type", result.getTaskAttributes().get(TASK_TYPE.value()));
    }
}
