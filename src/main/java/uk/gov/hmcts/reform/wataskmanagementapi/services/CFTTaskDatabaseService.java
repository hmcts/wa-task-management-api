package uk.gov.hmcts.reform.wataskmanagementapi.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.Tasks;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.repository.TasksRepository;

import java.util.Optional;

@Slf4j
@Service
@SuppressWarnings({"PMD.TooManyMethods", "PMD.DataflowAnomalyAnalysis"})
public class CFTTaskDatabaseService {
    private TasksRepository tasksRepository;

    public CFTTaskDatabaseService(TasksRepository tasksRepository) {
        this.tasksRepository = tasksRepository;
    }

    public Optional<Tasks> findByIdAndObtainPessimisticWriteLock(String taskId) {
        return tasksRepository.findById(taskId);
    }


    public Tasks saveTask(Tasks task) {
        return tasksRepository.save(task);
    }
}
