package uk.gov.hmcts.reform.wataskmanagementapi.cft.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TaskSystemTest {

    @Test
    void simpleEnumExampleOutsideClassTest() {
        final String selfEnum = TaskSystem.SELF.getTaskStystem();
        final String ctscEnum = TaskSystem.CTSC.getTaskStystem();

        assertEquals("SELF", selfEnum);
        assertEquals("CTSC", ctscEnum);
    }

    @Test
    void update_test_whenever_additions_to_assign_enum_are_made() {
        int assigneeEnumLength = TaskSystem.values().length;
        assertEquals(2, assigneeEnumLength);
    }
}
