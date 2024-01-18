package uk.gov.hmcts.reform.wataskmanagementapi.cft.replicarepository;

import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskAssignmentsResource;

import java.util.List;

@Profile("replica | preview")
public interface TaskAssignmentsRepository
    extends CrudRepository<TaskAssignmentsResource, String>, JpaSpecificationExecutor<TaskAssignmentsResource> {

    List<TaskAssignmentsResource> findAllByTaskIdOrderByAssignmentIdAsc(String taskId);

}
