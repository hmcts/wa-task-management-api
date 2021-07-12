package uk.gov.hmcts.reform.wataskmanagementapi.cft.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SecurityClassificationTest {

    @Test
    void simpleEnumExampleOutsideClassTest() {
        final String publicEnum = SecurityClassification.PUBLIC.getValue();
        final String privateEnum = SecurityClassification.PRIVATE.getValue();
        final String restrictedEnum = SecurityClassification.RESTRICTED.getValue();


        assertEquals("PUBLIC", publicEnum);
        assertEquals("PRIVATE", privateEnum);
        assertEquals("RESTRICTED", restrictedEnum);
    }

    @Test
    void update_test_whenever_additions_to_assign_enum_are_made() {
        int assigneeEnumLength = SecurityClassification.values().length;
        assertEquals(3, assigneeEnumLength);
    }
}
