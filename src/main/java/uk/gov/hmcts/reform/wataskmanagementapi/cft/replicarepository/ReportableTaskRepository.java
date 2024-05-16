package uk.gov.hmcts.reform.wataskmanagementapi.cft.replicarepository;

import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.ReportableTaskResource;

import java.util.List;

@Profile("replica | preview")
public interface ReportableTaskRepository
    extends CrudRepository<ReportableTaskResource, String>, JpaSpecificationExecutor<ReportableTaskResource> {


    String SHOW_WAL_LEVEL =
        "SHOW wal_level;";


    List<ReportableTaskResource> findAllByTaskIdOrderByUpdatedAsc(String taskId);

    @Query(value = SHOW_WAL_LEVEL, nativeQuery = true)
    String showWalLevel();
}
