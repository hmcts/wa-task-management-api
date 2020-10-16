package uk.gov.hmcts.reform.wataskmanagementapi.domain.enums;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.TaskState;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AssigneeEnumTest {

    @Test
     void simpleEnumExampleOutsideClassTest() {
        String assigned = TaskState.ASSIGNED.getTaskState();
        String cancelled = TaskState.CANCELLED.getTaskState();
        String completed = TaskState.COMPLETED.getTaskState();
        final String configured = TaskState.CONFIGURED.getTaskState();
        final String unassigned = TaskState.UNASSIGNED.getTaskState();
        final String unconfigured = TaskState.UNCONFIGURED.getTaskState();
        final String referred = TaskState.REFERRED.getTaskState();

        assertEquals("assigned",assigned);
        assertEquals("cancelled",cancelled);
        assertEquals("completed",completed);
        assertEquals("configured",configured);
        assertEquals("unassigned",unassigned);
        assertEquals("unconfigured",unconfigured);
        assertEquals("referred",referred);
    }
}
