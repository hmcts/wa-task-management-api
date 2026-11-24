package uk.gov.hmcts.reform.wataskmanagementapi.services;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.wataskmanagementapi.config.IntegrationTest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.task.Task;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@IntegrationTest(properties = {
    "search.jurisdictionUsers.probate=probate-search@example.test",
    "search.jurisdictionUsers.ia=ia-search@example.test"
})
@Transactional
@Sql("/scripts/system_task_search.sql")
class SystemTaskSearchServiceTest {

    private static final String CASE_ID = "1793986596864362";

    @Autowired
    private SystemTaskSearchService systemTaskSearchService;

    @Test
    void should_use_the_other_users_configured_jurisdiction_for_the_same_case() {
        List<Task> tasks = systemTaskSearchService.searchTasks("ia-search@example.test", CASE_ID);

        assertThat(tasks)
            .extracting(Task::getId, Task::getJurisdiction)
            .containsExactly(tuple("70000000-0000-0000-0000-000000000003", "IA"));
    }
}
