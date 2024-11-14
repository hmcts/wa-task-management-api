package uk.gov.hmcts.reform.wataskmanagementapi.entity;

import com.vladmihalcea.hibernate.type.json.JsonType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@ToString
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity(name = "sensitive_task_event_logs")
@TypeDef(
    name = TaskResource.JSONB,
    typeClass = JsonType.class
)
@SuppressWarnings("PMD.ExcessiveParameterList")
public class SensitiveTaskEventLog implements Serializable {

    private static final long serialVersionUID = -4550112481797873963L;

    private static final String PGSQL_ENUM = "pgsql_enum";
    public static final String JSONB = "jsonb";
    public static final String TIMESTAMP_WITH_TIME_ZONE = "TIMESTAMP WITH TIME ZONE";

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
        name = "UUID",
        strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(insertable = false, updatable = false, nullable = false)
    private UUID id;
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
    private Users userData;

    @Column(columnDefinition = TIMESTAMP_WITH_TIME_ZONE)
    private OffsetDateTime expiryTime;

    @Column(columnDefinition = TIMESTAMP_WITH_TIME_ZONE)
    private OffsetDateTime logEventTime;

    protected SensitiveTaskEventLog() {
        // required for runtime proxy generation in Hibernate
    }

    public SensitiveTaskEventLog(String requestId,
                                 String correlationId,
                                 String taskId,
                                 String caseId,
                                 String message,
                                 List<TaskResource> taskData,
                                 Users userData,
                                 OffsetDateTime expiryTime,
                                 OffsetDateTime logEventTime) {
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
}
