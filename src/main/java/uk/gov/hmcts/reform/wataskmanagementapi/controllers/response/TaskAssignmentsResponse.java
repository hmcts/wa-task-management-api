package uk.gov.hmcts.reform.wataskmanagementapi.controllers.response;

import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskAssignmentsResource;

import java.util.List;

@Builder(toBuilder = true)
@Data
public class TaskAssignmentsResponse {
    private List<TaskAssignmentsResource> taskAssignmentsList;

}
