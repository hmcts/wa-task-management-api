package uk.gov.hmcts.reform.wataskmanagementapi.controllers.utils;


import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.InvalidRequestException;
import uk.gov.hmcts.reform.wataskmanagementapi.services.utils.DeleteTaskConstants;

import static java.util.regex.Pattern.compile;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.utils.DeleteTaskConstants.CASEID_DELETE_EXCEPTION_MESSAGE;

public final class InputParamsVerifier {

    private InputParamsVerifier() {
    }

    public static void verifyCaseId(final String caseRef) {
        if (isEmpty(caseRef)
                || !compile(DeleteTaskConstants.CASE_ID_REGEX).matcher(caseRef).matches()) {
            throw new InvalidRequestException(CASEID_DELETE_EXCEPTION_MESSAGE.concat(caseRef));
        }
    }
}
