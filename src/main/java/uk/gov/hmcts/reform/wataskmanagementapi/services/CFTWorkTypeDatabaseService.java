package uk.gov.hmcts.reform.wataskmanagementapi.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.WorkTypeQuerySpecification;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.task.WorkType;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.WorkTypeResource;
import uk.gov.hmcts.reform.wataskmanagementapi.repository.WorkTypeResourceRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
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
    public Optional<WorkType> findById(String workTypeId) {
        Optional<WorkTypeResource> workTypeResource = workTypeResourceRepository.findById(workTypeId);
        if (workTypeResource.isEmpty()) {
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

    /**
     * Retrieves work type for a set of work type ids.
     * @param workTypes work type ids for a user
     * @return list of work types
     */
    public List<WorkType> getWorkTypes(Set<String> workTypes) {
        final Specification<WorkTypeResource> specification = WorkTypeQuerySpecification.findByIds(workTypes);
        return workTypeResourceRepository.findAll(specification)
            .stream().map(workTypeResource -> new WorkType(workTypeResource.getId(), workTypeResource.getLabel()))
            .collect(Collectors.toList());
    }

}
