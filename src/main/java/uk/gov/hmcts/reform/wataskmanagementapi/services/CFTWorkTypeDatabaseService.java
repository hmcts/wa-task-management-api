package uk.gov.hmcts.reform.wataskmanagementapi.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.WorkTypeResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.repository.WorkTypeResourceRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.WorkType;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CFTWorkTypeDatabaseService {
    private final WorkTypeResourceRepository workTypeResourceRepository;

    public CFTWorkTypeDatabaseService(WorkTypeResourceRepository workTypeResourceRepository) {
        this.workTypeResourceRepository = workTypeResourceRepository;
    }

    /**
     * Retrieves a work type based on work type id.
     *
     * @param workTypeId          the work type id.
     * @return A mapped optional of work type {@link WorkType}
     */
    public Optional<WorkType> getWorkType(String workTypeId) {
        Optional<WorkTypeResource> workTypeResource = workTypeResourceRepository.findById(workTypeId);
        if (!workTypeResource.isPresent()) {
            return Optional.empty();
        }
        return Optional.of(new WorkType(workTypeResource.get().getId(),workTypeResource.get().getLabel()));
    }

    /**
     * Retrieves all work types.
     *
     * @return collection of work type {@link WorkType}
     */
    public List<WorkType> getAllWorkTypes() {
        final List<WorkTypeResource> workTypeResources = workTypeResourceRepository.findAll();

        return workTypeResources.stream().map(workTypeResource ->
            new WorkType(workTypeResource.getId(), workTypeResource.getLabel()))
            .collect(Collectors.toList());
    }

}
