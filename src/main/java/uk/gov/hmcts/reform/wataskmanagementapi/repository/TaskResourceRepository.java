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
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import javax.persistence.LockModeType;
import javax.persistence.QueryHint;

@SuppressWarnings({
    "PMD.UseVarargs"})
public interface TaskResourceRepository extends CrudRepository<TaskResource, String>,
    JpaSpecificationExecutor<TaskResource> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "javax.persistence.lock.timeout", value = "0")})
    @Transactional
    @NonNull
    @Override
    Optional<TaskResource> findById(@NonNull String id);

    Optional<TaskResource> getByTaskId(String id);

    List<TaskResource> getByCaseId(String caseId);

    List<TaskResource> findAllByTaskIdIn(List<String> taskIds, Sort order);

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

    @Query(value = "SELECT t.task_id "
                   + "FROM {h-schema}tasks t "
                   + "WHERE indexed "
                   + "AND state IN ('ASSIGNED','UNASSIGNED') "
                   + "AND {h-schema}filter_signatures(t.task_id) && :filter_signature ",
        //+ "AND {h-schema}role_signatures(t.task_id) && :role_signature",
        nativeQuery = true)
    @Transactional
    List<String> searchTasksIds(@Param("filter_signature") String[] filterSignature);
    //@Param("role_signature") String[] roleSignature

    @Query(value = "SELECT count(*) "
                   + "FROM {h-schema}tasks t "
                   + "WHERE indexed "
                   + "AND state IN ('ASSIGNED','UNASSIGNED') "
                   + "AND {h-schema}filter_signatures(t.task_id) && :filter_signature ",
        //+ "AND {h-schema}role_signatures(t.task_id) && :role_signature",
        nativeQuery = true)
    @Transactional
    Long searchTasksCount(@Param("filter_signature") String[] filterSignature);
    //@Param("role_signature") String[] roleSignature
}
