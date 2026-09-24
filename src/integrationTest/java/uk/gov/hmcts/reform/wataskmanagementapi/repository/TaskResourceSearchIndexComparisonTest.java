//package uk.gov.hmcts.reform.wataskmanagementapi.repository;
//
//import lombok.extern.slf4j.Slf4j;
//import org.junit.jupiter.api.MethodOrderer;
//import org.junit.jupiter.api.Order;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.TestMethodOrder;
//import org.junit.jupiter.params.ParameterizedTest;
//import org.junit.jupiter.params.provider.Arguments;
//import org.junit.jupiter.params.provider.EnumSource;
//import org.junit.jupiter.params.provider.MethodSource;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.core.io.ClassPathResource;
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.transaction.annotation.Isolation;
//import org.springframework.transaction.annotation.Transactional;
//import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAttributeDefinition;
//import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType;
//import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleCategory;
//import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
//import uk.gov.hmcts.reform.wataskmanagementapi.config.IntegrationTest;
//import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.RequestContext;
//import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SearchRequest;
//import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SortField;
//import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SortOrder;
//import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SortingParameter;
//import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.TaskSearchRoleCriteria;
//import uk.gov.hmcts.reform.wataskmanagementapi.services.signature.RoleSignatureBuilder;
//import uk.gov.hmcts.reform.wataskmanagementapi.services.signature.SearchFilterSignatureBuilder;
//
//import java.io.IOException;
//import java.nio.charset.StandardCharsets;
//import java.sql.ResultSet;
//import java.sql.SQLException;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.LinkedHashSet;
//import java.util.List;
//import java.util.Locale;
//import java.util.Set;
//import java.util.function.Supplier;
//import java.util.stream.Collectors;
//import java.util.stream.Stream;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//@IntegrationTest(properties = {
//    "spring.datasource.driver-class-name=org.postgresql.Driver",
//    "spring.datasource.driverClassName=org.postgresql.Driver",
//    "spring.datasource.url=jdbc:postgresql://localhost:5432/postgres",
////        + "?options=-c%20default_transaction_read_only%3Don%20-c%20statement_timeout%3D20000",
//    "spring.datasource.jdbcUrl=jdbc:postgresql://localhost:5432/postgres",
////        + "?options=-c%20default_transaction_read_only%3Don%20-c%20statement_timeout%3D20000",
//    "spring.datasource.username=pgadmin",
//    "spring.datasource.password=pgadmin",
//    "spring.datasource.hikari.maximum-pool-size=2",
//    "spring.flyway.enabled=false",
//    "spring.jpa.hibernate.ddl-auto=none",
//    "logging.level.uk.gov.hmcts.reform.wataskmanagementapi.repository.TaskResourceCustomRepositoryImpl=WARN"
//})
//@Slf4j
//@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
//class TaskResourceSearchIndexComparisonTest {
//
//    private static final String SEARCH_SCENARIOS_SQL =
//        "scripts/search-index-comparison/real_search_scenarios.sql";
//    private static final int MAX_EXHAUSTIVE_RESULTS = 10_000;
//    private static final int COUNT_LIMIT = Integer.MAX_VALUE;
//    private static final double NANOS_PER_SECOND = 1_000_000_000.0;
//
//    @Autowired
//    private TaskResourceRepository taskResourceRepository;
//
//    @Autowired
//    private JdbcTemplate jdbcTemplate;
//
//    @Test
//    @Order(1)
//    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
//    void should_compare_all_three_searches_for_real_world_scenarios() {
//        List<SearchScenario> scenarios = loadRealWorldSearchScenarios();
//
//        assertThat(scenarios)
//            .as("search scenario script must provide all 100 recorded scenarios")
//            .hasSize(100);
//        assertThat(scenarios)
//            .extracting(SearchScenario::number)
//            .as("search scenario numbers must be unique")
//            .doesNotHaveDuplicates();
//        assertThat(scenarios)
//            .extracting(SearchScenario::description)
//            .as("search scenario descriptions must be unique")
//            .doesNotHaveDuplicates();
//
//        for (SearchScenario scenario : scenarios) {
//            assertThat(compareExhaustiveResults(scenario))
//                .as("scenario %s (%s) should still match its database snapshot",
//                    scenario.number(), scenario.description())
//                .hasSize(Math.toIntExact(scenario.expectedResultCount()))
//                .contains(scenario.taskId());
//        }
//
//        assertPaginationAndEmptyRoles(scenarios.getFirst());
//        assertCaseExclusions(scenarios);
//        assertUnsupportedLegacyStates(scenarios.getFirst());
//    }
//
//    @ParameterizedTest(name = "all 100 scenarios with context {0}")
//    @EnumSource(value = RequestContext.class, names = {"ALL_WORK", "AVAILABLE_TASKS"})
//    @Order(2)
//    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
//    void should_compare_all_three_searches_for_every_scenario_in_each_context(RequestContext context) {
//        List<SearchScenario> scenarios = loadRealWorldSearchScenarios();
//        assertThat(scenarios).hasSize(100);
//        int nonEmptyScenarios = 0;
//        for (SearchScenario scenario : scenarios) {
//            if (!compareExhaustiveResults(scenario.withRequestContext(context)).isEmpty()) {
//                nonEmptyScenarios++;
//            }
//        }
//        assertThat(nonEmptyScenarios).as("%s must exercise successful permission matches", context).isPositive();
//        log.info("All three searches match for context={}: scenarios={}, nonEmptyScenarios={}, emptyScenarios={}",
//            context, scenarios.size(), nonEmptyScenarios, scenarios.size() - nonEmptyScenarios);
//    }
//
//    @ParameterizedTest(name = "broad pages and relational counts from SQL: {0}, {1}")
//    @MethodSource("recordedRequestContexts")
//    @Order(3)
//    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
//    void should_compare_broad_ordered_pages_and_relational_counts(RequestContext context,
//                                                                 String jurisdictionSelection) {
//        for (RecordedSearchInput input : loadRecordedSearchInputs(context, jurisdictionSelection)) {
//            compareRecordedPages(input);
//        }
//    }
//
//    @ParameterizedTest(name = "broad unrestricted counts from SQL: {0}, {1}")
//    @MethodSource("recordedRequestContexts")
//    @Order(4)
//    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
//    void should_compare_broad_unrestricted_counts_with_search_index(RequestContext context,
//                                                                    String jurisdictionSelection) {
//        // Each invocation has its own snapshot and timeout. A legacy timeout remains a test failure,
//        // after all broad ordered-page and relational-count comparisons have already completed.
//        for (RecordedSearchInput input : loadRecordedSearchInputs(context, jurisdictionSelection)) {
//            SearchRequest request = input.scenario().request();
//            String description = describeRecordedRequest(request);
//            log.info("Comparing all three unrestricted counts: {}", description);
//            ThreeSearchResults<Long> counts = timeAllThree(
//                () -> taskResourceRepository.searchTasksCountUsingSearchIndex(
//                    input.filterSignatures(), input.roleSignatures(), input.excludedCaseIds(), request),
//                () -> taskResourceRepository.searchTasksCountUsingRoleCriteria(
//                    input.roleCriteria(), input.excludedCaseIds(), request, COUNT_LIMIT),
//                () -> taskResourceRepository.searchTasksCountUsingTaskRoles(
//                    input.roleCriteria(), input.excludedCaseIds(), request),
//                1); // Execute the two relational counts before the slow legacy count in the same snapshot.
//            assertEqualCounts(description, counts);
//            log.info("All three broad counts match: {}, count={}, search_indexCountSeconds={}, "
//                    + "task_search_permissionsCountSeconds={}, task_rolesCountSeconds={}",
//                description, counts.searchIndex().value(),
//                formatSeconds(counts.searchIndex().durationNanos()),
//                formatSeconds(counts.permissions().durationNanos()), formatSeconds(counts.taskRoles().durationNanos()));
//        }
//    }
//
//    private static Stream<Arguments> recordedRequestContexts() {
//        return Stream.of(RequestContext.ALL_WORK, RequestContext.AVAILABLE_TASKS)
//            .flatMap(context -> Stream.of("original", "IA+EMPLOYMENT", "IA+PUBLICLAW")
//                .map(jurisdictions -> Arguments.of(context, jurisdictions)));
//    }
//
//    private List<String> compareExhaustiveResults(SearchScenario scenario) {
//        String description = "%s scenario %s (%s)"
//            .formatted(scenario.requestContext(), scenario.number(), scenario.description());
//        ThreeSearchResults<Long> counts = timeAllThree(
//            () -> countSearchIndex(scenario), () -> countPermissions(scenario), () -> countTaskRoles(scenario),
//            scenario.number());
//        assertEqualCounts(description, counts);
//        assertThat(counts.searchIndex().value())
//            .as("%s must remain bounded for an exhaustive comparison", description)
//            .isBetween(0L, (long) MAX_EXHAUSTIVE_RESULTS);
//
//        int resultCount = Math.toIntExact(counts.searchIndex().value());
//        // Request one more row so an undercount cannot hide a matching task behind LIMIT.
//        int maxResults = resultCount + 1;
//        ThreeSearchResults<List<String>> tasks = timeAllThree(
//            () -> searchIndex(scenario, maxResults),
//            () -> searchPermissions(scenario, maxResults),
//            () -> searchTaskRoles(scenario, 0, maxResults, List.of()),
//            scenario.number() + 1);
//        assertOrderedIds(description, tasks);
//        assertThat(tasks.searchIndex().value())
//            .as("%s must return every counted task, with no additional matches", description)
//            .hasSize(resultCount);
//
//        logScenarioPerformance("Task ID search", scenario, tasks.searchIndex().durationNanos(),
//            tasks.permissions().durationNanos(), tasks.taskRoles().durationNanos());
//        logScenarioPerformance("Count", scenario, counts.searchIndex().durationNanos(),
//            counts.permissions().durationNanos(), counts.taskRoles().durationNanos());
//        return tasks.searchIndex().value();
//    }
//
//    private List<RecordedSearchInput> loadRecordedSearchInputs(RequestContext context, String jurisdictionSelection) {
//        List<String> selectedJurisdictions = "original".equals(jurisdictionSelection)
//            ? List.of() : List.of(jurisdictionSelection.split("\\+"));
//        List<RecordedSearchScenario> scenarios = jdbcTemplate.query(
//            loadSql("scripts/search-index-comparison/real_search_scenarios-levelup.sql"),
//            (result, row) -> RecordedSearchScenario.read(result, selectedJurisdictions, context));
//        assertThat(scenarios).as("levelup SQL must return at least one recorded request").isNotEmpty();
//        return scenarios.stream().map(RecordedSearchInput::from).toList();
//    }
//
//    private void compareRecordedPages(RecordedSearchInput input) {
//        RecordedSearchScenario scenario = input.scenario();
//        SearchRequest request = scenario.request();
//        String description = describeRecordedRequest(request);
//        log.info("Comparing recorded broad pages: description={}, {}, roleCriteria={}",
//            scenario.description(), description, input.roleCriteria().size());
//        ThreeSearchResults<List<String>> pages = timeAllThree(
//            () -> taskResourceRepository.searchTasksIdsUsingSearchIndex(
//                scenario.firstResult(), scenario.maxResults(), input.filterSignatures(), input.roleSignatures(),
//                input.excludedCaseIds(), request),
//            () -> taskResourceRepository.searchTasksIdsUsingRoleCriteria(
//                scenario.firstResult(), scenario.maxResults(), input.roleCriteria(), input.excludedCaseIds(), request),
//            () -> taskResourceRepository.searchTasksIdsUsingTaskRoles(
//                scenario.firstResult(), scenario.maxResults(), input.roleCriteria(), input.excludedCaseIds(), request),
//            0);
//        assertOrderedIds(description, pages);
//        log.info("Broad ordered pages match: {}, taskIds={}, search_indexSeconds={}, "
//                + "task_search_permissionsSeconds={}, task_rolesSeconds={}",
//            description, pages.searchIndex().value(),
//            formatSeconds(pages.searchIndex().durationNanos()), formatSeconds(pages.permissions().durationNanos()),
//            formatSeconds(pages.taskRoles().durationNanos()));
//
//        long count = compareRelationalCounts(input);
//        assertThat(pages.searchIndex().value()).hasSize((int) Math.min(
//            scenario.maxResults(), Math.max(0, count - scenario.firstResult())));
//    }
//
//    private long compareRelationalCounts(RecordedSearchInput input) {
//        SearchRequest request = input.scenario().request();
//        TimedValue<Long> taskRolesCount = time(() -> taskResourceRepository.searchTasksCountUsingTaskRoles(
//            input.roleCriteria(), input.excludedCaseIds(), request));
//        TimedValue<Long> permissionsCount = time(() -> taskResourceRepository.searchTasksCountUsingRoleCriteria(
//            input.roleCriteria(), input.excludedCaseIds(), request, COUNT_LIMIT));
//        String description = describeRecordedRequest(request);
//        assertThat(taskRolesCount.value()).as("%s: relational unrestricted counts must match", description)
//            .isEqualTo(permissionsCount.value());
//        log.info("Broad relational counts match: {}, count={}, task_search_permissionsCountSeconds={}, "
//                + "task_rolesCountSeconds={}, task_rolesCountUnderThreeSeconds={}",
//            description, taskRolesCount.value(), formatSeconds(permissionsCount.durationNanos()),
//            formatSeconds(taskRolesCount.durationNanos()), taskRolesCount.durationNanos() < 3 * NANOS_PER_SECOND);
//        return taskRolesCount.value();
//    }
//
//    private String describeRecordedRequest(SearchRequest request) {
//        RequestContext context = request.isAvailableTasksOnly()
//            ? RequestContext.AVAILABLE_TASKS : RequestContext.ALL_WORK;
//        return "context=%s, jurisdictions=%s".formatted(context, request.getJurisdictions());
//    }
//
//    private List<SearchScenario> loadRealWorldSearchScenarios() {
//        return jdbcTemplate.query(
//            loadSql(SEARCH_SCENARIOS_SQL),
//            (rs, rowNum) -> new SearchScenario(
//                rs.getInt("scenario_no"),
//                rs.getString("scenario_description"),
//                rs.getString("task_id"),
//                getStateList(rs, "states"),
//                getStringList(rs, "case_ids"),
//                rs.getString("assignee"),
//                getStringList(rs, "task_types"),
//                rs.getLong("expected_result_count"),
//                getStringSet(rs, "filter_signatures"),
//                getStringSet(rs, "role_signatures"),
//                getSortingParameter(rs)
//            )
//        );
//    }
//
//    private SortingParameter getSortingParameter(ResultSet resultSet) throws SQLException {
//        String sortField = resultSet.getString("sort_field");
//        String sortOrder = resultSet.getString("sort_order");
//
//        if (sortField == null && sortOrder == null) {
//            return null;
//        }
//        if (sortField == null || sortOrder == null) {
//            throw new IllegalArgumentException("Scenario sort_field and sort_order must both be provided");
//        }
//        return new SortingParameter(SortField.valueOf(sortField), SortOrder.valueOf(sortOrder));
//    }
//
//    private List<String> getStringList(ResultSet resultSet, String columnName) throws SQLException {
//        return Arrays.asList((String[]) resultSet.getArray(columnName).getArray());
//    }
//
//    private List<CFTTaskState> getStateList(ResultSet resultSet, String columnName) throws SQLException {
//        return getStringList(resultSet, columnName).stream()
//            .map(state -> CFTTaskState.from(state).orElseThrow())
//            .toList();
//    }
//
//    private Set<String> getStringSet(ResultSet resultSet, String columnName) throws SQLException {
//        return new LinkedHashSet<>(Arrays.asList((String[]) resultSet.getArray(columnName).getArray()));
//    }
//
//    private String loadSql(String resource) {
//        try {
//            return new ClassPathResource(resource)
//                .getContentAsString(StandardCharsets.UTF_8);
//        } catch (IOException exception) {
//            throw new IllegalStateException("Unable to load search scenarios", exception);
//        }
//    }
//
//    private List<String> searchIndex(SearchScenario scenario, int maxResults) {
//        return taskResourceRepository.searchTasksIdsUsingSearchIndex(
//            0,
//            maxResults,
//            scenario.filterSignatures(),
//            scenario.roleSignatures(),
//            List.of(),
//            scenario.toSearchRequest()
//        );
//    }
//
//    private List<String> searchPermissions(SearchScenario scenario, int maxResults) {
//        return taskResourceRepository.searchTasksIdsUsingRoleCriteria(
//            0,
//            maxResults,
//            scenario.toRoleCriteria(),
//            List.of(),
//            scenario.toSearchRequest()
//        );
//    }
//
//    private Long countSearchIndex(SearchScenario scenario) {
//        return taskResourceRepository.searchTasksCountUsingSearchIndex(
//            scenario.filterSignatures(),
//            scenario.roleSignatures(),
//            List.of(),
//            scenario.toSearchRequest()
//        );
//    }
//
//    private Long countPermissions(SearchScenario scenario) {
//        return taskResourceRepository.searchTasksCountUsingRoleCriteria(
//            scenario.toRoleCriteria(),
//            List.of(),
//            scenario.toSearchRequest(),
//            COUNT_LIMIT
//        );
//    }
//
//    private List<String> searchTaskRoles(SearchScenario scenario,
//                                         int firstResult,
//                                         int maxResults,
//                                         List<String> excludedCaseIds) {
//        return taskResourceRepository.searchTasksIdsUsingTaskRoles(
//            firstResult,
//            maxResults,
//            scenario.toRoleCriteria(),
//            excludedCaseIds,
//            scenario.toSearchRequest()
//        );
//    }
//
//    private Long countTaskRoles(SearchScenario scenario) {
//        return taskResourceRepository.searchTasksCountUsingTaskRoles(
//            scenario.toRoleCriteria(),
//            List.of(),
//            scenario.toSearchRequest()
//        );
//    }
//
//    private void assertPaginationAndEmptyRoles(SearchScenario scenario) {
//        List<String> expectedIds = searchIndex(scenario, Math.toIntExact(scenario.expectedResultCount()));
//        int offset = Math.min(1, expectedIds.size());
//        int pageSize = Math.min(2, expectedIds.size() - offset);
//        assertThat(searchTaskRoles(scenario, offset, pageSize, List.of()))
//            .as("task_roles pagination must preserve the corresponding legacy result slice")
//            .containsExactlyElementsOf(expectedIds.subList(offset, offset + pageSize));
//        assertThat(searchTaskRoles(scenario, expectedIds.size(), 25, List.of()))
//            .as("task_roles must return an empty page after the final matching task")
//            .isEmpty();
//        assertThat(countTaskRoles(scenario))
//            .as("task_roles count must remain exact independently of the requested page")
//            .isEqualTo((long) expectedIds.size());
//        assertThat(taskResourceRepository.searchTasksIdsUsingTaskRoles(
//            0, 25, List.of(), List.of(), scenario.toSearchRequest()))
//            .as("task_roles must not return tasks without any role criteria")
//            .isEmpty();
//        assertThat(taskResourceRepository.searchTasksCountUsingTaskRoles(
//            List.of(), List.of(), scenario.toSearchRequest()))
//            .as("task_roles must count zero tasks without any role criteria")
//            .isZero();
//    }
//
//    private void assertCaseExclusions(List<SearchScenario> scenarios) {
//        SearchScenario scenario = scenarios.stream()
//            .filter(candidate -> candidate.caseIds().size() > 1)
//            .findFirst()
//            .orElseThrow(() -> new IllegalStateException("A multiple-case exclusion scenario is required"));
//        List<String> excludedCaseIds = List.of(scenario.caseIds().getFirst());
//        SearchRequest request = scenario.toSearchRequest();
//        List<String> expectedIds = taskResourceRepository.searchTasksIdsUsingSearchIndex(
//            0, MAX_EXHAUSTIVE_RESULTS, scenario.filterSignatures(), scenario.roleSignatures(),
//            excludedCaseIds, request);
//        Long expectedCount = taskResourceRepository.searchTasksCountUsingSearchIndex(
//            scenario.filterSignatures(), scenario.roleSignatures(), excludedCaseIds, request);
//
//        assertThat(expectedCount)
//            .as("excluding a matching case must remove tasks from this scenario")
//            .isLessThan(scenario.expectedResultCount());
//        assertThat(expectedIds).hasSize(Math.toIntExact(expectedCount));
//        assertThat(searchTaskRoles(scenario, 0, MAX_EXHAUSTIVE_RESULTS, excludedCaseIds))
//            .as("task_roles must preserve the legacy case exclusions and ordering")
//            .containsExactlyElementsOf(expectedIds);
//        assertThat(taskResourceRepository.searchTasksCountUsingTaskRoles(
//            scenario.toRoleCriteria(), excludedCaseIds, request))
//            .as("task_roles must preserve the legacy count after excluding a case")
//            .isEqualTo(expectedCount);
//        assertThat(taskResourceRepository.searchTasksIdsUsingRoleCriteria(
//            0, MAX_EXHAUSTIVE_RESULTS, scenario.toRoleCriteria(), excludedCaseIds, request))
//            .as("task_search_permissions must preserve the same case exclusions and ordering")
//            .containsExactlyElementsOf(expectedIds);
//        assertThat(taskResourceRepository.searchTasksCountUsingRoleCriteria(
//            scenario.toRoleCriteria(), excludedCaseIds, request, COUNT_LIMIT))
//            .as("task_search_permissions must preserve the same count after excluding a case")
//            .isEqualTo(expectedCount);
//    }
//
//    private void assertUnsupportedLegacyStates(SearchScenario scenario) {
//        for (SearchRequest request : List.of(
//            SearchRequest.builder().cftTaskStates(List.of(CFTTaskState.COMPLETED)).build(),
//            SearchRequest.builder().cftTaskStates(List.of(CFTTaskState.CANCELLED)).build()
//        )) {
//            Set<String> filterSignatures = SearchFilterSignatureBuilder.buildFilterSignatures(request);
//            assertThat(taskResourceRepository.searchTasksCountUsingSearchIndex(
//                filterSignatures, scenario.roleSignatures(), List.of(), request))
//                .as("legacy filter abbreviations must reject unsupported explicit states")
//                .isZero();
//            assertThat(taskResourceRepository.searchTasksCountUsingTaskRoles(
//                scenario.toRoleCriteria(), List.of(), request))
//                .as("task_roles must preserve legacy counts for unsupported filter abbreviations")
//                .isZero();
//            assertThat(taskResourceRepository.searchTasksIdsUsingTaskRoles(
//                0, 25, scenario.toRoleCriteria(), List.of(), request))
//                .as("task_roles must preserve empty legacy results for unsupported filter abbreviations")
//                .isEmpty();
//        }
//    }
//
//    private void assertEqualCounts(String description, ThreeSearchResults<Long> results) {
//        assertThat(results.permissions().value())
//            .as("%s: task_search_permissions count must match search_index", description)
//            .isEqualTo(results.searchIndex().value());
//        assertThat(results.taskRoles().value())
//            .as("%s: task_roles count must match search_index", description)
//            .isEqualTo(results.searchIndex().value());
//    }
//
//    private void assertOrderedIds(String description, ThreeSearchResults<List<String>> results) {
//        assertThat(results.permissions().value())
//            .as("%s: task_search_permissions must preserve every search_index ID and its position", description)
//            .containsExactlyElementsOf(results.searchIndex().value());
//        assertThat(results.taskRoles().value())
//            .as("%s: task_roles must preserve every search_index ID and its position", description)
//            .containsExactlyElementsOf(results.searchIndex().value());
//        assertThat(results.searchIndex().value())
//            .as("%s: matching task IDs must not be duplicated", description)
//            .doesNotHaveDuplicates();
//    }
//
//    private <T> TimedValue<T> time(Supplier<T> operation) {
//        long started = System.nanoTime();
//        return new TimedValue<>(operation.get(), System.nanoTime() - started);
//    }
//
//    private <T> ThreeSearchResults<T> timeAllThree(Supplier<T> searchIndexOperation,
//                                           Supplier<T> permissionsOperation,
//                                           Supplier<T> taskRolesOperation,
//                                           int rotation) {
//        List<Supplier<T>> operations = List.of(searchIndexOperation, permissionsOperation, taskRolesOperation);
//        List<TimedValue<T>> results = new ArrayList<>(Arrays.asList(null, null, null));
//        for (int position = 0; position < operations.size(); position++) {
//            int index = (rotation + position) % operations.size();
//            results.set(index, time(operations.get(index)));
//        }
//        return new ThreeSearchResults<>(results.get(0), results.get(1), results.get(2));
//    }
//
//    private void logScenarioPerformance(String operation,
//                                        SearchScenario scenario,
//                                        long searchIndexDurationNanos,
//                                        long permissionsDurationNanos,
//                                        long taskRolesDurationNanos) {
//        log.info(
//            "{} context={} scenario={} taskId={}: search_indexSeconds={}, task_search_permissionsSeconds={}, "
//                + "task_rolesSeconds={}, task_rolesSpeedupVsSearchIndex={}, task_rolesSpeedupVsPermissions={}",
//            operation,
//            scenario.requestContext(),
//            scenario.number(),
//            scenario.taskId(),
//            formatSeconds(searchIndexDurationNanos),
//            formatSeconds(permissionsDurationNanos),
//            formatSeconds(taskRolesDurationNanos),
//            formatSpeedup(speedupRatio(searchIndexDurationNanos, taskRolesDurationNanos)),
//            formatSpeedup(speedupRatio(permissionsDurationNanos, taskRolesDurationNanos))
//        );
//    }
//
//    private double speedupRatio(double baselineDurationNanos, double candidateDurationNanos) {
//        return candidateDurationNanos == 0
//            ? 0
//            : baselineDurationNanos / candidateDurationNanos;
//    }
//
//    private String formatSeconds(double nanos) {
//        return formatDecimal(nanos / NANOS_PER_SECOND);
//    }
//
//    private String formatSpeedup(double value) {
//        return formatDecimal(value) + "x";
//    }
//
//    private String formatDecimal(double value) {
//        return String.format(Locale.ROOT, "%.3f", value);
//    }
//
//    private record TimedValue<T>(T value, long durationNanos) {
//    }
//
//    private record ThreeSearchResults<T>(TimedValue<T> searchIndex,
//                                       TimedValue<T> permissions,
//                                       TimedValue<T> taskRoles) {
//    }
//
//    private record RecordedSearchInput(RecordedSearchScenario scenario,
//                                       Set<String> filterSignatures,
//                                       Set<String> roleSignatures,
//                                       List<TaskSearchRoleCriteria> roleCriteria,
//                                       List<String> excludedCaseIds) {
//
//        private static RecordedSearchInput from(RecordedSearchScenario scenario) {
//            SearchRequest request = scenario.request();
//            Set<String> roleSignatures = RoleSignatureBuilder.buildRoleSignatures(scenario.roleAssignments(), request);
//            List<String> excludedCaseIds = scenario.roleAssignments().stream()
//                .filter(role -> role.getGrantType() == GrantType.EXCLUDED)
//                .flatMap(role -> role.getAttributeValue(RoleAttributeDefinition.CASE_ID).stream())
//                .toList();
//            return new RecordedSearchInput(
//                scenario, SearchFilterSignatureBuilder.buildFilterSignatures(request), roleSignatures,
//                roleSignatures.stream().map(SearchScenario::toRoleCriteria).toList(), excludedCaseIds);
//        }
//    }
//
//    private record SearchScenario(int number,
//                                  String description,
//                                  String taskId,
//                                  List<CFTTaskState> states,
//                                  List<String> caseIds,
//                                  String assignee,
//                                  List<String> taskTypes,
//                                  long expectedResultCount,
//                                  Set<String> filterSignatures,
//                                  Set<String> roleSignatures,
//                                  SortingParameter sortingParameter) {
//
//        private SearchRequest toSearchRequest() {
//            return toSearchRequest(requestContext());
//        }
//
//        private SearchRequest toSearchRequest(RequestContext context) {
//            SearchFilterCriteria filterCriteria = SearchFilterCriteria.from(filterSignatures);
//            SearchRequest.SearchRequestBuilder builder = SearchRequest.builder()
//                .cftTaskStates(context == RequestContext.AVAILABLE_TASKS ? List.of(CFTTaskState.UNASSIGNED) : states)
//                .jurisdictions(filterCriteria.jurisdictions())
//                .roleCategories(filterCriteria.roleCategories())
//                .workTypes(filterCriteria.workTypes())
//                .regions(filterCriteria.regions())
//                .locations(filterCriteria.locations())
//                .taskTypes(taskTypes)
//                .sortingParameters(sortingParameter == null ? List.of() : List.of(sortingParameter))
//                .requestContext(context);
//
//            if (!caseIds.isEmpty()) {
//                builder.caseIds(caseIds);
//            }
//
//            if (assignee != null && !assignee.isBlank()) {
//                builder.users(List.of(assignee));
//            }
//
//            return builder.build();
//        }
//
//        private SearchScenario withRequestContext(RequestContext context) {
//            SearchRequest request = toSearchRequest(context);
//            Set<String> contextRoles = roleSignatures.stream()
//                .map(signature -> roleSignatureForContext(signature, context))
//                .collect(Collectors.toCollection(LinkedHashSet::new));
//            return new SearchScenario(number, description, taskId, request.getCftTaskStates(), caseIds, assignee,
//                taskTypes, expectedResultCount, SearchFilterSignatureBuilder.buildFilterSignatures(request),
//                contextRoles, sortingParameter);
//        }
//
//        private static String roleSignatureForContext(String signature, RequestContext context) {
//            String[] parts = roleSignatureParts(signature);
//            parts[5] = context == RequestContext.AVAILABLE_TASKS ? "a" : "m";
//            if (context == RequestContext.ALL_WORK || !"*".equals(parts[4])) {
//                parts[7] = "*";
//            }
//            // Preserve recorded organisational skills. Read/manage fixtures only contain wildcard skills;
//            // the broad SQL fixtures exercise full role assignments and their available-task authorisations.
//            return String.join(":", parts);
//        }
//
//        private List<TaskSearchRoleCriteria> toRoleCriteria() {
//            return roleSignatures.stream()
//                .map(SearchScenario::toRoleCriteria)
//                .toList();
//        }
//
//        private static TaskSearchRoleCriteria toRoleCriteria(String roleSignature) {
//            String[] parts = roleSignatureParts(roleSignature);
//
//            return new TaskSearchRoleCriteria(
//                nullIfWildcard(parts[0]),
//                nullIfWildcard(parts[1]),
//                nullIfWildcard(parts[2]),
//                parts[3],
//                nullIfWildcard(parts[4]),
//                parts[5],
//                parts[6],
//                nullIfWildcard(parts[7])
//            );
//        }
//
//        private RequestContext requestContext() {
//            List<String> permissions = roleSignatures.stream()
//                .map(SearchScenario::roleSignatureParts)
//                .map(parts -> parts[5])
//                .distinct()
//                .toList();
//
//            if (permissions.size() != 1) {
//                throw new IllegalArgumentException(
//                    "Scenario " + number + " must contain exactly one role permission: " + permissions
//                );
//            }
//
//            return switch (permissions.get(0)) {
//                case "a" -> RequestContext.AVAILABLE_TASKS;
//                case "m" -> RequestContext.ALL_WORK;
//                case "r" -> null;
//                default -> throw new IllegalArgumentException(
//                    "Invalid role permission in scenario " + number + ": " + permissions.get(0)
//                );
//            };
//        }
//
//        private static String[] roleSignatureParts(String roleSignature) {
//            String[] parts = roleSignature.split(":", 8);
//            if (parts.length != 8) {
//                throw new IllegalArgumentException("Invalid role signature: " + roleSignature);
//            }
//            return parts;
//        }
//
//        private static String nullIfWildcard(String value) {
//            return "*".equals(value) ? null : value;
//        }
//    }
//
//    private record SearchFilterCriteria(List<String> jurisdictions,
//                                        List<RoleCategory> roleCategories,
//                                        List<String> workTypes,
//                                        List<String> regions,
//                                        List<String> locations) {
//
//        private static SearchFilterCriteria from(Set<String> filterSignatures) {
//            Set<String> jurisdictions = new LinkedHashSet<>();
//            Set<RoleCategory> roleCategories = new LinkedHashSet<>();
//            Set<String> workTypes = new LinkedHashSet<>();
//            Set<String> regions = new LinkedHashSet<>();
//            Set<String> locations = new LinkedHashSet<>();
//
//            for (String filterSignature : filterSignatures) {
//                String[] parts = filterSignature.split(":", -1);
//                if (parts.length != 6) {
//                    throw new IllegalArgumentException("Invalid filter signature: " + filterSignature);
//                }
//
//                addIfConstrained(jurisdictions, parts[1]);
//                if (!"*".equals(parts[2])) {
//                    roleCategories.add(expandRoleCategory(parts[2]));
//                }
//                addIfConstrained(workTypes, parts[3]);
//                addIfConstrained(regions, parts[4]);
//                addIfConstrained(locations, parts[5]);
//            }
//
//            return new SearchFilterCriteria(
//                List.copyOf(jurisdictions),
//                List.copyOf(roleCategories),
//                List.copyOf(workTypes),
//                List.copyOf(regions),
//                List.copyOf(locations)
//            );
//        }
//
//        private static void addIfConstrained(Set<String> values, String value) {
//            if (!"*".equals(value)) {
//                values.add(value);
//            }
//        }
//
//        private static RoleCategory expandRoleCategory(String value) {
//            return switch (value) {
//                case "J" -> RoleCategory.JUDICIAL;
//                case "L" -> RoleCategory.LEGAL_OPERATIONS;
//                case "A" -> RoleCategory.ADMIN;
//                case "C" -> RoleCategory.CTSC;
//                case "E" -> RoleCategory.ENFORCEMENT;
//                default -> throw new IllegalArgumentException("Invalid filter role category: " + value);
//            };
//        }
//    }
//}
