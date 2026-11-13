package uk.gov.hmcts.reform.wataskmanagementapi.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.config.TaskSearchConfiguration;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.GenericForbiddenException;
import uk.gov.hmcts.reform.wataskmanagementapi.repository.TaskResourceRepository;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class SystemTaskSearchServiceTest {

    private static final String CASE_ID = "1793986596864362";
    private static final String USERNAME = "probate-search@example.test";

    @Mock
    private TaskResourceRepository taskResourceRepository;
    @Mock
    private CFTTaskMapper taskMapper;

    private TaskSearchConfiguration taskSearchConfiguration;
    private SystemTaskSearchService systemTaskSearchService;

    @BeforeEach
    void setUp() {
        taskSearchConfiguration = new TaskSearchConfiguration();
        taskSearchConfiguration.setJurisdictionUsers(Map.of("probate", USERNAME));
        systemTaskSearchService = new SystemTaskSearchService(
            taskSearchConfiguration, taskResourceRepository, taskMapper);
    }

    @Test
    void should_reject_user_not_mapped_to_a_jurisdiction() {
        String username = "normal-user@example.test";

        GenericForbiddenException actualException = assertThrows(
            GenericForbiddenException.class,
            () -> systemTaskSearchService.searchTasks(username, CASE_ID)
        );

        assertThat(actualException).hasMessageContaining("User does not belong to any jurisdiction");

        verifyNoInteractions(taskResourceRepository, taskMapper);
    }
}
