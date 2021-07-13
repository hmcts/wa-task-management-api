package uk.gov.hmcts.reform.wataskmanagementapi.cft.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TaskStateTest {

    @Test
    void simpleEnumExampleOutsideClassTest() {
        final String unconfiguredEnum = TaskState.UNCONFIGURED.getValue();
        final String autoAssignEnum = TaskState.PENDING_AUTO_ASSIGN.getValue();
        final String assignedEnum = TaskState.ASSIGNED.getValue();
        final String unassignedEnum = TaskState.UNASSIGNED.getValue();
        final String completedEnum = TaskState.COMPLETED.getValue();
        final String cancelledEnum = TaskState.CANCELLED.getValue();
        final String terminatedEnum = TaskState.TERMINATED.getValue();
        final String reconfigureENum = TaskState.PENDING_RECONFIGURATION.getValue();

        assertEquals("UNCONFIGURED", unconfiguredEnum);
        assertEquals("PENDING_AUTO_ASSIGN", autoAssignEnum);
        assertEquals("ASSIGNED", assignedEnum);
        assertEquals("UNASSIGNED", unassignedEnum);
        assertEquals("COMPLETED", completedEnum);
        assertEquals("CANCELLED", cancelledEnum);
        assertEquals("TERMINATED", terminatedEnum);
        assertEquals("PENDING_RECONFIGURATION", reconfigureENum);

    }

    @Test
    void update_test_whenever_additions_to_assign_enum_are_made() {
        int assigneeEnumLength = TaskState.values().length;
        assertEquals(8, assigneeEnumLength);
    }
}
