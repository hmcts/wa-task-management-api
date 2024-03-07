package uk.gov.hmcts.reform.wataskmanagementapi.cft.replicarepository;

import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.CrudRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.replica.ReplicaTaskResource;

import java.util.List;
import java.util.Optional;

@Profile("replica | preview")
public interface ReplicaTaskResourceRepository extends CrudRepository<ReplicaTaskResource, String> {

    Optional<ReplicaTaskResource> getByTaskId(String id);

    List<ReplicaTaskResource> findAllByTaskIdIn(List<String> taskIds, Sort order);

}
