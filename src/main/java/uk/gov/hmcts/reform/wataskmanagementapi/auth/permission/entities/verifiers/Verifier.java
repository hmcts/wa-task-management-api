package uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.verifiers;

import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.VerificationData;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.VerificationResult;

public interface Verifier<T extends VerificationData> {

    default boolean canVerify(final T verificationData) {
        return true;
    };

    VerificationResult verify(final T verificationData);

}
