package uk.gov.hmcts.reform.wataskmanagementapi.services;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAttributeDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.config.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SearchRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.repository.TaskResourceRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("integration")
@DataJpaTest(properties = "config.search.countLimit=2")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Sql("/scripts/wa/search_tasks_data.sql")
@Import(CFTTaskSearchService.class)
class CFTTaskSearchServiceIntegrationTest {

    private static final List<String> MATCHING_TASK_IDS = List.of(
        "8d6cc5cf-c973-11eb-aaaa-000000000001",
        "8d6cc5cf-c973-11eb-aaaa-000000000002",
        "8d6cc5cf-c973-11eb-aaaa-000000000003",
        "8d6cc5cf-c973-11eb-aaaa-000000000004"
    );

    @Autowired
    private CFTTaskSearchService cftTaskSearchService;

    @Autowired
    private TaskResourceRepository taskResourceRepository;

    @Autowired
    private TestEntityManager entityManager;

    @MockitoBean
    private LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider;

    @Test
    void should_apply_configured_count_limit_without_limiting_task_page() {
        indexMatchingTasks();

        CFTTaskSearchService.SearchResult result = cftTaskSearchService.searchUsingRoleCriteria(
            0,
            10,
            SearchRequest.builder().build(),
            List.of(RoleAssignment.builder()
                .roleName("tribunal-caseworker")
                .classification(Classification.PRIVATE)
                .grantType(GrantType.STANDARD)
                .attributes(Map.of(
                    RoleAttributeDefinition.JURISDICTION.value(), "WA",
                    RoleAttributeDefinition.REGION.value(), "1",
                    RoleAttributeDefinition.BASE_LOCATION.value(), "765324"
                ))
                .authorisations(List.of())
                .build()),
            List.of()
        );

        assertThat(result.taskIds()).hasSize(4);
        assertThat(result.totalRecords()).isEqualTo(2);
    }

    private void indexMatchingTasks() {
        List<TaskResource> tasks = new ArrayList<>();
        for (String taskId : MATCHING_TASK_IDS) {
            TaskResource task = taskResourceRepository.findById(taskId).orElseThrow();
            task.setIndexed(true);
            tasks.add(task);
        }
        taskResourceRepository.saveAll(tasks);
        entityManager.flush();
        entityManager.clear();
    }
}
