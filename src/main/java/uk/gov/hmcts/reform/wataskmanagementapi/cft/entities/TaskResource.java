package uk.gov.hmcts.reform.wataskmanagementapi.cft.entities;

import com.vladmihalcea.hibernate.type.basic.PostgreSQLEnumType;
import com.vladmihalcea.hibernate.type.json.JsonType;
import lombok.Getter;
import lombok.ToString;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.BusinessContext;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.TaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.TaskSystem;
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
@Getter
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
public class TaskResource implements Serializable {

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
    private TaskState state;

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
    private Set<TaskRole> taskRoles;

    protected TaskResource() {
        // required for runtime proxy generation in Hibernate
    }

    @SuppressWarnings("squid:S00107")
    public TaskResource(String taskId, String taskName, String taskType, OffsetDateTime dueDateTime, TaskState state,
                        TaskSystem taskSystem, SecurityClassification securityClassification, String title,
                        String description, Notes notes, Integer majorPriority, Integer minorPriority, String assignee,
                        boolean autoAssigned, ExecutionTypes executionTypeCode, String workType, String roleCategory,
                        boolean hasWarnings, OffsetDateTime assignmentExpiry, String caseId, String caseTypeId,
                        String caseName, String jurisdiction, String region, String regionName, String location,
                        String locationName, BusinessContext businessContext, String terminationReason,
                        OffsetDateTime created, Set<TaskRole> taskRoles) {

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

    @Override
    public boolean equals(Object anotherObject) {
        if (this == anotherObject) {
            return true;
        }
        if (anotherObject == null || getClass() != anotherObject.getClass()) {
            return false;
        }
        TaskResource tasks = (TaskResource) anotherObject;

        return Objects.equals(taskId, tasks.taskId)
               && Objects.equals(caseId, tasks.caseId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskId, caseId);
    }
}
