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
    protected OffsetDateTime updated;
    protected String updateAction;

    //abstract class must have abstract method
    protected abstract String getTaskTitle();
}
