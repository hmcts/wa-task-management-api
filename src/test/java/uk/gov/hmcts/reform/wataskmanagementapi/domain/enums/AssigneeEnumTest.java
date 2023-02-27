package uk.gov.hmcts.reform.wataskmanagementapi.domain.enums;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.TaskState;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AssigneeEnumTest {

    @Test
    void simpleEnumExampleOutsideClassTest() {
        String assigned = TaskState.ASSIGNED.value();
        String cancelled = TaskState.CANCELLED.value();
        String completed = TaskState.COMPLETED.value();
        final String configured = TaskState.CONFIGURED.value();
        final String unassigned = TaskState.UNASSIGNED.value();
        final String unconfigured = TaskState.UNCONFIGURED.value();
        final String referred = TaskState.REFERRED.value();

        assertEquals("assigned", assigned);
        assertEquals("cancelled", cancelled);
        assertEquals("completed", completed);
        assertEquals("configured", configured);
        assertEquals("unassigned", unassigned);
        assertEquals("unconfigured", unconfigured);
        assertEquals("referred", referred);
        assertEquals("referred", TaskState.REFERRED.toString());
    }

    @Test
    void update_test_whenever_additions_to_assign_enum_are_made() {
        int assigneeEnumLength = TaskState.values().length;
        assertEquals(7, assigneeEnumLength);
    }
}
