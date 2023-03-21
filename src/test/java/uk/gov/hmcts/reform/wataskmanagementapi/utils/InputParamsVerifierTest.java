package uk.gov.hmcts.reform.wataskmanagementapi.utils;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.InvalidRequestException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.fail;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.utils.InputParamsVerifier.verifyCaseId;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.utils.DeleteTaskConstants.CASEID_DELETE_EXCEPTION_MESSAGE;

class InputParamsVerifierTest {

    @Test
    public void shouldVerifyCaseRef() {
        assertDoesNotThrow(() -> verifyCaseId("1615817621013640"));
    }

    @Test
    public void shouldNotVerifyCaseRef() {
        final String caseRef = "123456";
        try {
            verifyCaseId(caseRef);
            fail("The method should have thrown InvalidRequestException due to invalid caseRef");
        } catch (final InvalidRequestException invalidRequestException) {
            assertThat(invalidRequestException.getMessage())
                    .isEqualTo("Bad Request: ".concat(CASEID_DELETE_EXCEPTION_MESSAGE).concat(caseRef));
        }
    }
}