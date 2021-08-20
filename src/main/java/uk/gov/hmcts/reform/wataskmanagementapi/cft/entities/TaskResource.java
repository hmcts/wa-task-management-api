package uk.gov.hmcts.reform.wataskmanagementapi.cft.entities;

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
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

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
            name = "jsonb",
            typeClass = JsonType.class
        )
    }
)
@SuppressWarnings({"PMD.ExcessiveParameterList", "PMD.TooManyFields"})
public class TaskResource implements Serializable {

    private static final long serialVersionUID = -4550112481797873963L;

    private static final String PGSQL_ENUM = "pgsql_enum";

    @Id
    @EqualsAndHashCode.Include()
    private String taskId;
    private String taskName;
    private String taskType;

    @Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
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
    @Column(columnDefinition = "jsonb")
    private NoteResource notes;

    private Integer majorPriority;
    private Integer minorPriority;
    private String assignee;
    private Boolean autoAssigned = false;
    private String workType;
    private String roleCategory;
    private Boolean hasWarnings = false;

    @Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime assignmentExpiry;
    @EqualsAndHashCode.Include()
    private String caseId;
    private String caseTypeId;
    private String caseName;
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

    @Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime created;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "executionTypeCode", referencedColumnName = "execution_code")
    private ExecutionTypeResource executionTypeCode;

    @ToString.Exclude
    @OneToMany(mappedBy = "taskResource", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<TaskRoleResource> taskRoleResources;

    protected TaskResource() {
        // required for runtime proxy generation in Hibernate
    }

    public TaskResource(String taskId,
                        String taskName,
                        String taskType) {
        this.taskId = taskId;
        this.taskName = taskName;
        this.taskType = taskType;
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
                        NoteResource notes,
                        Integer majorPriority,
                        Integer minorPriority,
                        String assignee,
                        boolean autoAssigned,
                        ExecutionTypeResource executionTypeCode,
                        String workType,
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
                        Set<TaskRoleResource> taskRoleResources) {

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
        this.workType = workType;
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

    public void setNotes(NoteResource notes) {
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

    public void setWorkType(String workType) {
        this.workType = workType;
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

}
