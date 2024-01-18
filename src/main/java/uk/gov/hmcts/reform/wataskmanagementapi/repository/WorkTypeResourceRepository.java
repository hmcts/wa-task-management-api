package uk.gov.hmcts.reform.wataskmanagementapi.repository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.WorkTypeResource;

import java.util.List;
import java.util.Optional;

public interface WorkTypeResourceRepository extends CrudRepository<WorkTypeResource, String>,
    JpaSpecificationExecutor<WorkTypeResource> {

    @Override
    Optional<WorkTypeResource> findById(String id);

    @Override
    List<WorkTypeResource> findAll();

}
