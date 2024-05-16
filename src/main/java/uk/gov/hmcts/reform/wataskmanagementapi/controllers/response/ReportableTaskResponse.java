package uk.gov.hmcts.reform.wataskmanagementapi.controllers.response;

import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.ReportableTaskResource;

import java.util.List;

@Builder(toBuilder = true)
@Data
public class ReportableTaskResponse {
    private List<ReportableTaskResource> reportableTaskList;

}
