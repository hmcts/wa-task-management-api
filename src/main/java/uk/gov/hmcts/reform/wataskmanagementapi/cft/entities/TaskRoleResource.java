package uk.gov.hmcts.reform.wataskmanagementapi.cft.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.vladmihalcea.hibernate.type.array.StringArrayType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
@Entity(name = "task_roles")
@TypeDef(
    name = "string-array",
    typeClass = StringArrayType.class
)
@SuppressWarnings({"PMD.ExcessiveParameterList", "PMD.TooManyFields", "PMD.UseVarargs", "PMD.AvoidDuplicateLiterals"})
public class TaskRoleResource implements Serializable {

    private static final long serialVersionUID = -4769530559311463016L;

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
        name = "UUID",
        strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(insertable = false, updatable = false, nullable = false)
    private UUID taskRoleId;

    @EqualsAndHashCode.Include()
    private String roleName;
    @Column(columnDefinition = "boolean default false")
    private Boolean read;
    @Column(columnDefinition = "boolean default false")
    private Boolean own;
    @Column(columnDefinition = "boolean default false")
    private Boolean execute;
    @Column(columnDefinition = "boolean default false")
    private Boolean manage;
    @Column(columnDefinition = "boolean default false")
    private Boolean cancel;
    @Column(columnDefinition = "boolean default false")
    private Boolean refer;

    //This string array cannot be converted to List<String> without significant compatibility work
    @ToString.Exclude
    @Type(type = "string-array")
    @Column(columnDefinition = "text[]")
    private String[] authorizations;

    private Integer assignmentPriority;
    @Column(columnDefinition = "boolean default false")
    private Boolean autoAssignable;
    private String roleCategory;

    @Column(name = "task_id", nullable = false)
    @EqualsAndHashCode.Include()
    private String taskId;

    @Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime created;

    @JsonBackReference
    @JoinColumn(name = "task_id", insertable = false, updatable = false)
    @ManyToOne(targetEntity = TaskResource.class, fetch = FetchType.LAZY)
    @ToString.Exclude
    private TaskResource taskResource;

    protected TaskRoleResource() {
        // required for runtime proxy generation in Hibernate
    }

    public TaskRoleResource(String roleName,
                            Boolean read,
                            Boolean own,
                            Boolean execute,
                            Boolean manage,
                            Boolean cancel,
                            Boolean refer,
                            String[] authorizations,
                            Integer assignmentPriority,
                            Boolean autoAssignable) {
        this(roleName,
            read,
            own,
            execute,
            manage,
            cancel,
            refer,
            authorizations,
            assignmentPriority,
            autoAssignable,
            null,
            null,
            null);
    }

    public TaskRoleResource(String roleName,
                            Boolean read,
                            Boolean own,
                            Boolean execute,
                            Boolean manage,
                            Boolean cancel,
                            Boolean refer,
                            String[] authorizations,
                            Integer assignmentPriority,
                            Boolean autoAssignable,
                            String roleCategory) {
        this(roleName,
            read,
            own,
            execute,
            manage,
            cancel,
            refer,
            authorizations,
            assignmentPriority,
            autoAssignable,
            roleCategory,
            null,
            null);
    }

    @SuppressWarnings("squid:S00107")
    public TaskRoleResource(String roleName,
                            Boolean read,
                            Boolean own,
                            Boolean execute,
                            Boolean manage,
                            Boolean cancel,
                            Boolean refer,
                            String[] authorizations,
                            Integer assignmentPriority,
                            Boolean autoAssignable,
                            String roleCategory,
                            String taskId,
                            OffsetDateTime created) {
        this.roleName = roleName;
        this.read = read;
        this.own = own;
        this.execute = execute;
        this.manage = manage;
        this.cancel = cancel;
        this.refer = refer;
        this.authorizations = authorizations == null ? new String[]{} : authorizations.clone();
        this.assignmentPriority = assignmentPriority;
        this.autoAssignable = autoAssignable;
        this.roleCategory = roleCategory;
        this.taskId = taskId;
        this.created = created;
    }

    public void setTaskRoleId(UUID taskRoleId) {
        this.taskRoleId = taskRoleId;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public void setRead(Boolean read) {
        this.read = read;
    }

    public void setOwn(Boolean own) {
        this.own = own;
    }

    public void setExecute(Boolean execute) {
        this.execute = execute;
    }

    public void setManage(Boolean manage) {
        this.manage = manage;
    }

    public void setCancel(Boolean cancel) {
        this.cancel = cancel;
    }

    public void setRefer(Boolean refer) {
        this.refer = refer;
    }

    public void setAuthorizations(String[] authorizations) {
        this.authorizations = authorizations == null ? new String[]{} : authorizations.clone();
    }

    public void setAssignmentPriority(Integer assignmentPriority) {
        this.assignmentPriority = assignmentPriority;
    }

    public void setAutoAssignable(Boolean autoAssignable) {
        this.autoAssignable = autoAssignable;
    }

    public void setRoleCategory(String roleCategory) {
        this.roleCategory = roleCategory;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public void setCreated(OffsetDateTime created) {
        this.created = created;
    }

    public void setTaskResource(TaskResource taskResource) {
        this.taskResource = taskResource;
    }
}
