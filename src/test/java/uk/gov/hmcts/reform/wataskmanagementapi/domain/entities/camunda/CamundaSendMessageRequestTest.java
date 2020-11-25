package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda;

import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaProcessVariables.ProcessVariablesBuilder.processVariables;

class CamundaSendMessageRequestTest {

    @Test
    void should_set_properties() {

        CamundaProcessVariables testProcessVariables = processVariables()
            .withProcessVariable("caseId", "0000000")
            .withProcessVariable("taskId", "someTaskId")
            .withProcessVariable("group", "TCW")
            .withProcessVariable("dueDate", "2020-09-27")
            .withProcessVariable("name", "taskName")
            .build();

        CamundaSendMessageRequest testObject = new CamundaSendMessageRequest(
            "someMessageName",
            testProcessVariables.getProcessVariablesMap()
        );

        assertEquals("someMessageName", testObject.getMessageName());
        assertEquals(testProcessVariables.getProcessVariablesMap(), testObject.getProcessVariables());
        assertEquals(5, testObject.getProcessVariables().size());
    }

    @Test
    void should_allow_unset_process_variables_properties() {

        CamundaSendMessageRequest testObject = new CamundaSendMessageRequest(
            "someMessageName",
            null
        );

        assertEquals("someMessageName", testObject.getMessageName());
        assertEquals(null, testObject.getProcessVariables());
    }

    @Test
    void should_allow_unset_message_name_properties() {

        CamundaProcessVariables testProcessVariables = processVariables()
            .withProcessVariable("caseId", "0000000")
            .withProcessVariable("taskId", "someTaskId")
            .withProcessVariable("group", "TCW")
            .withProcessVariable("dueDate", "2020-09-27")
            .withProcessVariable("name", "taskName")
            .build();

        CamundaSendMessageRequest testObject = new CamundaSendMessageRequest(
            null,
            testProcessVariables.getProcessVariablesMap()
        );

        assertEquals(null, testObject.getMessageName());
        assertEquals(testProcessVariables.getProcessVariablesMap(), testObject.getProcessVariables());
        assertEquals(5, testObject.getProcessVariables().size());
    }
}
