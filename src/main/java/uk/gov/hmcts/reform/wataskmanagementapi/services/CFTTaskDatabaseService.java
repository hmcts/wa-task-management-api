package uk.gov.hmcts.reform.wataskmanagementapi.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.repository.TaskResourceRepository;

import java.sql.SQLException;
import java.util.Optional;

@Slf4j
@Service
public class CFTTaskDatabaseService {
    private final TaskResourceRepository tasksRepository;

    public CFTTaskDatabaseService(TaskResourceRepository tasksRepository) {
        this.tasksRepository = tasksRepository;
    }

    public Optional<TaskResource> findByIdAndObtainPessimisticWriteLock(String taskId) {
        return tasksRepository.findById(taskId);
    }

    public Optional<TaskResource> findByIdOnly(String taskId) {
        return tasksRepository.getByTaskId(taskId);
    }

    public TaskResource saveTask(TaskResource task) {
        return tasksRepository.save(task);
    }

    public void insertAndLock(String taskId) throws SQLException {
        tasksRepository.insertAndLock(taskId);
    }

    public Optional<TaskResource> findTaskBySpecification(Specification<TaskResource> specification) {
        return tasksRepository.findOne(specification);
    }
}
