package uk.gov.hmcts.reform.wataskmanagementapi.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.task.WorkType;
import uk.gov.hmcts.reform.wataskmanagementapi.repository.WorkTypeResourceRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

@SpringBootTest
@ActiveProfiles({"integration"})
@AutoConfigureMockMvc(addFilters = false)
@TestInstance(PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CFTWorkTypeDatabaseServiceTest {

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

        assertEquals(20, allWorkTypes.size());
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
        assertEquals("review_case", allWorkTypes.get(8).getId());
        assertEquals("Review Case", allWorkTypes.get(8).getLabel());
        assertEquals("evidence", allWorkTypes.get(9).getId());
        assertEquals("Evidence", allWorkTypes.get(9).getLabel());
        assertEquals("follow_up", allWorkTypes.get(10).getId());
        assertEquals("Follow Up", allWorkTypes.get(10).getLabel());
        assertEquals("pre_hearing", allWorkTypes.get(11).getId());
        assertEquals("Pre-Hearing", allWorkTypes.get(11).getLabel());
        assertEquals("post_hearing", allWorkTypes.get(12).getId());
        assertEquals("Post-Hearing", allWorkTypes.get(12).getLabel());
        assertEquals("intermediate_track_hearing_work", allWorkTypes.get(13).getId());
        assertEquals("Intermediate track hearing work", allWorkTypes.get(13).getLabel());
        assertEquals("multi_track_hearing_work", allWorkTypes.get(14).getId());
        assertEquals("Multi track hearing work", allWorkTypes.get(14).getLabel());
        assertEquals("intermediate_track_decision_making_work", allWorkTypes.get(15).getId());
        assertEquals("Intermediate track decision making work", allWorkTypes.get(15).getLabel());
        assertEquals("multi_track_decision_making_work", allWorkTypes.get(16).getId());
        assertEquals("Multi track decision making work", allWorkTypes.get(16).getLabel());
        assertEquals("query_work", allWorkTypes.get(17).getId());
        assertEquals("Query work", allWorkTypes.get(17).getLabel());
        assertEquals("welsh_translation_work", allWorkTypes.get(18).getId());
        assertEquals("Welsh translation work", allWorkTypes.get(18).getLabel());
        assertEquals("bail_work", allWorkTypes.get(19).getId());
        assertEquals("Bail work", allWorkTypes.get(19).getLabel());
    }

    @Test
    void should_retrieve_work_type_when_work_type_id_is_given() {

        final Optional<WorkType> workType = cftWorkTypeDatabaseService.findById("hearing_work");

        assertEquals("hearing_work", workType.get().getId());
        assertEquals("Hearing work", workType.get().getLabel());
    }

    @Test
    void should_retrieve_work_type_when_work_type_ids_is_given() {

        final List<WorkType> workTypes = cftWorkTypeDatabaseService.getWorkTypes(
            Set.of("hearing_work", "upper_tribunal"));
        assertEquals(2, workTypes.size());
        assertEquals("hearing_work", workTypes.get(0).getId());
        assertEquals("Hearing work", workTypes.get(0).getLabel());
        assertEquals("upper_tribunal", workTypes.get(1).getId());
        assertEquals("Upper Tribunal", workTypes.get(1).getLabel());

    }

    @Test
    void should_return_empty_work_type_when_invalid_work_type_id_is_given() {

        final Optional<WorkType> workType = cftWorkTypeDatabaseService.findById("invalid_work");

        assertTrue(workType.isEmpty());
    }

}
