package uk.gov.hmcts.reform.wataskmanagementapi.utils;

import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.repository.TaskResourceRepository;

import java.util.List;

public class IntegrationTestIndexUtils {

    public void updateIndexedAttribute(TaskResourceRepository taskResourceRepository, List<String> taskIds) {
        Iterable<TaskResource> taskResources = taskResourceRepository.findAllById(taskIds);
        taskResources.forEach(taskResource -> taskResource.setIndexed(true));
        taskResourceRepository.saveAll(taskResources);
    }

}
