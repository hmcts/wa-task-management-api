package uk.gov.hmcts.reform.wataskmanagementapi.exceptions;

import lombok.Getter;

import java.util.UUID;

@Getter
public class TaskSecondaryKeyConflictException extends RuntimeException {

    private static final long serialVersionUID = 4957223859157220692L;

    private final UUID externalTaskId;
    private final String caseTypeId;

    public TaskSecondaryKeyConflictException(UUID externalTaskId, String caseTypeId, Throwable cause) {
        super(buildMessage(externalTaskId, caseTypeId), cause);
        this.externalTaskId = externalTaskId;
        this.caseTypeId = caseTypeId;
    }

    private static String buildMessage(UUID externalTaskId, String caseTypeId) {
        return String.format(
            "Task already exists for external_task_id=%s, case_type_id=%s.",
            externalTaskId,
            caseTypeId
        );
    }

}
