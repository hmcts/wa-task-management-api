package uk.gov.hmcts.reform.wataskmanagementapi.cft.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity(name = "reportableTask")
@NoArgsConstructor
@SuppressWarnings({"PMD.TooManyFields", "PMD.LinguisticNaming"})
public class ReportableTaskResource {
    public static final String TIMESTAMP_WITH_TIME_ZONE = "TIMESTAMP WITH TIME ZONE";

    @Id
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

    private Date createdDate;
    private String finalStateLabel;
    private Integer waitTimeDays;
    private Integer handlingTimeDays;
    private Integer processingTimeDays;
    private String isWithinSla;
    private Integer dueDateToCompletedDiffDays;
    private Date completedDate;
    private OffsetDateTime completedDateTime;
    private Date firstAssignedDate;
    private OffsetDateTime firstAssignedDateTime;
    private Integer numberOfReassignments;
    private Date dueDate;
    private Date lastUpdatedDate;
    private String waitTime;
    private String handlingTime;
    private String processingTime;
    private String dueDateToCompletedDiffTime;

    public ReportableTaskResource(String taskId,
                        String taskName,
                        String taskType,
                        String caseId,
                        String assignee,
                        Date createdDate,
                        OffsetDateTime updated,
                        String updateAction) {
        this.taskId = taskId;
        this.taskName = taskName;
        this.taskType = taskType;
        this.caseId = caseId;
        this.assignee = assignee;
        this.createdDate = createdDate;
        this.updated = updated;
        this.updateAction = updateAction;
    }

}
