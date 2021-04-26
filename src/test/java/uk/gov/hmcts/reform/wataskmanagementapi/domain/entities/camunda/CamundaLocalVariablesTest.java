package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda;

import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaLocalVariables.LocalVariablesBuilder.localVariables;

class CamundaLocalVariablesTest {

    @Test
    void should_set_properties() {

        CamundaLocalVariables testObject = localVariables()
            .withLocalVariable("caseId", "0000000")
            .withLocalVariable("taskId", "someTaskId")
            .withLocalVariable("group", "TCW")
            .withLocalVariable("dueDate", "2020-09-27")
            .withLocalVariable("taskState", "unassigned")
            .build();

        assertEquals(new CamundaValue<>("0000000", "String"), testObject.getLocalVariablesMap().get("caseId"));
        assertEquals(new CamundaValue<>("someTaskId", "String"), testObject.getLocalVariablesMap().get("taskId"));
        assertEquals(new CamundaValue<>("TCW", "String"), testObject.getLocalVariablesMap().get("group"));
        assertEquals(new CamundaValue<>("unassigned", "String"), testObject.getLocalVariablesMap().get("taskState"));
        assertEquals(new CamundaValue<>("2020-09-27", "String"), testObject.getLocalVariablesMap().get("dueDate"));
        assertEquals(5, testObject.getLocalVariablesMap().size());
    }

}
