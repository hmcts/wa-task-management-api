package uk.gov.hmcts.reform.wataskmanagementapi.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.repository.TaskResourceRepository;

import java.util.Optional;

@Slf4j
@Service
@SuppressWarnings({"PMD.TooManyMethods", "PMD.DataflowAnomalyAnalysis"})
public class CFTTaskDatabaseService {
    private final TaskResourceRepository tasksRepository;

    public CFTTaskDatabaseService(TaskResourceRepository tasksRepository) {
        this.tasksRepository = tasksRepository;
    }

    public Optional<TaskResource> findByIdAndObtainPessimisticWriteLock(String taskId) {
        return tasksRepository.findById(taskId);
    }


    public TaskResource saveTask(TaskResource task) {
        return tasksRepository.save(task);
    }
}
