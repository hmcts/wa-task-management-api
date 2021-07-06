package uk.gov.hmcts.reform.wataskmanagementapi.cft.repository;

import org.springframework.data.repository.CrudRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.Tasks;

public interface TasksRepository extends CrudRepository<Tasks, String> {

}
