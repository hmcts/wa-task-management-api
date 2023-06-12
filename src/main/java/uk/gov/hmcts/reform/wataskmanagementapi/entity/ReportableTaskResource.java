package uk.gov.hmcts.reform.wataskmanagementapi.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.ToString;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.Table;

@ToString
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressWarnings({"PMD.TooManyFields", "PMD.LinguisticNaming", "PMD.UnusedPrivateField"})
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
    private Duration waitTime;
    private Duration handlingTime;
    private Duration processingTime;
    private Duration dueDateToCompletedDiffTime;

    @Override
    public String getTaskTitle() {
        return getTitle();
    }
}
