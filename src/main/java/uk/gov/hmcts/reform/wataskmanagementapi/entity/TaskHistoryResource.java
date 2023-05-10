package uk.gov.hmcts.reform.wataskmanagementapi.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.Entity;
import javax.persistence.Table;

@JsonIgnoreProperties(ignoreUnknown = true)
@Entity(name = "taskHistory")
@Table(name = "task_history")
public class TaskHistoryResource extends BaseTaskHistoryResource {

    @Override
    protected void method() {

    }
}
