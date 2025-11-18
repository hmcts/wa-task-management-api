package uk.gov.hmcts.reform.wataskmanagementapi.utils;

import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.repository.TaskResourceRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class IntegrationTestIndexUtils {

    public void indexRecord(TaskResourceRepository taskResourceRepository) {
        List<String> ids = new ArrayList<>();
        taskResourceRepository.findAll().forEach(taskResource -> ids.add(taskResource.getTaskId()));
        ids.forEach(id -> {
            Optional<TaskResource> taskResource = taskResourceRepository.findById(id);
            TaskResource task = taskResource.get();
            task.setIndexed(true);
            taskResourceRepository.save(task);
        });
    }

}
