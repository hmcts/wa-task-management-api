package uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities;

public class VerificationResult {

    private final boolean isVerified;

    public VerificationResult(boolean isVerified) {
        this.isVerified = isVerified;
    }

    public boolean isVerified() {
        return isVerified;
    }
}
