package uk.gov.hmcts.reform.wataskmanagementapi.cft.entities;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.vladmihalcea.hibernate.type.basic.PostgreSQLEnumType;
import com.vladmihalcea.hibernate.type.json.JsonType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.BusinessContext;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.TaskSystem;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.SecurityClassification;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

@ToString
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity(name = "tasks")
@TypeDefs(
    {
        @TypeDef(
            name = "pgsql_enum",
            typeClass = PostgreSQLEnumType.class
        ),
        @TypeDef(
            name = TaskResource.JSONB,
            typeClass = JsonType.class
        )
    }
)
@SuppressWarnings({"PMD.ExcessiveParameterList", "PMD.TooManyFields",
    "PMD.UnnecessaryFullyQualifiedName", "PMD.ExcessiveImports"})
public class TaskResource implements Serializable {

    private static final long serialVersionUID = -4550112481797873963L;

    private static final String PGSQL_ENUM = "pgsql_enum";
    public static final String JSONB = "jsonb";
    public static final String TIMESTAMP_WITH_TIME_ZONE = "TIMESTAMP WITH TIME ZONE";

    @Id
    @EqualsAndHashCode.Include()
    private String taskId;
    private String taskName;
    private String taskType;

    @Column(columnDefinition = TIMESTAMP_WITH_TIME_ZONE)
    private OffsetDateTime dueDateTime;

    @Enumerated(EnumType.STRING)
    @Type(type = PGSQL_ENUM)
    @Column(columnDefinition = "task_state_enum")
    private CFTTaskState state;

    @Enumerated(EnumType.STRING)
    @Type(type = PGSQL_ENUM)
    @Column(columnDefinition = "task_system_enum")
    private TaskSystem taskSystem;

    @Enumerated(EnumType.STRING)
    @Type(type = PGSQL_ENUM)
    @Column(columnDefinition = "security_classification_enum")
    private SecurityClassification securityClassification;

    private String title;
    private String description;

    @Type(type = "jsonb")
    @Column(columnDefinition = JSONB)
    private List<NoteResource> notes;

    private Integer majorPriority;
    private Integer minorPriority;
    private String assignee;
    private Boolean autoAssigned = false;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "work_type", referencedColumnName = "work_type_id")
    private WorkTypeResource workTypeResource;

    private String roleCategory;
    private Boolean hasWarnings = false;

    @Column(columnDefinition = TIMESTAMP_WITH_TIME_ZONE)
    private OffsetDateTime assignmentExpiry;
    @EqualsAndHashCode.Include()
    private String caseId;
    private String caseTypeId;
    private String caseName;
    private String caseCategory;
    private String jurisdiction;
    private String region;
    private String regionName;
    private String location;
    private String locationName;

    @Enumerated(EnumType.STRING)
    @Type(type = PGSQL_ENUM)
    @Column(columnDefinition = "business_context_enum")
    private BusinessContext businessContext;

    private String terminationReason;

    @Column(columnDefinition = TIMESTAMP_WITH_TIME_ZONE)
    private OffsetDateTime created;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "executionTypeCode", referencedColumnName = "execution_code")
    private ExecutionTypeResource executionTypeCode;

    @JsonManagedReference
    @ToString.Exclude
    @OneToMany(mappedBy = "taskResource", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<TaskRoleResource> taskRoleResources;

    @Type(type = "jsonb")
    @Column(columnDefinition = JSONB)
    private Map<String, String> additionalProperties;

    @Column(columnDefinition = TIMESTAMP_WITH_TIME_ZONE)
    private OffsetDateTime reconfigureRequestTime;

    @Column(columnDefinition = TIMESTAMP_WITH_TIME_ZONE)
    private OffsetDateTime lastReconfigurationTime;

    private String nextHearingId;

    @Column(columnDefinition = TIMESTAMP_WITH_TIME_ZONE)
    private OffsetDateTime nextHearingDate;

    @Column(columnDefinition = TIMESTAMP_WITH_TIME_ZONE)
    private OffsetDateTime priorityDate;

    protected TaskResource() {
        // required for runtime proxy generation in Hibernate
    }

    public TaskResource(String taskId,
                        String taskName,
                        String taskType,
                        CFTTaskState state) {
        this.taskId = taskId;
        this.taskName = taskName;
        this.taskType = taskType;
        this.state = state;
    }


    public TaskResource(String taskId,
                        String taskName,
                        String taskType,
                        CFTTaskState state,
                        String caseId) {
        this.taskId = taskId;
        this.taskName = taskName;
        this.taskType = taskType;
        this.state = state;
        this.caseId = caseId;
    }

    public TaskResource(String taskId,
                        String taskName,
                        String taskType,
                        CFTTaskState state,
                        String caseId,
                        String assignee) {
        this.taskId = taskId;
        this.taskName = taskName;
        this.taskType = taskType;
        this.state = state;
        this.caseId = caseId;
        this.assignee = assignee;
    }

    public TaskResource(String taskId,
                        String taskName,
                        String taskType,
                        CFTTaskState state,
                        OffsetDateTime dueDateTime) {
        this.taskId = taskId;
        this.taskName = taskName;
        this.taskType = taskType;
        this.state = state;
        this.dueDateTime = dueDateTime;
    }

    public TaskResource(String taskId,
                        String taskName,
                        String taskType,
                        CFTTaskState state,
                        OffsetDateTime created,
                        OffsetDateTime dueDateTime,
                        OffsetDateTime priorityDate) {
        this.taskId = taskId;
        this.taskName = taskName;
        this.taskType = taskType;
        this.state = state;
        this.created = created;
        this.dueDateTime = dueDateTime;
        this.priorityDate = priorityDate;
    }

    public TaskResource(String taskId,
                        String taskName,
                        String taskType,
                        CFTTaskState state,
                        String caseId,
                        OffsetDateTime dueDateTime) {
        this.taskId = taskId;
        this.taskName = taskName;
        this.taskType = taskType;
        this.state = state;
        this.caseId = caseId;
        this.dueDateTime = dueDateTime;
    }

    public TaskResource(String taskId,
                        String taskName,
                        String taskType,
                        CFTTaskState state,
                        String caseId,
                        Set<TaskRoleResource> taskRoleResources) {
        this.taskId = taskId;
        this.taskName = taskName;
        this.taskType = taskType;
        this.state = state;
        this.caseId = caseId;
        this.taskRoleResources = taskRoleResources;
    }

    @SuppressWarnings("squid:S00107")
    public TaskResource(String taskId,
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
        this.taskName = taskName;
        this.taskType = taskType;
        this.dueDateTime = dueDateTime;
        this.state = state;
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
        this.caseCategory = caseCategory;
        this.additionalProperties = additionalProperties;
        this.nextHearingId = nextHearingId;
        this.nextHearingDate = nextHearingDate;
        this.priorityDate = priorityDate;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public void setDueDateTime(OffsetDateTime dueDateTime) {
        this.dueDateTime = dueDateTime;
    }

    public void setState(CFTTaskState state) {
        this.state = state;
    }

    public void setTaskSystem(TaskSystem taskSystem) {
        this.taskSystem = taskSystem;
    }

    public void setSecurityClassification(SecurityClassification securityClassification) {
        this.securityClassification = securityClassification;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setNotes(List<NoteResource> notes) {
        this.notes = notes;
    }

    public void setMajorPriority(Integer majorPriority) {
        this.majorPriority = majorPriority;
    }

    public void setMinorPriority(Integer minorPriority) {
        this.minorPriority = minorPriority;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    public void setAutoAssigned(Boolean autoAssigned) {
        this.autoAssigned = autoAssigned;
    }

    public void setWorkTypeResource(WorkTypeResource workTypeResource) {
        this.workTypeResource = workTypeResource;
    }

    public void setRoleCategory(String roleCategory) {
        this.roleCategory = roleCategory;
    }

    public void setHasWarnings(Boolean hasWarnings) {
        this.hasWarnings = hasWarnings;
    }

    public void setAssignmentExpiry(OffsetDateTime assignmentExpiry) {
        this.assignmentExpiry = assignmentExpiry;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }

    public void setCaseTypeId(String caseTypeId) {
        this.caseTypeId = caseTypeId;
    }

    public void setCaseName(String caseName) {
        this.caseName = caseName;
    }

    public void setJurisdiction(String jurisdiction) {
        this.jurisdiction = jurisdiction;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public void setRegionName(String regionName) {
        this.regionName = regionName;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public void setBusinessContext(BusinessContext businessContext) {
        this.businessContext = businessContext;
    }

    public void setTerminationReason(String terminationReason) {
        this.terminationReason = terminationReason;
    }

    public void setCreated(OffsetDateTime created) {
        this.created = created;
    }

    public void setExecutionTypeCode(ExecutionTypeResource executionTypeResource) {
        this.executionTypeCode = executionTypeResource;
    }

    public void setTaskRoleResources(Set<TaskRoleResource> taskRoleResources) {
        this.taskRoleResources = taskRoleResources;
    }

    public void setCaseCategory(String caseCategory) {
        this.caseCategory = caseCategory;
    }

    public void setAdditionalProperties(Map<String, String> additionalProperties) {
        this.additionalProperties = additionalProperties;
    }

    public void setReconfigureRequestTime(OffsetDateTime reconfigureRequestTime) {
        this.reconfigureRequestTime = reconfigureRequestTime;
    }

    public void setLastReconfigurationTime(OffsetDateTime lastReconfigurationTime) {
        this.lastReconfigurationTime = lastReconfigurationTime;
    }

    public void setNextHearingId(String nextHearingId) {
        this.nextHearingId = nextHearingId;
    }

    public void setNextHearingDate(OffsetDateTime nextHearingDate) {
        this.nextHearingDate = nextHearingDate;
    }

    public void setPriorityDate(OffsetDateTime priorityDate) {
        this.priorityDate = priorityDate;
    }
}
