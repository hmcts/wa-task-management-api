package uk.gov.hmcts.reform.wataskmanagementapi.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
@Entity(name = "taskHistory")
@Table(name = "task_history")
public class TaskHistoryResource extends BaseTaskHistoryResource {

    public TaskHistoryResource() {
        super();
    }

    public TaskHistoryResource(String taskId,
                        OffsetDateTime lastUpdatedTimestamp,
                        String lastUpdatedAction,
                        String lastUpdatedUser) {
        super();
        super.taskId = taskId;
        super.updated = lastUpdatedTimestamp;
        super.updateAction = lastUpdatedAction;
        super.updatedBy = lastUpdatedUser;
    }

    @Override
    public String getTaskTitle() {
        return getTitle();
    }
}
