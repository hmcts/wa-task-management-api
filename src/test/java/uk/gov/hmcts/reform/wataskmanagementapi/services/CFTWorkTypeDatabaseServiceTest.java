package uk.gov.hmcts.reform.wataskmanagementapi.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.WorkTypeResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.repository.WorkTypeResourceRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.WorkType;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CFTWorkTypeDatabaseServiceTest extends CamundaHelpers {

    @Mock
    private WorkTypeResourceRepository workTypeResourceRepository;

    private CFTWorkTypeDatabaseService cftWorkTypeDatabaseService;

    @BeforeEach
    void setUp() {

        cftWorkTypeDatabaseService = new CFTWorkTypeDatabaseService(workTypeResourceRepository);

    }

    @Test
    void findById_test() {
        String workTypeId = "work_type_id";

        Optional<WorkTypeResource> optionalWorkTypeResource = Optional.of(createWorkTypeResource());

        when(workTypeResourceRepository.findById(workTypeId))
            .thenReturn(optionalWorkTypeResource);

        Optional<WorkType> actualWorkType =
            cftWorkTypeDatabaseService.findById(workTypeId);

        assertNotNull(actualWorkType);
        assertTrue(actualWorkType.isPresent());
        assertEquals("work_type_id", actualWorkType.get().getId());
        assertEquals("work type label", actualWorkType.get().getLabel());
    }

    @Test
    void getAllWorkTypes_test() {

        List<WorkTypeResource> workTypeResources = List.of(createWorkTypeResource());
        when(workTypeResourceRepository.findAll())
            .thenReturn(workTypeResources);

        List<WorkType> actualWorkTypes =
            cftWorkTypeDatabaseService.getAllWorkTypes();

        assertNotNull(actualWorkTypes);
        assertEquals(1, actualWorkTypes.size());
        assertEquals("work_type_id", actualWorkTypes.get(0).getId());
        assertEquals("work type label", actualWorkTypes.get(0).getLabel());

    }

    private WorkTypeResource createWorkTypeResource() {
        return new WorkTypeResource("work_type_id", "work type label");
    }

}
