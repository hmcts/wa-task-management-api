package uk.gov.hmcts.reform.wataskmanagementapi.entity;

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
import javax.persistence.Transient;

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

    @Transient
    private Boolean refer = false;

    @Column(columnDefinition = "boolean default false")
    private Boolean complete;
    @Column(name = "complete_own", columnDefinition = "boolean default false")
    private Boolean completeOwn;
    @Column(name = "cancel_own", columnDefinition = "boolean default false")
    private Boolean cancelOwn;
    @Column(columnDefinition = "boolean default false")
    private Boolean claim;
    @Column(columnDefinition = "boolean default false")
    private Boolean unclaim;
    @Column(columnDefinition = "boolean default false")
    private Boolean assign;
    @Column(columnDefinition = "boolean default false")
    private Boolean unassign;
    @Column(name = "unclaim_assign", columnDefinition = "boolean default false")
    private Boolean unclaimAssign;
    @Column(name = "unassign_claim", columnDefinition = "boolean default false")
    private Boolean unassignClaim;
    @Column(name = "unassign_assign", columnDefinition = "boolean default false")
    private Boolean unassignAssign;

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
        this(
            roleName,
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
            taskId,
            created,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false
        );
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
                            OffsetDateTime created,
                            Boolean complete,
                            Boolean completeOwn,
                            Boolean cancelOwn,
                            Boolean claim,
                            Boolean unclaim,
                            Boolean assign,
                            Boolean unassign,
                            Boolean unclaimAssign,
                            Boolean unassignClaim,
                            Boolean unassignAssign) {
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
        this.complete = complete;
        this.completeOwn = completeOwn;
        this.cancelOwn = cancelOwn;
        this.claim = claim;
        this.unclaim = unclaim;
        this.assign = assign;
        this.unassign = unassign;
        this.unclaimAssign = unclaimAssign;
        this.unassignClaim = unassignClaim;
        this.unassignAssign = unassignAssign;
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

    public void setComplete(Boolean complete) {
        this.complete = complete;
    }

    public void setCompleteOwn(Boolean completeOwn) {
        this.completeOwn = completeOwn;
    }

    public void setCancelOwn(Boolean cancelOwn) {
        this.cancelOwn = cancelOwn;
    }

    public void setClaim(Boolean claim) {
        this.claim = claim;
    }

    public void setUnclaim(Boolean unclaim) {
        this.unclaim = unclaim;
    }

    public void setAssign(Boolean assign) {
        this.assign = assign;
    }

    public void setUnassign(Boolean unassign) {
        this.unassign = unassign;
    }

    public void setUnclaimAssign(Boolean unclaimAssign) {
        this.unclaimAssign = unclaimAssign;
    }

    public void setUnassignClaim(Boolean unassignClaim) {
        this.unassignClaim = unassignClaim;
    }

    public void setUnassignAssign(Boolean unassignAssign) {
        this.unassignAssign = unassignAssign;
    }
}

