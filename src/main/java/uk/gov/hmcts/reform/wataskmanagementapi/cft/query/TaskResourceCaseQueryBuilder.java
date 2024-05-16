package uk.gov.hmcts.reform.wataskmanagementapi.cft.query;

import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;

public interface TaskResourceCaseQueryBuilder {
    String getTaskId();

    CFTTaskState getState();
}