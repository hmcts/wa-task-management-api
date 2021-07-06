package uk.gov.hmcts.reform.wataskmanagementapi.cft.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BusinessContextTest {

    @Test
    void simpleEnumExampleOutsideClassTest() {
        final String businessContext = BusinessContext.CFT_TASK.getBusinessContext();

        assertEquals("CFT_TASK", businessContext);
    }

    @Test
    void update_test_whenever_additions_to_assign_enum_are_made() {
        int assigneeEnumLength = BusinessContext.values().length;
        assertEquals(1, assigneeEnumLength);
    }
}
