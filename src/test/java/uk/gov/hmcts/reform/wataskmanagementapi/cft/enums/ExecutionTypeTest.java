package uk.gov.hmcts.reform.wataskmanagementapi.cft.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExecutionTypeTest {

    @Test
    void simpleEnumExampleOutsideClassTest() {
        final String manualEnum = ExecutionType.MANUAL.getExecutionType();
        final String builtInEnum = ExecutionType.BUILT_IN.getExecutionType();
        final String caseEventEnum = ExecutionType.CASE_EVENT.getExecutionType();

        assertEquals("MANUAL", manualEnum);
        assertEquals("BUILT_IN", builtInEnum);
        assertEquals("CASE_EVENT", caseEventEnum);
    }

    @Test
    void update_test_whenever_additions_to_assign_enum_are_made() {
        int assigneeEnumLength = ExecutionType.values().length;
        assertEquals(3, assigneeEnumLength);
    }
}
