package uk.gov.hmcts.reform.wataskmanagementapi.repository;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskResourceCaseQueryBuilder;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import javax.persistence.LockModeType;
import javax.persistence.QueryHint;

@SuppressWarnings({
    "PMD.UseVarargs"})
public interface TaskResourceRepository extends CrudRepository<TaskResource, String>,
    JpaSpecificationExecutor<TaskResource>, TaskResourceCustomRepository {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "javax.persistence.lock.timeout", value = "0")})
    @Transactional
    @NonNull
    @Override
    Optional<TaskResource> findById(@NonNull String id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "javax.persistence.lock.timeout", value = "5000")})
    @Transactional
    @NonNull
    @Query("select t from tasks t where t.taskId = :id")
    Optional<TaskResource> findByIdAndWaitForLock(@NonNull String id);

    Optional<TaskResource> getByTaskId(String id);

    List<TaskResource> getByCaseId(String caseId);

    List<TaskResource> findByIndexedFalse();

    List<TaskResource> findAllByTaskIdIn(List<String> taskIds, Sort order);

    @Query(value = "select c.task_id AS taskid, c.state AS state from {h-schema}tasks c where c.case_id=:caseId",
            nativeQuery = true)
    List<TaskResourceCaseQueryBuilder> getTaskIdsByCaseId(final @Param("caseId") String caseId);

    List<TaskResource> findByCaseIdInAndStateInAndReconfigureRequestTimeIsNull(
        List<String> caseIds, List<CFTTaskState> states);

    List<TaskResource> findByStateInAndReconfigureRequestTimeGreaterThan(
        List<CFTTaskState> states, OffsetDateTime reconfigureRequestTime);

    List<TaskResource> findByTaskIdInAndStateInAndReconfigureRequestTimeIsLessThan(
        List<String> taskIds, List<CFTTaskState> states, OffsetDateTime retry);

    @Modifying
    @QueryHints({
        @QueryHint(name = "javax.persistence.lock.timeout", value = "0"),
        @QueryHint(name = "javax.persistence.query.timeout", value = "5000"),
        @QueryHint(name = "org.hibernate.timeout", value = "5")
    })
    @Query(
        value =
            "INSERT INTO {h-schema}tasks (task_id, created, due_date_time, priority_date) "
                + "VALUES (:task_id, :created, :due_date_time, :priority_date)",
        nativeQuery = true)
    @Transactional
    void insertAndLock(
        @Param("task_id") String taskId,
        @Param("created") OffsetDateTime created,
        @Param("due_date_time") OffsetDateTime dueDate,
        @Param("priority_date") OffsetDateTime priorityDate
    );
}
