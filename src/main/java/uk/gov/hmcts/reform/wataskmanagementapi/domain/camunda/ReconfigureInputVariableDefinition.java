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
@SuppressWarnings({"PMD.ExcessiveParameterList", "PMD.TooManyFields"})
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

    public ReconfigureInputVariableDefinition(String taskId,
                                              String taskName,
                                              String taskType,
                                              OffsetDateTime dueDateTime,
                                              CFTTaskState state,
                                              TaskSystem taskSystem,
                                              SecurityClassification securityClassification,
                                              String title,
                                              String description,
                                              List<NoteResource> notes,
                                              Integer majorPriority,
                                              Integer minorPriority,
                                              String assignee,
                                              boolean autoAssigned,
                                              ExecutionTypeResource executionTypeCode,
                                              WorkTypeResource workTypeResource,
                                              String roleCategory,
                                              boolean hasWarnings,
                                              OffsetDateTime assignmentExpiry,
                                              String caseId,
                                              String caseTypeId,
                                              String caseName,
                                              String jurisdiction,
                                              String region,
                                              String regionName,
                                              String location,
                                              String locationName,
                                              BusinessContext businessContext,
                                              String terminationReason,
                                              OffsetDateTime created,
                                              Set<TaskRoleResource> taskRoleResources,
                                              String caseCategory,
                                              Map<String, String> additionalProperties,
                                              String nextHearingId,
                                              OffsetDateTime nextHearingDate,
                                              OffsetDateTime priorityDate) {
        this.taskId = taskId;
        this.name = taskName;
        this.taskType = taskType;
        this.dueDate = dueDateTime;
        this.taskState = state;
        this.taskSystem = taskSystem;
        this.securityClassification = securityClassification;
        this.title = title;
        this.description = description;
        this.notes = notes;
        this.majorPriority = majorPriority;
        this.minorPriority = minorPriority;
        this.assignee = assignee;
        this.autoAssigned = autoAssigned;
        this.executionTypeCode = executionTypeCode;
        this.workTypeResource = workTypeResource;
        this.roleCategory = roleCategory;
        this.hasWarnings = hasWarnings;
        this.assignmentExpiry = assignmentExpiry;
        this.caseId = caseId;
        this.caseTypeId = caseTypeId;
        this.caseName = caseName;
        this.jurisdiction = jurisdiction;
        this.region = region;
        this.regionName = regionName;
        this.location = location;
        this.locationName = locationName;
        this.businessContext = businessContext;
        this.terminationReason = terminationReason;
        this.created = created;
        this.taskRoleResources = taskRoleResources;
        this.caseManagementCategory = caseCategory;
        this.additionalProperties = additionalProperties;
        this.nextHearingId = nextHearingId;
        this.nextHearingDate = nextHearingDate;
        this.priorityDate = priorityDate;
    }
}
