package uk.gov.hmcts.reform.wataskmanagementapi.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.time.OffsetDateTime;
import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.Table;

@ToString
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressWarnings({"PMD.TooManyFields", "PMD.LinguisticNaming", "PMD.UnusedPrivateField"})
@EqualsAndHashCode(callSuper = false)
@Entity(name = "reportableTask")
@Table(name = "reportable_task")
public class ReportableTaskResource extends BaseTaskHistoryResource {

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
    private String stateLabel;
    private String roleCategoryLabel;
    private String jurisdictionLabel;
    private String caseTypeLabel;

    @Override
    public String getTaskTitle() {
        return getTitle();
    }
}
