package uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.actions;

public class CamundaTaskCancelException extends RuntimeException {

    private static final long serialVersionUID = 4044350921592596729L;

    public CamundaTaskCancelException(Throwable cause) {
        super(cause);
    }
}
