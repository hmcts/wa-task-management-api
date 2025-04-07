package uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.BusinessContext;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.TaskSystem;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.ExecutionTypeResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.NoteResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskRoleResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.WorkTypeResource;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;


@AllArgsConstructor
@Builder
@ToString
@Getter
@JsonIgnoreProperties
@SuppressWarnings({"PMD.TooManyFields"})
public class ReconfigureInputVariableDefinition {
    private String taskId;
    private String name;
    private String taskType;

    private OffsetDateTime dueDate;

    private CFTTaskState taskState;

    private TaskSystem taskSystem;

    private SecurityClassification securityClassification;

    private String title;
    private String description;

    private List<NoteResource> notes;

    private Integer majorPriority;
    private Integer minorPriority;
    private String assignee;
    private Boolean autoAssigned;

    private WorkTypeResource workTypeResource;

    private String roleCategory;
    private Boolean hasWarnings;

    private OffsetDateTime assignmentExpiry;
    private String caseId;
    private String caseTypeId;
    private String caseName;
    private String caseManagementCategory;
    private String jurisdiction;
    private String region;
    private String regionName;
    private String location;
    private String locationName;

    private BusinessContext businessContext;

    private String terminationReason;

    private OffsetDateTime created;

    private ExecutionTypeResource executionTypeCode;

    private Set<TaskRoleResource> taskRoleResources;

    private Map<String, String> additionalProperties;

    private OffsetDateTime reconfigureRequestTime;

    private OffsetDateTime lastReconfigurationTime;

    private String nextHearingId;

    private OffsetDateTime nextHearingDate;

    private OffsetDateTime priorityDate;

    private OffsetDateTime lastUpdatedTimestamp;

    private String lastUpdatedUser;

    private String lastUpdatedAction;

    private Boolean indexed;

    private String terminationProcess;
}
