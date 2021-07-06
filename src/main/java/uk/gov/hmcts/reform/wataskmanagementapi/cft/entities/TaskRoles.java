package uk.gov.hmcts.reform.wataskmanagementapi.cft.entities;

import com.vladmihalcea.hibernate.type.array.StringArrayType;
import lombok.Getter;
import lombok.ToString;
import net.logstash.logback.encoder.org.apache.commons.lang3.builder.ToStringExclude;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

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
@Entity
@TypeDefs({
    @TypeDef(
        name = "string-array",
        typeClass = StringArrayType.class
    )
})
@SuppressWarnings({"PMD.ExcessiveParameterList"})
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
    private String[] authorisations;

    private Integer assignmentPriority;
    private Boolean autoAssignable = false;
    private String roleCategory;

    @Column(name = "task_id", insertable = false, updatable = false, nullable = false)
    private String taskId;

    private OffsetDateTime created;

    @ManyToOne
    @JoinColumn(name = "task_id", nullable = false)
    @ToStringExclude
    private Tasks tasks;

    protected TaskRoles() {
        // required for runtime proxy generation in Hibernate
    }

    public TaskRoles(String roleName, Boolean read, Boolean own, Boolean execute, Boolean manage, Boolean cancel,
                     Boolean refer, String[] authorisations, Integer assignmentPriority, Boolean autoAssignable,
                     String roleCategory, String taskId, OffsetDateTime created) {
        this.roleName = roleName;
        this.read = read;
        this.own = own;
        this.execute = execute;
        this.manage = manage;
        this.cancel = cancel;
        this.refer = refer;
        this.authorisations = authorisations.clone();
        this.assignmentPriority = assignmentPriority;
        this.autoAssignable = autoAssignable;
        this.roleCategory = roleCategory;
        this.taskId = taskId;
        this.created = created;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof TaskRoles)) {
            return false;
        }

        TaskRoles taskRoles = (TaskRoles) obj;

        return taskRoleId.equals(taskRoles.taskRoleId) && taskId.equals(taskRoles.taskId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskRoleId, taskId);
    }
}
