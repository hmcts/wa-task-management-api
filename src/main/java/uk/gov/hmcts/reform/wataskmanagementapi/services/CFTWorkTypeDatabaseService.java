package uk.gov.hmcts.reform.wataskmanagementapi.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.WorkTypeResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.repository.WorkTypeResourceRepository;

import java.util.Optional;

@Slf4j
@Service
public class CFTWorkTypeDatabaseService {
    private final WorkTypeResourceRepository workTypeResourceRepository;

    public CFTWorkTypeDatabaseService(WorkTypeResourceRepository workTypeResourceRepository) {
        this.workTypeResourceRepository = workTypeResourceRepository;
    }

    public Optional<WorkTypeResource> findById(String id) {
        return workTypeResourceRepository.findById(id);
    }

}
