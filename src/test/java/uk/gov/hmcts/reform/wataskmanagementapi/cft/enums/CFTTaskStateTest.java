package uk.gov.hmcts.reform.wataskmanagementapi.cft.enums;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CFTTaskStateTest {

    @Test
    void simpleEnumExampleOutsideClassTest() {
        final String unconfiguredEnum = CFTTaskState.UNCONFIGURED.getValue();
        final String autoAssignEnum = CFTTaskState.PENDING_AUTO_ASSIGN.getValue();
        final String assignedEnum = CFTTaskState.ASSIGNED.getValue();
        final String unassignedEnum = CFTTaskState.UNASSIGNED.getValue();
        final String completedEnum = CFTTaskState.COMPLETED.getValue();
        final String cancelledEnum = CFTTaskState.CANCELLED.getValue();
        final String terminatedEnum = CFTTaskState.TERMINATED.getValue();
        final String reconfigureENum = CFTTaskState.PENDING_RECONFIGURATION.getValue();

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
        int assigneeEnumLength = CFTTaskState.values().length;
        assertEquals(9, assigneeEnumLength);
    }

    @Test
    void fromTest() {
        assertThatThrownBy(() -> CFTTaskState.from(null))
            .hasMessage("please provide a value");

        assertEquals(Optional.empty(), CFTTaskState.from(""));
        assertEquals(Optional.empty(), CFTTaskState.from("dummyValue"));
        assertEquals(Optional.of(CFTTaskState.UNCONFIGURED),
            CFTTaskState.from("UNCONFIGURED"));
    }
}
