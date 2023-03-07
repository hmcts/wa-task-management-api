package utils;

import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.TaskOperationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface TaskOperationResponseExtractor {

    static List<TaskResource> extractTaskResource(TaskOperationResponse taskOperationResponse) {
        List<TaskResource> taskResources = new ArrayList<>();
        Map<String, Object> responseMap = taskOperationResponse.getResponseMap();
        for (Object value : responseMap.values()) {
            ((List<?>) value).forEach(v -> taskResources.add((TaskResource) v));
        }
        return taskResources;
    }

}
