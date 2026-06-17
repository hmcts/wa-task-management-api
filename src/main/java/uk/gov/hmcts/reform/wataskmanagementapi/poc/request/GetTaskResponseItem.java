package uk.gov.hmcts.reform.wataskmanagementapi.poc.request;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.ExecutionType;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.SecurityClassification;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.TaskSystem;

import java.time.OffsetDateTime;
import java.util.Map;

@Getter
@Setter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class GetTaskResponseItem {

    private String id;
    private String name;
    private String assignee;
    private String type;
    private String taskState;
    private TaskSystem taskSystem;
    private SecurityClassification securityClassification;
    private String taskTitle;
    private OffsetDateTime createdDate;
    private OffsetDateTime dueDate;
    private String locationName;
    private String location;
    private ExecutionType executionType;
    private String jurisdiction;
    private String region;
    private String caseTypeId;
    private String caseId;
    private String caseCategory;
    private String caseName;
    private Boolean autoAssigned;
    private Boolean warnings;
    private String caseManagementCategory;
    private String workTypeId;
    private String workTypeLabel;
    private String description;
    private String roleCategory;
    private Map<String, String> additionalProperties;
    private String nextHearingId;
    private OffsetDateTime nextHearingDate;
    private Integer minorPriority;
    private Integer majorPriority;
    private OffsetDateTime priorityDate;
    private OffsetDateTime reconfigureRequestTime;
    private OffsetDateTime lastReconfigurationTime;
    private String terminationProcess;
}
