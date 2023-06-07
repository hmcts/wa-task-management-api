package uk.gov.hmcts.reform.wataskmanagementapi.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@JsonIgnoreProperties(ignoreUnknown = true)
@Entity(name = "reportableTask")
@Table(name = "reportable_task")
public class ReportableTaskResource extends BaseTaskHistoryResource {

    @Override
    public String getTaskTitle() {
        return getTitle();
    }
}
