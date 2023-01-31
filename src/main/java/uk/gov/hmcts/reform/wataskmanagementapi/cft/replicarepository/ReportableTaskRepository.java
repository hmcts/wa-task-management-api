package uk.gov.hmcts.reform.wataskmanagementapi.cft.replicarepository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.ReportableTaskResource;

import java.util.List;

public interface ReportableTaskRepository
    extends CrudRepository<ReportableTaskResource, String>, JpaSpecificationExecutor<ReportableTaskResource> {

    List<ReportableTaskResource> findAllByTaskIdOrderByUpdatedAsc(String taskId);

}
