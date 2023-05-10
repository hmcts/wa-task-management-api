package uk.gov.hmcts.reform.wataskmanagementapi.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.time.OffsetDateTime;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

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

    protected abstract void method();
}
