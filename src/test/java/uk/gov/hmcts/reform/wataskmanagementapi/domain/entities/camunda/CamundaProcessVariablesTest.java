package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.WarningValues;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaProcessVariables.ProcessVariablesBuilder.processVariables;

class CamundaProcessVariablesTest {

    @Test
    void should_set_properties() {

        CamundaProcessVariables testObject = processVariables()
            .withProcessVariable("caseId", "0000000")
            .withProcessVariable("taskId", "someTaskId")
            .withProcessVariable("group", "TCW")
            .withProcessVariable("dueDate", "2020-09-27")
            .withProcessVariableBoolean("unknown", true)
            .withProcessVariable("warningList", (new WarningValues(Collections.emptyList())).toString())
            .build();

        assertEquals(new CamundaValue<>("0000000", "String"), testObject.getProcessVariablesMap().get("caseId"));
        assertEquals(new CamundaValue<>("someTaskId", "String"), testObject.getProcessVariablesMap().get("taskId"));
        assertEquals(new CamundaValue<>("TCW", "String"), testObject.getProcessVariablesMap().get("group"));
        assertEquals(new CamundaValue<>("2020-09-27", "String"), testObject.getProcessVariablesMap().get("dueDate"));
        assertEquals(new CamundaValue<>(true, "boolean"), testObject.getProcessVariablesMap().get("unknown"));

        String wv = (new WarningValues(Collections.emptyList())).toString();
        assertEquals(new CamundaValue<>(wv, "String"), testObject.getProcessVariablesMap().get("warningList"));
        assertEquals(6, testObject.getProcessVariablesMap().size());
    }

}
