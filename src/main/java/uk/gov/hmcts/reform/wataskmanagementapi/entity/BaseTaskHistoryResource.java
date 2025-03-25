package uk.gov.hmcts.reform.wataskmanagementapi.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static java.sql.Types.TIMESTAMP;
import static uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource.JSONB;

@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@SuppressWarnings({"PMD.TooManyFields"})
@MappedSuperclass
public abstract class BaseTaskHistoryResource {
    public static final String TIMESTAMP_WITH_TIME_ZONE = "TIMESTAMP WITH TIME ZONE";

    @Id
    @EqualsAndHashCode.Include()
    protected String updateId;

    @EqualsAndHashCode.Include()
    protected String taskId;
    protected String taskName;
    protected String taskType;

    @Column(columnDefinition = TIMESTAMP_WITH_TIME_ZONE)
    protected OffsetDateTime dueDateTime;

    protected String state;
    protected String taskSystem;
    protected String securityClassification;
    protected String title;
    protected Integer majorPriority;
    protected Integer minorPriority;
    protected String assignee;
    protected Boolean autoAssigned = false;
    protected String executionTypeCode;
    protected String workType;
    protected String roleCategory;
    protected Boolean hasWarnings = false;

    @EqualsAndHashCode.Include()
    protected String caseId;
    protected String caseTypeId;
    protected String caseName;
    protected String caseCategory;
    protected String jurisdiction;
    protected String region;
    protected String location;
    protected String businessContext;
    protected String terminationReason;

    @Column(columnDefinition = TIMESTAMP_WITH_TIME_ZONE)
    protected OffsetDateTime assignmentExpiry;

    @Column(columnDefinition = TIMESTAMP_WITH_TIME_ZONE)
    protected OffsetDateTime created;
    protected String updatedBy;

    @Column(columnDefinition = TIMESTAMP_WITH_TIME_ZONE)
    @JdbcTypeCode(TIMESTAMP)
    protected OffsetDateTime updated;
    protected String updateAction;

    private String description;

    @Type(JsonType.class)
    @Column(columnDefinition = JSONB)
    private List<NoteResource> notes;

    private String regionName;
    private String locationName;

    @Type(JsonType.class)
    @Column(columnDefinition = JSONB)
    private Map<String, String> additionalProperties;

    @Column(columnDefinition = TIMESTAMP_WITH_TIME_ZONE)
    private OffsetDateTime reconfigureRequestTime;

    @Column(columnDefinition = TIMESTAMP_WITH_TIME_ZONE)
    private OffsetDateTime lastReconfigurationTime;

    private String nextHearingId;

    @Column(columnDefinition = TIMESTAMP_WITH_TIME_ZONE)
    private OffsetDateTime nextHearingDate;

    @Column(columnDefinition = TIMESTAMP_WITH_TIME_ZONE)
    private OffsetDateTime priorityDate;

    private String terminationProcess;

    //abstract class must have abstract method
    protected abstract String getTaskTitle();
}
