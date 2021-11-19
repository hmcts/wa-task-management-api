package uk.gov.hmcts.reform.wataskmanagementapi.cft.enums;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Optional;

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

    @ParameterizedTest
    @CsvSource(
        value = {
            ",",       // null
            "''",      // empty
            "' '",     // blank
            "123",
            "null",
            "some-value"
        }
    )
    void should_return_optional_empty_when_input_is_invalid(String input) {

        assertEquals(Optional.empty(), CFTTaskState.from(input));

    }

    @ParameterizedTest
    @CsvSource(
        value = {
            "UNCONFIGURED, UNCONFIGURED",
            "unconfigured, UNCONFIGURED",
            "PENDING_AUTO_ASSIGN, PENDING_AUTO_ASSIGN",
            "pending_auto_assign, PENDING_AUTO_ASSIGN",
            "ASSIGNED, ASSIGNED",
            "assigned, ASSIGNED",
            "CONFIGURED, CONFIGURED",
            "configured, CONFIGURED",
            "UNASSIGNED, UNASSIGNED",
            "unassigned, UNASSIGNED",
            "COMPLETED, COMPLETED",
            "completed, COMPLETED",
            "CANCELLED, CANCELLED",
            "cancelled, CANCELLED",
            "TERMINATED, TERMINATED",
            "terminated, TERMINATED",
            "PENDING_RECONFIGURATION, PENDING_RECONFIGURATION",
            "pending_reconfiguration, PENDING_RECONFIGURATION",
            "pending_reCONFIGurATion, PENDING_RECONFIGURATION"
        }
    )
    void should_return_optional_empty_when_input_is_valid(String input, String expected) {

        assertEquals(Optional.of(CFTTaskState.valueOf(expected)), CFTTaskState.from(input));

    }
}
