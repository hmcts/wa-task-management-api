package uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InitiateTaskOperationTest {

    @Test
    void simple_enum_example_outside_class_test() {
        assertEquals("INITIATION", InitiateTaskOperation.INITIATION.name());
    }

    @Test
    void update_test_whenever_additions_to_assign_enum_are_made() {
        int enumLength = InitiateTaskOperation.values().length;
        assertEquals(1, enumLength);
    }
}
