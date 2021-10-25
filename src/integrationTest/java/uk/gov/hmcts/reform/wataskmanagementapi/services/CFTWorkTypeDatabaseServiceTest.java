package uk.gov.hmcts.reform.wataskmanagementapi.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootIntegrationBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.repository.WorkTypeResourceRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.WorkType;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CFTWorkTypeDatabaseServiceTest extends SpringBootIntegrationBaseTest {

    @Autowired
    WorkTypeResourceRepository workTypeResourceRepository;

    CFTWorkTypeDatabaseService cftWorkTypeDatabaseService;

    @BeforeEach
    void setUp() {
        cftWorkTypeDatabaseService = new CFTWorkTypeDatabaseService(workTypeResourceRepository);
    }

    @Test
    void should_retrieve_all_work_types() {

        final List<WorkType> allWorkTypes = cftWorkTypeDatabaseService.getAllWorkTypes();

        assertEquals(8, allWorkTypes.size());
        assertEquals("hearing_work", allWorkTypes.get(0).getId());
        assertEquals("Hearing work", allWorkTypes.get(0).getLabel());
        assertEquals("upper_tribunal", allWorkTypes.get(1).getId());
        assertEquals("Upper Tribunal", allWorkTypes.get(1).getLabel());
        assertEquals("routine_work", allWorkTypes.get(2).getId());
        assertEquals("Routine work", allWorkTypes.get(2).getLabel());
        assertEquals("decision_making_work", allWorkTypes.get(3).getId());
        assertEquals("Decision-making work", allWorkTypes.get(3).getLabel());
        assertEquals("applications", allWorkTypes.get(4).getId());
        assertEquals("Applications", allWorkTypes.get(4).getLabel());
        assertEquals("priority", allWorkTypes.get(5).getId());
        assertEquals("Priority", allWorkTypes.get(5).getLabel());
        assertEquals("access_requests", allWorkTypes.get(6).getId());
        assertEquals("Access requests", allWorkTypes.get(6).getLabel());
        assertEquals("error_management", allWorkTypes.get(7).getId());
        assertEquals("Error management", allWorkTypes.get(7).getLabel());

    }

    @Test
    void should_retrieve_work_type_when_work_type_id_is_given() {

        final Optional<WorkType> workType = cftWorkTypeDatabaseService.findById("hearing_work");

        assertEquals("hearing_work", workType.get().getId());
        assertEquals("Hearing work", workType.get().getLabel());
    }

    @Test
    void should_return_empty_work_type_when_invalid_work_type_id_is_given() {

        final Optional<WorkType> workType = cftWorkTypeDatabaseService.findById("invalid_work");

        assertTrue(workType.isEmpty());
    }

}
