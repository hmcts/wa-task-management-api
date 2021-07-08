package uk.gov.hmcts.reform.wataskmanagementapi.cft.entities;

import com.vladmihalcea.hibernate.type.basic.PostgreSQLEnumType;
import com.vladmihalcea.hibernate.type.json.JsonType;
import lombok.ToString;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.BusinessContext;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.TaskSystem;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.utils.Notes;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.SecurityClassification;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Objects;
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
@Entity
@TypeDef(
    name = "pgsql_enum",
    typeClass = PostgreSQLEnumType.class
)
@TypeDef(
    name = "jsonb",
    typeClass = JsonType.class
)
@SuppressWarnings({"PMD.ExcessiveParameterList", "PMD.TooManyFields"})
public class Tasks implements Serializable {

    private static final long serialVersionUID = -4550112481797873963L;

    private static final String PGSQL_ENUM = "pgsql_enum";

    @Id
    private String taskId;
    private String taskName;
    private String taskType;

    @Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime dueDateTime;

    @Column
    @Enumerated(EnumType.STRING)
    @Type(type = PGSQL_ENUM)
    private CFTTaskState state;

    @Enumerated(EnumType.STRING)
    @Type(type = PGSQL_ENUM)
    private TaskSystem taskSystem;

    @Enumerated(EnumType.STRING)
    @Type(type = PGSQL_ENUM)
    private SecurityClassification securityClassification;

    private String title;
    private String description;

    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb")
    private Notes notes;

    private Integer majorPriority;
    private Integer minorPriority;
    private String assignee;
    private Boolean autoAssigned = false;
    private String workType;
    private String roleCategory;
    private Boolean hasWarnings = false;

    @Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime assignmentExpiry;
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
    private BusinessContext businessContext;

    private String terminationReason;

    @Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime created;

    @OneToOne
    @JoinColumn(name = "execution_code", referencedColumnName = "execution_code")
    private ExecutionTypes executionTypeCode;

    @ToString.Exclude
    @OneToMany(mappedBy = "tasks", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<TaskRoles> taskRoles;

    protected Tasks() {
        // required for runtime proxy generation in Hibernate
    }

    @SuppressWarnings("squid:S00107")
    public Tasks(String taskId, String taskName, String taskType, OffsetDateTime dueDateTime, CFTTaskState state,
                 TaskSystem taskSystem, SecurityClassification securityClassification, String title,
                 String description, Notes notes, Integer majorPriority, Integer minorPriority, String assignee,
                 boolean autoAssigned, ExecutionTypes executionTypeCode, String workType, String roleCategory,
                 boolean hasWarnings, OffsetDateTime assignmentExpiry, String caseId, String caseTypeId,
                 String caseName, String jurisdiction, String region, String regionName, String location,
                 String locationName, BusinessContext businessContext, String terminationReason, OffsetDateTime created,
                 Set<TaskRoles> taskRoles) {

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
        this.taskRoles = taskRoles;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public OffsetDateTime getDueDateTime() {
        return dueDateTime;
    }

    public void setDueDateTime(OffsetDateTime dueDateTime) {
        this.dueDateTime = dueDateTime;
    }

    public CFTTaskState getState() {
        return state;
    }

    public void setState(CFTTaskState state) {
        this.state = state;
    }

    public TaskSystem getTaskSystem() {
        return taskSystem;
    }

    public void setTaskSystem(TaskSystem taskSystem) {
        this.taskSystem = taskSystem;
    }

    public SecurityClassification getSecurityClassification() {
        return securityClassification;
    }

    public void setSecurityClassification(SecurityClassification securityClassification) {
        this.securityClassification = securityClassification;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Notes getNotes() {
        return notes;
    }

    public void setNotes(Notes notes) {
        this.notes = notes;
    }

    public Integer getMajorPriority() {
        return majorPriority;
    }

    public void setMajorPriority(Integer majorPriority) {
        this.majorPriority = majorPriority;
    }

    public Integer getMinorPriority() {
        return minorPriority;
    }

    public void setMinorPriority(Integer minorPriority) {
        this.minorPriority = minorPriority;
    }

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    public Boolean getAutoAssigned() {
        return autoAssigned;
    }

    public void setAutoAssigned(Boolean autoAssigned) {
        this.autoAssigned = autoAssigned;
    }

    public String getWorkType() {
        return workType;
    }

    public void setWorkType(String workType) {
        this.workType = workType;
    }

    public String getRoleCategory() {
        return roleCategory;
    }

    public void setRoleCategory(String roleCategory) {
        this.roleCategory = roleCategory;
    }

    public Boolean getHasWarnings() {
        return hasWarnings;
    }

    public void setHasWarnings(Boolean hasWarnings) {
        this.hasWarnings = hasWarnings;
    }

    public OffsetDateTime getAssignmentExpiry() {
        return assignmentExpiry;
    }

    public void setAssignmentExpiry(OffsetDateTime assignmentExpiry) {
        this.assignmentExpiry = assignmentExpiry;
    }

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }

    public String getCaseTypeId() {
        return caseTypeId;
    }

    public void setCaseTypeId(String caseTypeId) {
        this.caseTypeId = caseTypeId;
    }

    public String getCaseName() {
        return caseName;
    }

    public void setCaseName(String caseName) {
        this.caseName = caseName;
    }

    public String getJurisdiction() {
        return jurisdiction;
    }

    public void setJurisdiction(String jurisdiction) {
        this.jurisdiction = jurisdiction;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getRegionName() {
        return regionName;
    }

    public void setRegionName(String regionName) {
        this.regionName = regionName;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public BusinessContext getBusinessContext() {
        return businessContext;
    }

    public void setBusinessContext(BusinessContext businessContext) {
        this.businessContext = businessContext;
    }

    public String getTerminationReason() {
        return terminationReason;
    }

    public void setTerminationReason(String terminationReason) {
        this.terminationReason = terminationReason;
    }

    public OffsetDateTime getCreated() {
        return created;
    }

    public void setCreated(OffsetDateTime created) {
        this.created = created;
    }

    public ExecutionTypes getExecutionTypeCode() {
        return executionTypeCode;
    }

    public void setExecutionTypeCode(ExecutionTypes executionTypeCode) {
        this.executionTypeCode = executionTypeCode;
    }

    public Set<TaskRoles> getTaskRoles() {
        return taskRoles;
    }

    public void setTaskRoles(Set<TaskRoles> taskRoles) {
        this.taskRoles = taskRoles;
    }

    @Override
    public boolean equals(Object anotherObject) {
        if (this == anotherObject) {
            return true;
        }
        if (anotherObject == null || getClass() != anotherObject.getClass()) {
            return false;
        }
        Tasks tasks = (Tasks) anotherObject;

        return Objects.equals(taskId, tasks.taskId)
               && Objects.equals(caseId, tasks.caseId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskId, caseId);
    }
}
