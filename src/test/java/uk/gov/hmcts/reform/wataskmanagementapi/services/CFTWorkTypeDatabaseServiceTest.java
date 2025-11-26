package uk.gov.hmcts.reform.wataskmanagementapi.services;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.task.WorkType;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.WorkTypeResource;
import uk.gov.hmcts.reform.wataskmanagementapi.repository.WorkTypeResourceRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CFTWorkTypeDatabaseServiceTest {

    @Mock
    WorkTypeResourceRepository workTypeResourceRepository;

    private CFTWorkTypeDatabaseService cftWorkTypeDatabaseService;
    private String workTypeId;

    @BeforeEach
    void setUp() {
        cftWorkTypeDatabaseService = new CFTWorkTypeDatabaseService(workTypeResourceRepository);

        workTypeId = UUID.randomUUID().toString();
    }

    @Test
    void should_find_by_Id() {
        WorkTypeResource workTypeResource = new WorkTypeResource("upper_tribunal", "Upper Tribunal");
        WorkType expectedWorkType = new WorkType("upper_tribunal", "Upper Tribunal");

        when(workTypeResourceRepository.findById(workTypeId)).thenReturn(Optional.of(workTypeResource));

        final Optional<WorkType> actualWorkTypeResource = cftWorkTypeDatabaseService.findById(workTypeId);

        assertNotNull(actualWorkTypeResource);
        assertEquals(expectedWorkType.getId(), actualWorkTypeResource.get().getId());
    }

    @Test
    void should_return_empty_if_no_work_type_found() {
        final String someWorkTypeId = "someWorkTypeId";
        when(workTypeResourceRepository.findById(someWorkTypeId)).thenReturn(Optional.empty());
        final Optional<WorkType> actualWorkTypeResource = cftWorkTypeDatabaseService.findById(someWorkTypeId);

        assertNotNull(actualWorkTypeResource);
    }

    @Test
    void getAllWorkTypes() {

        final List<WorkTypeResource> workTypeResources = List.of(
            new WorkTypeResource("hearing-work", "Hearing work"),
            new WorkTypeResource("upper-tribunal", "Upper Tribunal"),
            new WorkTypeResource("routine-work", "Routine work"),
            new WorkTypeResource("decision-making-work", "Decision-making work"),
            new WorkTypeResource("applications", "Applications"),
            new WorkTypeResource("priority", "Priority"),
            new WorkTypeResource("access-requests", "Access requests"),
            new WorkTypeResource("error-management", "Error management"),
            new WorkTypeResource("review-case", "Review Case"),
            new WorkTypeResource("evidence", "Evidence"),
            new WorkTypeResource("follow-up", "Follow Up"),
            new WorkTypeResource("pre-hearing", "Pre-Hearing"),
            new WorkTypeResource("post-hearing", "Post-Hearing"),
            new WorkTypeResource("intermediate-track-hearing-work", "Intermediate track hearing work"),
            new WorkTypeResource("multi-track-hearing-work", "Multi track hearing work"),
            new WorkTypeResource("intermediate-track-decision-making-work", "Intermediate track decision making work"),
            new WorkTypeResource("multi-track-decision-making-work", "Multi track decision making work"),
            new WorkTypeResource("query_work", "Query work"),
            new WorkTypeResource("welsh_translation_work", "Welsh translation work"),
            new WorkTypeResource("bail_work", "Bail work")
        );

        when(workTypeResourceRepository.findAll()).thenReturn(workTypeResources);

        final List<WorkType> actualWorkTypes = cftWorkTypeDatabaseService.getAllWorkTypes();

        List<WorkType> expectedWorkTypes = getAllExpectedWorkTypes();

        assertNotNull(actualWorkTypes);
        assertThat(actualWorkTypes).isNotEmpty();
        assertEquals(expectedWorkTypes.size(), actualWorkTypes.size());
        assertEquals("hearing-work", actualWorkTypes.get(0).getId());
        assertEquals("Hearing work", actualWorkTypes.get(0).getLabel());
    }

    @NotNull
    private List<WorkType> getAllExpectedWorkTypes() {
        return List.of(
            new WorkType("hearing-work", "Hearing work"),
            new WorkType("upper-tribunal", "Upper Tribunal"),
            new WorkType("routine-work", "Routine work"),
            new WorkType("decision-making-work", "Decision-making work"),
            new WorkType("applications", "Applications"),
            new WorkType("priority", "Priority"),
            new WorkType("access-requests", "Access requests"),
            new WorkType("error-management", "Error management"),
            new WorkType("review-case", "Review Case"),
            new WorkType("evidence", "Evidence"),
            new WorkType("follow-up", "Follow Up"),
            new WorkType("pre_hearing", "Pre-Hearing"),
            new WorkType("post_hearing", "Post-Hearing"),
            new WorkType("intermediate_track_hearing_work", "Intermediate track hearing work"),
            new WorkType("multi_track_hearing_work", "Multi track hearing work"),
            new WorkType("intermediate_track_decision_making_work", "Intermediate track decision making work"),
            new WorkType("multi_track_decision_making_work", "Multi track decision making work"),
            new WorkType("query_work", "Query work"),
            new WorkType("welsh_translation_work", "Welsh translation work"),
            new WorkType("bail_work", "Bail work")
        );
    }
}
