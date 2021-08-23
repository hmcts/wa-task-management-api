package uk.gov.hmcts.reform.wataskmanagementapi.cft.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExecutionTypeTest {

    @Test
    void simpleEnumExampleOutsideClassTest() {
        assertEquals("MANUAL", ExecutionType.MANUAL.getValue());
        assertEquals("Manual", ExecutionType.MANUAL.getName());
        assertEquals(
            "The task is carried out manually, and must be completed by the user in the task management UI.",
            ExecutionType.MANUAL.getDescription()
        );

        assertEquals("BUILT_IN", ExecutionType.BUILT_IN.getValue());
        assertEquals("Built In", ExecutionType.BUILT_IN.getName());
        assertEquals(
            "The application through which the task is presented to the user knows "
            + "how to launch and complete this task, based on its formKey.",
            ExecutionType.BUILT_IN.getDescription()
        );

        assertEquals("CASE_EVENT", ExecutionType.CASE_EVENT.getValue());
        assertEquals("Case Management Task", ExecutionType.CASE_EVENT.getName());
        assertEquals(
            "The task requires a case management event to be "
            + "executed by the user. (Typically this will be in CCD.)",
            ExecutionType.CASE_EVENT.getDescription()
        );
    }

    @Test
    void update_test_whenever_additions_to_assign_enum_are_made() {
        int assigneeEnumLength = ExecutionType.values().length;
        assertEquals(3, assigneeEnumLength);
    }
}
