package uk.gov.hmcts.reform.wataskmanagementapi.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.repository.TaskResourceRepository;

import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
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

    public List<TaskResource> findByCaseIdOnly(String caseId) {
        return tasksRepository.getByCaseId(caseId);
    }

    public List<TaskResource> getActiveTasksByCaseIdsAndReconfigureRequestTimeIsNull(
        List<String> caseIds, List<CFTTaskState> states) {
        return tasksRepository.findByCaseIdInAndStateInAndReconfigureRequestTimeIsNull(caseIds, states);
    }

    public List<TaskResource> getActiveTasksAndReconfigureRequestTimeGreaterThan(
        List<CFTTaskState> states, OffsetDateTime reconfigureRequestTime) {
        return tasksRepository.findByStateInAndReconfigureRequestTimeGreaterThan(
            states, reconfigureRequestTime);
    }

    public List<TaskResource> getTasksByTaskIdAndStateInAndReconfigureRequestTimeIsLessThanRetry(
        List<String> taskIds, List<CFTTaskState> states, OffsetDateTime retryWindow) {
        return tasksRepository.findByTaskIdInAndStateInAndReconfigureRequestTimeIsLessThan(
            taskIds, states, retryWindow);
    }

    public TaskResource saveTask(TaskResource task) {
        if (task.getPriorityDate() == null) {
            task.setPriorityDate(task.getDueDateTime());
        }
        return tasksRepository.save(task);
    }

    public void insertAndLock(String taskId, OffsetDateTime dueDate) throws SQLException {
        OffsetDateTime created = OffsetDateTime.now();
        tasksRepository.insertAndLock(taskId, dueDate, created, dueDate);
    }

    public Optional<TaskResource> findTaskBySpecification(Specification<TaskResource> specification) {
        return tasksRepository.findOne(specification);
    }

    public Optional<String> findCaseId(String taskId) {
        Optional<TaskResource> taskResource = findByIdOnly(taskId);
        if (taskResource.isPresent() && taskResource.get().getCaseId() != null) {
            return Optional.of(taskResource.get().getCaseId());
        }
        return Optional.empty();
    }
}
