package uk.gov.hmcts.reform.wataskmanagementapi.repository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskRoleResource;

import java.util.List;

public interface TaskRoleResourceRepository
    extends CrudRepository<TaskRoleResource, String>, JpaSpecificationExecutor<TaskRoleResource> {

    List<TaskRoleResource> findByTaskId(String taskId);

}
