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
    "PMD.UseVarargs", "PMD.TooManyMethods"})
public interface TaskResourceRepository extends CrudRepository<TaskResource, String>,
    JpaSpecificationExecutor<TaskResource>, TaskResourceCustomRepository {

    String CHECK_REPLICATION_SLOT =
        "select count(*) from pg_replication_slots pgrs WHERE slot_name='main_slot_v1';";

    String CREATE_REPLICATION_SLOT = "SELECT * FROM pg_create_logical_replication_slot('main_slot_v1', 'pgoutput');";

    String CHECK_PUBLICATION =
        "select count(*) from pg_publication pgp WHERE pubname='task_publication';";

    String CHECK_PUBLICATION_TABLES =
        "select count(*) from pg_publication_TABLES pgp WHERE pubname='task_publication';";
    String CREATE_PUBLICATION =
        "CREATE PUBLICATION task_publication FOR TABLE cft_task_db.tasks, "
            + "cft_task_db.work_types WITH (publish = 'insert,update,delete');";

    String ADD_WORK_TYPES_TO_PUBLICATION = "ALTER PUBLICATION task_publication ADD TABLE {h-schema}work_types;";

    String GET_TASK_ID_BY_CASE_ID = "select c.task_id AS taskid, c.state AS state from {h-schema}tasks c where "
           + "c.case_id=:caseId";

    String SHOW_WAL_LEVEL = "SHOW wal_level;";

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

    List<TaskResource> findByIndexedFalseAndStateIn(List<CFTTaskState> states);

    List<TaskResource> findAllByTaskIdIn(List<String> taskIds, Sort order);

    @Query(value = GET_TASK_ID_BY_CASE_ID, nativeQuery = true)
    List<TaskResourceCaseQueryBuilder> getTaskIdsByCaseId(final @Param("caseId") String caseId);

    List<TaskResource> findByCaseIdInAndStateInAndReconfigureRequestTimeIsNull(
        List<String> caseIds, List<CFTTaskState> states);

    List<TaskResource> findByStateInAndReconfigureRequestTimeGreaterThan(
        List<CFTTaskState> states, OffsetDateTime reconfigureRequestTime);

    List<TaskResource> findByTaskIdInAndStateInAndReconfigureRequestTimeIsLessThan(
        List<String> taskIds, List<CFTTaskState> states, OffsetDateTime retry);

    List<TaskResource> findByStateInAndReconfigureRequestTimeIsLessThan(
        List<CFTTaskState> states, OffsetDateTime retry);

    List<TaskResource> findTop5ByOrderByLastUpdatedTimestampDesc();

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

    @Query(value = CHECK_REPLICATION_SLOT, nativeQuery = true)
    int countReplicationSlots();

    @Transactional
    @Query(value = CREATE_REPLICATION_SLOT, nativeQuery = true)
    Object createReplicationSlot();

    @Query(value = CHECK_PUBLICATION, nativeQuery = true)
    int countPublications();

    @Query(value = CHECK_PUBLICATION_TABLES, nativeQuery = true)
    int countPublicationTables();

    @Modifying
    @Transactional
    @Query(value = CREATE_PUBLICATION, nativeQuery = true)
    Object createPublication();


    @Modifying
    @Transactional
    @Query(value = ADD_WORK_TYPES_TO_PUBLICATION, nativeQuery = true)
    Object addWorkTypesToPublication();

    @Query(value = SHOW_WAL_LEVEL, nativeQuery = true)
    String showWalLevel();
}
