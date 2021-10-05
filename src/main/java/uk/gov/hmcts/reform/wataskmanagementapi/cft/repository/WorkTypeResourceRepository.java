package uk.gov.hmcts.reform.wataskmanagementapi.cft.repository;

import org.springframework.data.repository.CrudRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.WorkTypeResource;

import java.util.Optional;

public interface WorkTypeResourceRepository extends CrudRepository<WorkTypeResource, String> {

    @Override
    Optional<WorkTypeResource> findById(String id);
}
