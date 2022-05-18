package uk.gov.hmcts.reform.wataskmanagementapi.cft.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@AllArgsConstructor
public class TaskResourceSummary {

    private String taskId;
    private OffsetDateTime dueDateTime;
    private String caseId;
    private String caseName;
    private String caseCategory;
    private String locationName;
    private String title;
}
