package uk.gov.hmcts.reform.wataskmanagementapi.cft.entities;

import com.vladmihalcea.hibernate.type.array.StringArrayType;
import lombok.Getter;
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
@Getter
@Entity(name = "task_roles")
@TypeDef(
    name = "string-array",
    typeClass = StringArrayType.class
)
@SuppressWarnings({"PMD.ExcessiveParameterList", "PMD.TooManyFields"})
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
    private TaskResource taskResource;

    protected TaskRoleResource() {
        // required for runtime proxy generation in Hibernate
    }

    @SuppressWarnings("squid:S00107")
    public TaskRoleResource(String roleName, Boolean read, Boolean own, Boolean execute, Boolean manage, Boolean cancel,
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

    @Override
    public boolean equals(Object anotherObject) {
        if (this == anotherObject) {
            return true;
        }
        if (anotherObject == null || getClass() != anotherObject.getClass()) {
            return false;
        }
        TaskRoleResource taskRoles = (TaskRoleResource) anotherObject;

        return Objects.equals(roleName, taskRoles.roleName)
               && Objects.equals(taskId, taskRoles.taskId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(roleName, taskId);
    }
}
