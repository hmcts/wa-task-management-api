package uk.gov.hmcts.reform.wataskmanagementapi.controllers.request;

// TEMPORARY SONAR FIX

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariable;

import java.time.ZonedDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CamundaTaskInitiationRequestTest {

    @Test
    void should_expose_constructor_values() {
        ZonedDateTime created = ZonedDateTime.parse("2026-05-06T10:15:30Z");
        ZonedDateTime due = ZonedDateTime.parse("2026-05-07T10:15:30Z");
        Map<String, CamundaVariable> variables = Map.of("key", new CamundaVariable("value", "String"));

        CamundaTaskInitiationRequest request = new CamundaTaskInitiationRequest(
            "name",
            "assignee",
            created,
            due,
            "description",
            "process-1",
            variables
        );

        assertEquals("name", request.getName());
        assertEquals("assignee", request.getAssignee());
        assertEquals(created, request.getCreated());
        assertEquals(due, request.getDue());
        assertEquals("description", request.getDescription());
        assertEquals("process-1", request.getProcessInstanceId());
        assertEquals(variables, request.getVariables());
    }

    @Test
    void should_support_no_args_constructor() {
        CamundaTaskInitiationRequest request = new CamundaTaskInitiationRequest();

        assertNull(request.getName());
        assertNull(request.getAssignee());
        assertNull(request.getCreated());
        assertNull(request.getDue());
        assertNull(request.getDescription());
        assertNull(request.getProcessInstanceId());
        assertNull(request.getVariables());
    }
}
