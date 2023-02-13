package uk.gov.hmcts.reform.wataskmanagementapi.entity;

import com.vladmihalcea.hibernate.type.json.JsonType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@ToString
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity(name = "sensitive_task_event_logs")
@TypeDefs(
    {
        @TypeDef(
            name = TaskResource.JSONB,
            typeClass = JsonType.class
        )
    }
)
@SuppressWarnings({"PMD.ExcessiveParameterList", "PMD.TooManyFields",
    "PMD.UnnecessaryFullyQualifiedName", "PMD.ExcessiveImports"})
public class SensitiveTaskEventLog implements Serializable {

    private static final long serialVersionUID = -4550112481797873963L;

    private static final String PGSQL_ENUM = "pgsql_enum";
    public static final String JSONB = "jsonb";
    public static final String TIMESTAMP_WITH_TIME_ZONE = "TIMESTAMP WITH TIME ZONE";

    @Id
    @EqualsAndHashCode.Include()
    private String id;
    private String requestId;
    private String correlationId;
    private String taskId;
    private String caseId;
    private String message;


    @Type(type = "jsonb")
    @Column(columnDefinition = JSONB)
    private List<TaskResource> taskData;

    @Type(type = "jsonb")
    @Column(columnDefinition = JSONB)
    private List<RoleAssignment> userData;

    @Column(columnDefinition = TIMESTAMP_WITH_TIME_ZONE)
    private OffsetDateTime expiryTime;

    @Column(columnDefinition = TIMESTAMP_WITH_TIME_ZONE)
    private OffsetDateTime logEventTime;

    protected SensitiveTaskEventLog() {
        // required for runtime proxy generation in Hibernate
    }

    public SensitiveTaskEventLog(String id,
                                 String requestId,
                                 String correlationId,
                                 String taskId,
                                 String caseId,
                                 String message,
                                 List<TaskResource> taskData,
                                 List<RoleAssignment> userData,
                                 OffsetDateTime expiryTime,
                                 OffsetDateTime logEventTime) {
        this.id = id;
        this.requestId = requestId;
        this.correlationId = correlationId;
        this.taskId = taskId;
        this.caseId = caseId;
        this.message = message;
        this.taskData = taskData;
        this.userData = userData;
        this.expiryTime = expiryTime;
        this.logEventTime = logEventTime;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<TaskResource> getTaskData() {
        return taskData;
    }

    public void setTaskData(List<TaskResource> taskData) {
        this.taskData = taskData;
    }

    public List<RoleAssignment> getUserDate() {
        return userData;
    }

    public void setUserData(List<RoleAssignment> userData) {
        this.userData = userData;
    }

    public OffsetDateTime getExpiryTime() {
        return expiryTime;
    }

    public void setExpiryTime(OffsetDateTime expiryTime) {
        this.expiryTime = expiryTime;
    }

    public OffsetDateTime getLogEventTime() {
        return logEventTime;
    }

    public void setLogEventTime(OffsetDateTime logEventTime) {
        this.logEventTime = logEventTime;
    }
}
