package uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TerminateReasonTest {

    @Test
    void simple_enum_example_outside_class_test() {
        assertEquals("CANCELLED", TerminateReason.CANCELLED.name());
        assertEquals("COMPLETED", TerminateReason.COMPLETED.name());
        assertEquals("DELETED", TerminateReason.DELETED.name());
    }

    @Test
    void update_test_whenever_additions_to_assign_enum_are_made() {
        int enumLength = TerminateReason.values().length;
        assertEquals(3, enumLength);
    }
}
