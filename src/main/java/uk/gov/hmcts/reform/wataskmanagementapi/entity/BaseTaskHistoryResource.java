package uk.gov.hmcts.reform.wataskmanagementapi.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.vladmihalcea.hibernate.type.json.JsonType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import static uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource.JSONB;

@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@TypeDefs(
    {
        @TypeDef(
            name = TaskResource.JSONB,
            typeClass = JsonType.class
        )
    }
)
@SuppressWarnings({"PMD.TooManyFields"})
@MappedSuperclass
public abstract class BaseTaskHistoryResource {
    public static final String TIMESTAMP_WITH_TIME_ZONE = "TIMESTAMP WITH TIME ZONE";

    @Id
    @EqualsAndHashCode.Include()
    private String updateId;

    @EqualsAndHashCode.Include()
    private String taskId;
    private String taskName;
    private String taskType;

    @Column(columnDefinition = TIMESTAMP_WITH_TIME_ZONE)
    private OffsetDateTime dueDateTime;

    private String state;
    private String taskSystem;
    private String securityClassification;
    private String title;
    private Integer majorPriority;
    private Integer minorPriority;
    private String assignee;
    private Boolean autoAssigned = false;
    private String executionTypeCode;
    private String workType;
    private String roleCategory;
    private Boolean hasWarnings = false;

    @EqualsAndHashCode.Include()
    private String caseId;
    private String caseTypeId;
    private String caseName;
    private String caseCategory;
    private String jurisdiction;
    private String region;
    private String location;
    private String businessContext;
    private String terminationReason;

    @Column(columnDefinition = TIMESTAMP_WITH_TIME_ZONE)
    private OffsetDateTime assignmentExpiry;

    @Column(columnDefinition = TIMESTAMP_WITH_TIME_ZONE)
    private OffsetDateTime created;
    private String updatedBy;

    @Column(columnDefinition = TIMESTAMP_WITH_TIME_ZONE)
    private OffsetDateTime updated;
    private String updateAction;

    private String description;

    @Type(type = "jsonb")
    @Column(columnDefinition = JSONB)
    private List<NoteResource> notes;

    private String regionName;
    private String locationName;

    @Type(type = "jsonb")
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

    //abstract class must have abstract method
    protected abstract String getTaskTitle();
}
