package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda;

import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaProcessVariables.ProcessVariablesBuilder.processVariables;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaSendMessageRequest.SendMessageBuilder.sendCamundaMessageRequest;

class CamundaSendMessageRequestTest {


    @Test
    void should_set_properties() {

        CamundaProcessVariables testProcessVariables = processVariables()
            .withProcessVariable("ccdId", "0000000")
            .withProcessVariable("taskId", "someTaskId")
            .withProcessVariable("group", "TCW")
            .withProcessVariable("dueDate", "2020-09-27")
            .build();

        CamundaSendMessageRequest testObject = sendCamundaMessageRequest()
            .withMessageName("someMessageName")
            .withProcessVariables(testProcessVariables.getProcessVariablesMap())
            .build();

        assertEquals("someMessageName", testObject.getMessageName());
        assertEquals(testProcessVariables.getProcessVariablesMap(), testObject.getProcessVariables());
        assertEquals(4, testObject.getProcessVariables().size());
    }


    @Test
    void should_allow_unset_process_variables_properties() {

        CamundaSendMessageRequest testObject = sendCamundaMessageRequest()
            .withMessageName("someMessageName")
            .build();

        assertEquals("someMessageName", testObject.getMessageName());
        assertEquals(null, testObject.getProcessVariables());
    }

    @Test
    void should_allow_unset_message_name_properties() {

        CamundaProcessVariables testProcessVariables = processVariables()
            .withProcessVariable("ccdId", "0000000")
            .withProcessVariable("taskId", "someTaskId")
            .withProcessVariable("group", "TCW")
            .withProcessVariable("dueDate", "2020-09-27")
            .build();

        CamundaSendMessageRequest testObject = sendCamundaMessageRequest()
            .withProcessVariables(testProcessVariables.getProcessVariablesMap())
            .build();

        assertEquals(null, testObject.getMessageName());
        assertEquals(testProcessVariables.getProcessVariablesMap(), testObject.getProcessVariables());
        assertEquals(4, testObject.getProcessVariables().size());
    }
}
