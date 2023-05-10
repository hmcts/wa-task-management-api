package uk.gov.hmcts.reform.wataskmanagementapi.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.Entity;
import javax.persistence.Table;

@JsonIgnoreProperties(ignoreUnknown = true)
@Entity(name = "reportableTask")
@Table(name = "reportable_task")
public class ReportableTaskResource extends BaseTaskHistoryResource {

    @Override
    protected void method() {

    }
}
