package uk.gov.hmcts.reform.wataskmanagementapi.cft.replicarepository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskHistoryResource;

import java.util.List;

public interface TaskHistoryResourceRepository
    extends CrudRepository<TaskHistoryResource, String>, JpaSpecificationExecutor<TaskHistoryResource> {

    String CHECK_SUBSCRIPTION =
        "select count(*) from pg_subscription pgp WHERE subname='task_subscription';";
    String CREATE_SUBSCRIPTION = "CREATE SUBSCRIPTION task_subscription"
        + " CONNECTION 'host=:host port=:port dbname=:dbname user=:username password=:password'"
        + " PUBLICATION task_publication"
        + " WITH (slot_name = :slotname, create_slot = FALSE);";

    List<TaskHistoryResource> getByTaskId(String taskId);

    List<TaskHistoryResource> getByCaseId(String caseId);

    @Query(value = CHECK_SUBSCRIPTION, nativeQuery = true)
    int isSubscriptionPresent();

    @Modifying
    @Transactional(value = "replicaTransactionManager")
    @Query(value = CREATE_SUBSCRIPTION, nativeQuery = true)
    Object createSubscription(
        @Param("slotname") String slotName,
        @Param("host") String host,
        @Param("port") String port,
        @Param("dbname") String dbname,
        @Param("username") String username,
        @Param("password") String password
    );

}
