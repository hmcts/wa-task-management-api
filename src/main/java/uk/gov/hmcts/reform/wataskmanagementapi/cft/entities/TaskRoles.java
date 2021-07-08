package uk.gov.hmcts.reform.wataskmanagementapi.cft.entities;

import com.vladmihalcea.hibernate.type.array.StringArrayType;
import lombok.ToString;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@ToString
@Entity
@TypeDef(
    name = "string-array",
    typeClass = StringArrayType.class
)
@SuppressWarnings({"PMD.ExcessiveParameterList", "PMD.TooManyFields"})
public class TaskRoles implements Serializable {

    private static final long serialVersionUID = -4769530559311463016L;

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
        name = "UUID",
        strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(insertable = false, updatable = false, nullable = false)
    private UUID taskRoleId;

    private String roleName;
    private Boolean read = false;
    private Boolean own = false;
    private Boolean execute = false;
    private Boolean manage = false;
    private Boolean cancel = false;
    private Boolean refer = false;

    @Type(type = "string-array")
    @Column(columnDefinition = "text[]")
    private String[] authorizations;

    private Integer assignmentPriority;
    private Boolean autoAssignable = false;
    private String roleCategory;

    @Column(name = "task_id", insertable = false, updatable = false, nullable = false)
    private String taskId;

    @Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime created;

    @ManyToOne
    @JoinColumn(name = "task_id", nullable = false)
    @ToString.Exclude
    private Tasks tasks;

    protected TaskRoles() {
        // required for runtime proxy generation in Hibernate
    }

    @SuppressWarnings("squid:S00107")
    public TaskRoles(String roleName, Boolean read, Boolean own, Boolean execute, Boolean manage, Boolean cancel,
                     Boolean refer, String[] authorizations, Integer assignmentPriority, Boolean autoAssignable,
                     String roleCategory, String taskId, OffsetDateTime created) {
        this.roleName = roleName;
        this.read = read;
        this.own = own;
        this.execute = execute;
        this.manage = manage;
        this.cancel = cancel;
        this.refer = refer;
        this.authorizations = authorizations.clone();
        this.assignmentPriority = assignmentPriority;
        this.autoAssignable = autoAssignable;
        this.roleCategory = roleCategory;
        this.taskId = taskId;
        this.created = created;
    }

    public UUID getTaskRoleId() {
        return taskRoleId;
    }

    public void setTaskRoleId(UUID taskRoleId) {
        this.taskRoleId = taskRoleId;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public Boolean getRead() {
        return read;
    }

    public void setRead(Boolean read) {
        this.read = read;
    }

    public Boolean getOwn() {
        return own;
    }

    public void setOwn(Boolean own) {
        this.own = own;
    }

    public Boolean getExecute() {
        return execute;
    }

    public void setExecute(Boolean execute) {
        this.execute = execute;
    }

    public Boolean getManage() {
        return manage;
    }

    public void setManage(Boolean manage) {
        this.manage = manage;
    }

    public Boolean getCancel() {
        return cancel;
    }

    public void setCancel(Boolean cancel) {
        this.cancel = cancel;
    }

    public Boolean getRefer() {
        return refer;
    }

    public void setRefer(Boolean refer) {
        this.refer = refer;
    }

    public String[] getAuthorizations() {
        return authorizations;
    }

    public void setAuthorizations(String[] authorizations) {
        this.authorizations = authorizations;
    }

    public Integer getAssignmentPriority() {
        return assignmentPriority;
    }

    public void setAssignmentPriority(Integer assignmentPriority) {
        this.assignmentPriority = assignmentPriority;
    }

    public Boolean getAutoAssignable() {
        return autoAssignable;
    }

    public void setAutoAssignable(Boolean autoAssignable) {
        this.autoAssignable = autoAssignable;
    }

    public String getRoleCategory() {
        return roleCategory;
    }

    public void setRoleCategory(String roleCategory) {
        this.roleCategory = roleCategory;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public OffsetDateTime getCreated() {
        return created;
    }

    public void setCreated(OffsetDateTime created) {
        this.created = created;
    }

    public Tasks getTasks() {
        return tasks;
    }

    public void setTasks(Tasks tasks) {
        this.tasks = tasks;
    }

    @Override
    public boolean equals(Object anotherObject) {
        if (this == anotherObject) {
            return true;
        }
        if (anotherObject == null || getClass() != anotherObject.getClass()) {
            return false;
        }
        TaskRoles taskRoles = (TaskRoles) anotherObject;

        return Objects.equals(taskRoleId, taskRoles.taskRoleId)
               && Objects.equals(taskId, taskRoles.taskId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskRoleId, taskId);
    }
}
