package uk.gov.hmcts.reform.wataskmanagementapi.controllers.response;

import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskHistoryResource;

import java.util.List;

@Builder(toBuilder = true)
@Data
public class TaskHistoryResponse {
    private List<TaskHistoryResource> taskHistoryList;

}
