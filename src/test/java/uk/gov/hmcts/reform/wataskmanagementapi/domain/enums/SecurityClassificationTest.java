package uk.gov.hmcts.reform.wataskmanagementapi.domain.enums;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.SecurityClassification;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SecurityClassificationTest {

    @Test
    void simpleEnumExampleOutsideClassTest() {
        final String publicEnum = SecurityClassification.PUBLIC.getSecurityClassification();
        final String privateEnum = SecurityClassification.PRIVATE.getSecurityClassification();
        final String restrictedEnum = SecurityClassification.RESTRICTED.getSecurityClassification();


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
