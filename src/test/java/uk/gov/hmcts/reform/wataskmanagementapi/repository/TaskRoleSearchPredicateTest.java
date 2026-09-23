package uk.gov.hmcts.reform.wataskmanagementapi.repository;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.TaskSearchRoleCriteria;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TaskRoleSearchPredicateTest {

    @Test
    void should_preserve_manage_and_available_permission_correlation_for_counts() {
        List<TaskSearchRoleCriteria> criteria = List.of(
            new TaskSearchRoleCriteria("IA", "1", "765324", "reader", null, "r", "U", null),
            new TaskSearchRoleCriteria("IA", null, null, "manager", null, "m", "R", null),
            new TaskSearchRoleCriteria("IA", null, null, "owner", null, "a", "P", null),
            new TaskSearchRoleCriteria("IA", null, null, "owner", null, "a", "P", "skill-1"),
            new TaskSearchRoleCriteria("IA", null, null, "case-owner", "case-1", "a", "P", null)
        );

        TaskRoleSearchPredicate page = TaskRoleSearchPredicate.forPage(criteria);
        TaskRoleSearchPredicate count = TaskRoleSearchPredicate.from(criteria);

        assertThat(page.sql().split("OFFSET 0\\s*\\)", -1)).hasSize(4);
        assertThat(count.sql().split("OFFSET 0\\s*\\)", -1)).hasSize(3);
        assertThat(count.sql()).startsWith("EXISTS (").doesNotContain("LIMIT");
        assertThat(page.parameters()).usingRecursiveComparison().isEqualTo(count.parameters());
        assertThat(page.parameters())
            .containsEntry("scope_r_0_region", "1")
            .containsEntry("scope_r_0_location", "765324")
            .containsEntry("scope_a_0_acceptsWildcard", true)
            .containsEntry("scope_a_1_caseId", "case-1")
            .doesNotContainKeys("scope_m_0_authorizations", "scope_r_0_authorizations", "scope_a_1_authorizations");
        assertThat(count.sql())
            .contains(":scope_a_0_authorizations")
            .doesNotContain(":scope_m_0_authorizations", ":scope_r_0_authorizations", ":scope_a_1_authorizations");
        assertThat((String[]) page.parameters().get("scope_a_0_authorizations")).containsExactly("skill-1");
    }

    @Test
    void should_expose_selective_task_scopes_outside_the_page_permission_check() {
        List<TaskSearchRoleCriteria> criteria = List.of(
            new TaskSearchRoleCriteria(null, null, null, "case-reader", "case-1", "r", "U", null),
            new TaskSearchRoleCriteria(null, "1", "765324", "location-reader", null, "r", "U", null)
        );

        TaskRoleSearchPredicate page = TaskRoleSearchPredicate.forPage(criteria);
        String taskScopes = page.sql().substring(0, page.sql().indexOf("EXISTS"));

        assertThat(taskScopes)
            .contains("t.case_id = CAST(:scope_r_0_caseId AS text)", " OR ",
                "t.region = CAST(:scope_r_1_region AS text) AND t.location = CAST(:scope_r_1_location AS text)")
            .doesNotContain("role_name", "authorizations", "IS NULL");
        assertThat(page.sql())
            .contains("tr.role_name IN (:scope_r_0_roleNames)", "tr.role_name IN (:scope_r_1_roleNames)");
        assertThat(TaskRoleSearchPredicate.from(criteria).sql()).startsWith("EXISTS");
    }

    @Test
    void should_leave_task_scan_unrestricted_when_a_role_has_no_task_scope() {
        TaskRoleSearchPredicate page = TaskRoleSearchPredicate.forPage(List.of(
            new TaskSearchRoleCriteria(null, null, null, "reader", null, "r", "U", null)
        ));

        assertThat(page.sql().substring(0, page.sql().indexOf("EXISTS"))).contains("TRUE");
        assertThat(page.sql()).contains("tr.role_name IN (:scope_r_0_roleNames)", "OFFSET 0");
    }

    @Test
    void should_apply_common_classification_only_on_the_task_scan() {
        List<TaskSearchRoleCriteria> criteria = List.of(
            new TaskSearchRoleCriteria("IA", null, null, "manager", null, "m", "U", null),
            new TaskSearchRoleCriteria("PUBLICLAW", null, null, "owner", null, "a", "U", null)
        );

        for (TaskRoleSearchPredicate predicate : List.of(TaskRoleSearchPredicate.from(criteria),
                                                       TaskRoleSearchPredicate.forPage(criteria))) {
            assertThat(predicate.taskClassificationSql()).isEqualTo("t.security_classification = 'PUBLIC'");
            assertThat(predicate.sql()).doesNotContain("security_classification");
            assertThat(predicate.parameters())
                .doesNotContainKeys("taskRoleClassifications",
                                    "scope_m_0_classifications",
                                    "scope_a_0_classifications");
        }
    }

    @Test
    void should_keep_narrower_classification_checks_attached_to_their_roles() {
        List<TaskSearchRoleCriteria> criteria = List.of(
            new TaskSearchRoleCriteria("IA", null, null, "public-manager", null, "m", "U", null),
            new TaskSearchRoleCriteria("IA", null, null, "private-manager", null, "m", "P", null),
            new TaskSearchRoleCriteria("IA", null, null, "restricted-manager", null, "m", "R", null)
        );

        for (TaskRoleSearchPredicate predicate : List.of(TaskRoleSearchPredicate.from(criteria),
                                                       TaskRoleSearchPredicate.forPage(criteria))) {
            assertThat(predicate.taskClassificationSql())
                .isEqualTo("t.security_classification IN ('PUBLIC', 'PRIVATE', 'RESTRICTED')");
            assertThat(predicate.sql())
                .contains("AND t.security_classification = 'PUBLIC'\n    AND tr.role_name IN (:scope_m_0_roleNames)",
                    "AND t.security_classification IN ('PUBLIC', 'PRIVATE')\n"
                        + "    AND tr.role_name IN (:scope_m_1_roleNames)")
                .doesNotContain("'RESTRICTED'");
        }
    }

    @Test
    void should_deny_pages_and_counts_when_no_roles_are_supplied() {
        TaskRoleSearchPredicate page = TaskRoleSearchPredicate.forPage(List.of());
        TaskRoleSearchPredicate count = TaskRoleSearchPredicate.from(List.of());

        assertThat(page.sql()).isEqualTo("FALSE");
        assertThat(count.sql()).isEqualTo("FALSE");
        assertThat(page.parameters()).usingRecursiveComparison().isEqualTo(count.parameters());
    }

    @Test
    void should_deny_pages_and_counts_when_roles_cannot_match_permissions() {
        List<TaskSearchRoleCriteria> criteria = List.of(
            new TaskSearchRoleCriteria("IA", null, null, "reader", null, "invalid", "U", null),
            new TaskSearchRoleCriteria("IA", null, null, "reader", null, "r", "invalid", null)
        );

        assertThat(TaskRoleSearchPredicate.forPage(criteria).sql()).isEqualTo("FALSE");
        assertThat(TaskRoleSearchPredicate.from(criteria).sql()).isEqualTo("FALSE");
    }
}
