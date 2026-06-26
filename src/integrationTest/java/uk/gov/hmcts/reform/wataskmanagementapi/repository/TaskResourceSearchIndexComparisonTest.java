package uk.gov.hmcts.reform.wataskmanagementapi.repository;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleCategory;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.config.IntegrationTest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.RequestContext;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SearchRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.TaskSearchRoleCriteria;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest(properties = {
    "spring.datasource.driver-class-name=org.postgresql.Driver",
    "spring.datasource.driverClassName=org.postgresql.Driver",
    "spring.datasource.url=jdbc:postgresql://localhost:5432/postgres",
    "spring.datasource.jdbcUrl=jdbc:postgresql://localhost:5432/postgres",
    "spring.datasource.username=pgadmin",
    "spring.datasource.password=pgadmin",
    "spring.datasource.hikari.maximum-pool-size=2",
    "spring.flyway.enabled=false",
    "logging.level.uk.gov.hmcts.reform.wataskmanagementapi.repository.TaskResourceCustomRepositoryImpl=WARN"
})
@Slf4j
class TaskResourceSearchIndexComparisonTest {

    private static final String SEARCH_SCENARIOS_SQL =
        "scripts/search-index-comparison/real_search_scenarios.sql";
    private static final String GENERIC_ALL_WORK_SCENARIOS_SQL = """
        SELECT
            10000 + ROW_NUMBER() OVER (
                ORDER BY COUNT(DISTINCT permissions.task_id) DESC, permissions.role_name
            ) AS scenario_no,
            MIN(permissions.task_id) AS task_id,
            COUNT(DISTINCT permissions.task_id)::INTEGER AS expected_result_floor,
            ARRAY['*:*:*:*:*:*']::TEXT[] AS filter_signatures,
            ARRAY['*:*:*:' || permissions.role_name || ':*:m:R:*']::TEXT[] AS role_signatures
        FROM cft_task_db.task_permissions permissions
        JOIN cft_task_db.tasks task
          ON task.task_id = permissions.task_id
        WHERE task.indexed
          AND task.state IN ('ASSIGNED', 'UNASSIGNED')
          AND permissions.permission = 'm'
          AND permissions.role_name IS NOT NULL
        GROUP BY permissions.role_name
        HAVING COUNT(DISTINCT permissions.task_id) > 25
        ORDER BY COUNT(DISTINCT permissions.task_id) DESC, permissions.role_name
        LIMIT 5
        """;
    private static final int REQUIRED_SCENARIOS = 500;
    private static final int REQUIRED_GENERIC_ALL_WORK_SCENARIOS = 5;
    private static final int MAX_RESULTS = 25;
    private static final int MINIMUM_RESULT_COUNT = 0;
    private static final int TOP_SCENARIO_LOG_LIMIT = 5;
    private static final double NANOS_PER_MILLISECOND = 1_000_000.0;

    @Autowired
    private TaskResourceRepository taskResourceRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void should_return_same_task_ids_for_old_search_index_and_new_signature_search() {
        List<SearchScenario> scenarios = loadRealWorldSearchScenarios();

        assertThat(scenarios)
            .as("local cft_task_db must contain at least %s indexed task scenarios", REQUIRED_SCENARIOS)
            .hasSize(REQUIRED_SCENARIOS);

        List<ScenarioComparison> comparisons = new ArrayList<>(scenarios.size());
        int fullPageScenarios = 0;
        int countComparedScenarios = 0;

        for (int index = 0; index < scenarios.size(); index++) {
            SearchScenario scenario = scenarios.get(index);

            TimedValue<List<String>> oldSearchResult = time(() -> searchOld(scenario));
            TimedValue<List<String>> newSearchResult = time(() -> searchNew(scenario));

            assertThat(newSearchResult.value())
                .as("scenario %s taskId=%s should match old search results", scenario.scenarioNo(), scenario.taskId())
                .containsExactlyElementsOf(oldSearchResult.value());

            assertThat(newSearchResult.value())
                .as("scenario %s should return more than %s tasks using filterSignatures=%s roleSignatures=%s",
                    scenario.scenarioNo(),
                    MINIMUM_RESULT_COUNT,
                    scenario.filterSignatures().size(),
                    scenario.roleSignatures().size())
                .hasSizeGreaterThan(MINIMUM_RESULT_COUNT);

            if (newSearchResult.value().size() == MAX_RESULTS) {
                fullPageScenarios++;
            }

            TimedValue<Long> oldSearchCount = time(() -> countOld(scenario));
            TimedValue<Long> newSearchCount = time(() -> countNew(scenario));
            countComparedScenarios++;

            assertThat(newSearchCount.value())
                .as("scenario %s taskId=%s should match old search count",
                    scenario.scenarioNo(),
                    scenario.taskId())
                .isEqualTo(oldSearchCount.value());

            comparisons.add(new ScenarioComparison(
                scenario,
                oldSearchResult,
                newSearchResult,
                oldSearchCount,
                newSearchCount
            ));
        }

        assertThat(fullPageScenarios)
            .as("at least one scenario should return more than the %s task IDs visible on the first page", MAX_RESULTS)
            .isPositive();

        logPerformanceComparison(comparisons, fullPageScenarios, countComparedScenarios);
    }

    @Test
    void should_return_generic_all_work_results_for_new_signature_search() {
        List<SearchScenario> scenarios = loadGenericAllWorkSearchScenarios();

        assertThat(scenarios)
            .as("local cft_task_db must contain at least %s generic all-work scenarios",
                REQUIRED_GENERIC_ALL_WORK_SCENARIOS)
            .hasSize(REQUIRED_GENERIC_ALL_WORK_SCENARIOS);

        List<GenericAllWorkComparison> comparisons = new ArrayList<>(scenarios.size());

        for (int index = 0; index < scenarios.size(); index++) {
            SearchScenario scenario = scenarios.get(index);

            TimedValue<List<String>> newSearchResult = time(() -> searchNew(scenario));
            TimedValue<Long> newSearchCount = time(() -> countNew(scenario));

            assertThat(newSearchResult.value())
                .as("generic all-work scenario %s should return a full first page", scenario.scenarioNo())
                .hasSize(MAX_RESULTS);

            assertThat(newSearchCount.value())
                .as("generic all-work scenario %s taskId=%s should match the direct permission count",
                    scenario.scenarioNo(),
                    scenario.taskId())
                .isEqualTo((long) scenario.expectedResultFloor());

            comparisons.add(new GenericAllWorkComparison(
                scenario,
                newSearchResult,
                newSearchCount
            ));
        }

        logGenericAllWorkPerformance(comparisons);
    }

    private List<SearchScenario> loadRealWorldSearchScenarios() {
        return jdbcTemplate.query(
            loadSearchScenariosSql(),
            (rs, rowNum) -> new SearchScenario(
                rs.getInt("scenario_no"),
                rs.getString("task_id"),
                List.of(CFTTaskState.from(rs.getString("state")).orElseThrow()),
                getStringList(rs, "case_ids"),
                rs.getString("assignee"),
                RequestContext.AVAILABLE_TASKS,
                rs.getInt("expected_result_floor"),
                getStringSet(rs, "filter_signatures"),
                getStringSet(rs, "role_signatures")
            )
        );
    }

    private List<SearchScenario> loadGenericAllWorkSearchScenarios() {
        return jdbcTemplate.query(
            GENERIC_ALL_WORK_SCENARIOS_SQL,
            (rs, rowNum) -> new SearchScenario(
                rs.getInt("scenario_no"),
                rs.getString("task_id"),
                List.of(),
                List.of(),
                null,
                RequestContext.ALL_WORK,
                rs.getInt("expected_result_floor"),
                getStringSet(rs, "filter_signatures"),
                getStringSet(rs, "role_signatures")
            )
        );
    }

    private List<String> getStringList(ResultSet resultSet, String columnName) throws SQLException {
        return Arrays.asList((String[]) resultSet.getArray(columnName).getArray());
    }

    private Set<String> getStringSet(ResultSet resultSet, String columnName) throws SQLException {
        return new LinkedHashSet<>(Arrays.asList((String[]) resultSet.getArray(columnName).getArray()));
    }

    private String loadSearchScenariosSql() {
        try {
            return new ClassPathResource(SEARCH_SCENARIOS_SQL)
                .getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load " + SEARCH_SCENARIOS_SQL, exception);
        }
    }

    private List<String> searchOld(SearchScenario scenario) {
        return taskResourceRepository.searchTasksIdsOld(
            0,
            MAX_RESULTS,
            scenario.filterSignatures(),
            scenario.roleSignatures(),
            List.of(),
            scenario.toSearchRequest()
        );
    }

    private List<String> searchNew(SearchScenario scenario) {
        return taskResourceRepository.searchTasksIds(
            0,
            MAX_RESULTS,
            scenario.toRoleCriteria(),
            List.of(),
            scenario.toSearchRequest()
        );
    }

    private Long countOld(SearchScenario scenario) {
        return taskResourceRepository.searchTasksCountOld(
            scenario.filterSignatures(),
            scenario.roleSignatures(),
            List.of(),
            scenario.toSearchRequest()
        );
    }

    private Long countNew(SearchScenario scenario) {
        return taskResourceRepository.searchTasksCount(
            scenario.toRoleCriteria(),
            List.of(),
            scenario.toSearchRequest()
        );
    }

    private <T> TimedValue<T> time(Supplier<T> search) {
        long started = System.nanoTime();
        T value = search.get();
        return new TimedValue<>(value, System.nanoTime() - started);
    }

    private void logPerformanceComparison(List<ScenarioComparison> comparisons,
                                          int fullPageScenarios,
                                          int countComparedScenarios) {
        final TimingSummary oldSearchSummary = summarize(
            "old_search_index_task_ids",
            comparisons.stream().map(ScenarioComparison::oldSearchDurationNanos).toList()
        );
        final TimingSummary newSearchSummary = summarize(
            "new_signature_search_task_ids",
            comparisons.stream().map(ScenarioComparison::newSearchDurationNanos).toList()
        );
        final TimingSummary oldCountSummary = summarize(
            "old_search_index_count",
            comparisons.stream().map(ScenarioComparison::oldCountDurationNanos).toList()
        );
        final TimingSummary newCountSummary = summarize(
            "new_signature_search_count",
            comparisons.stream().map(ScenarioComparison::newCountDurationNanos).toList()
        );

        int partialPageScenarios = comparisons.size() - fullPageScenarios;
        long noCaseIdScenarios = comparisons.stream()
            .filter(comparison -> comparison.scenario().caseIds().isEmpty())
            .count();
        long allWorkScenarios = comparisons.stream()
            .filter(comparison -> comparison.scenario().requestContext() == RequestContext.ALL_WORK)
            .count();
        int minResultCount = comparisons.stream().mapToInt(ScenarioComparison::resultCount).min().orElse(0);
        int maxResultCount = comparisons.stream().mapToInt(ScenarioComparison::resultCount).max().orElse(0);
        double averageResultCount = comparisons.stream().mapToInt(ScenarioComparison::resultCount).average().orElse(0);

        log.info(
            "Search index comparison results: scenarios={}, noCaseIdScenarios={}, fullPageScenarios={}, "
                + "countComparedScenarios={}, partialPageScenarios={}, allWorkScenarios={}, resultCountMin={}, "
                + "resultCountAvg={}, resultCountMax={}",
            comparisons.size(),
            noCaseIdScenarios,
            fullPageScenarios,
            countComparedScenarios,
            partialPageScenarios,
            allWorkScenarios,
            minResultCount,
            formatDecimal(averageResultCount),
            maxResultCount
        );
        logTimingSummary(oldSearchSummary);
        logTimingSummary(newSearchSummary);
        logTimingSummary(oldCountSummary);
        logTimingSummary(newCountSummary);

        logRelativePerformance(
            "task_id_search",
            comparisons,
            ScenarioComparison::oldSearchDurationNanos,
            ScenarioComparison::newSearchDurationNanos
        );
        logRelativePerformance(
            "count_search",
            comparisons,
            ScenarioComparison::oldCountDurationNanos,
            ScenarioComparison::newCountDurationNanos
        );

        logScenarios(
            "Search index slowest old_search_index scenarios",
            comparisons.stream()
                .sorted(Comparator.comparingLong(ScenarioComparison::oldSearchDurationNanos).reversed())
                .limit(TOP_SCENARIO_LOG_LIMIT)
                .toList()
        );
        logScenarios(
            "Search index fastest old_search_index scenarios",
            comparisons.stream()
                .sorted(Comparator.comparingLong(ScenarioComparison::oldSearchDurationNanos))
                .limit(TOP_SCENARIO_LOG_LIMIT)
                .toList()
        );
        logScenarios(
            "Search index slowest new_signature_search scenarios",
            comparisons.stream()
                .sorted(Comparator.comparingLong(ScenarioComparison::newSearchDurationNanos).reversed())
                .limit(TOP_SCENARIO_LOG_LIMIT)
                .toList()
        );
        logScenarios(
            "Search index fastest new_signature_search scenarios",
            comparisons.stream()
                .sorted(Comparator.comparingLong(ScenarioComparison::newSearchDurationNanos))
                .limit(TOP_SCENARIO_LOG_LIMIT)
                .toList()
        );
        logScenarios(
            "Search index largest new_signature_search improvements",
            comparisons.stream()
                .filter(comparison -> comparison.oldSearchDurationNanos() > comparison.newSearchDurationNanos())
                .sorted(Comparator.comparingLong(ScenarioComparison::oldSearchMinusNewSearchNanos).reversed())
                .limit(TOP_SCENARIO_LOG_LIMIT)
                .toList()
        );
        logScenarios(
            "Search index largest new_signature_search regressions",
            comparisons.stream()
                .filter(comparison -> comparison.newSearchDurationNanos() > comparison.oldSearchDurationNanos())
                .sorted(Comparator.comparingLong(ScenarioComparison::newSearchMinusOldSearchNanos).reversed())
                .limit(TOP_SCENARIO_LOG_LIMIT)
                .toList()
        );
    }

    private void logGenericAllWorkPerformance(List<GenericAllWorkComparison> comparisons) {
        log.info(
            "Generic all-work comparison results: scenarios={}, resultCountMin={}, resultCountAvg={}, "
                + "resultCountMax={}",
            comparisons.size(),
            comparisons.stream().mapToLong(GenericAllWorkComparison::totalCount).min().orElse(0),
            formatDecimal(comparisons.stream().mapToLong(GenericAllWorkComparison::totalCount).average().orElse(0)),
            comparisons.stream().mapToLong(GenericAllWorkComparison::totalCount).max().orElse(0)
        );
        logTimingSummary(summarize(
            "new_signature_search_generic_all_work_task_ids",
            comparisons.stream().map(GenericAllWorkComparison::searchDurationNanos).toList()
        ));
        logTimingSummary(summarize(
            "new_signature_search_generic_all_work_count",
            comparisons.stream().map(GenericAllWorkComparison::countDurationNanos).toList()
        ));

        log.info(
            "Generic all-work slowest new_signature_search scenarios: {}",
            comparisons.stream()
                .sorted(Comparator.comparingLong(GenericAllWorkComparison::countDurationNanos).reversed())
                .limit(TOP_SCENARIO_LOG_LIMIT)
                .map(this::formatGenericAllWorkComparison)
                .collect(Collectors.joining(" | "))
        );
    }

    private void logRelativePerformance(String label,
                                        List<ScenarioComparison> comparisons,
                                        ToLongFunction<ScenarioComparison> oldDuration,
                                        ToLongFunction<ScenarioComparison> newDuration) {
        long newFasterScenarios = comparisons.stream()
            .filter(comparison -> newDuration.applyAsLong(comparison) < oldDuration.applyAsLong(comparison))
            .count();
        long oldFasterScenarios = comparisons.stream()
            .filter(comparison -> oldDuration.applyAsLong(comparison) < newDuration.applyAsLong(comparison))
            .count();
        long equalTimingScenarios = comparisons.size() - newFasterScenarios - oldFasterScenarios;
        long oldTotalNanos = comparisons.stream().mapToLong(oldDuration).sum();
        long newTotalNanos = comparisons.stream().mapToLong(newDuration).sum();
        long totalSavedNanos = oldTotalNanos - newTotalNanos;
        double averageSpeedupRatio = comparisons.stream()
            .mapToDouble(comparison ->
                oldDuration.applyAsLong(comparison) / (double) newDuration.applyAsLong(comparison))
            .average()
            .orElse(0);

        log.info(
            "Search index {} relative performance: newFasterScenarios={}, oldFasterScenarios={}, "
                + "equalTimingScenarios={}, totalSavedMs={}, averageSavedMs={}, averageSpeedupRatio={}x",
            label,
            newFasterScenarios,
            oldFasterScenarios,
            equalTimingScenarios,
            formatMillis(totalSavedNanos),
            formatMillis(totalSavedNanos / (double) comparisons.size()),
            formatDecimal(averageSpeedupRatio)
        );
    }

    private TimingSummary summarize(String implementation, List<Long> durationsNanos) {
        List<Long> sortedDurations = durationsNanos.stream().sorted().toList();
        long totalNanos = durationsNanos.stream().mapToLong(Long::longValue).sum();
        double averageNanos = totalNanos / (double) durationsNanos.size();
        double standardDeviationNanos = Math.sqrt(
            durationsNanos.stream()
                .mapToDouble(durationNanos -> Math.pow(durationNanos - averageNanos, 2))
                .average()
                .orElse(0)
        );

        return new TimingSummary(
            implementation,
            durationsNanos.size(),
            totalNanos,
            sortedDurations.get(0),
            percentile(sortedDurations, 50),
            percentile(sortedDurations, 90),
            percentile(sortedDurations, 95),
            percentile(sortedDurations, 99),
            sortedDurations.get(sortedDurations.size() - 1),
            averageNanos,
            standardDeviationNanos
        );
    }

    private long percentile(List<Long> sortedDurations, int percentile) {
        int index = (int) Math.ceil(percentile / 100.0 * sortedDurations.size()) - 1;
        return sortedDurations.get(Math.max(0, Math.min(index, sortedDurations.size() - 1)));
    }

    private void logTimingSummary(TimingSummary summary) {
        log.info(
            "Search index {} timing: count={}, totalMs={}, avgMs={}, minMs={}, p50Ms={}, p90Ms={}, "
                + "p95Ms={}, p99Ms={}, maxMs={}, stdDevMs={}",
            summary.implementation(),
            summary.count(),
            formatMillis(summary.totalNanos()),
            formatMillis(summary.averageNanos()),
            formatMillis(summary.minNanos()),
            formatMillis(summary.p50Nanos()),
            formatMillis(summary.p90Nanos()),
            formatMillis(summary.p95Nanos()),
            formatMillis(summary.p99Nanos()),
            formatMillis(summary.maxNanos()),
            formatMillis(summary.standardDeviationNanos())
        );
    }

    private void logScenarios(String label, List<ScenarioComparison> comparisons) {
        if (comparisons.isEmpty()) {
            log.info("{}: none", label);
            return;
        }

        log.info(
            "{}: {}",
            label,
            comparisons.stream()
                .map(this::formatScenarioComparison)
                .collect(Collectors.joining(" | "))
        );
    }

    private String formatScenarioComparison(ScenarioComparison comparison) {
        SearchScenario scenario = comparison.scenario();
        return String.format(
            Locale.ROOT,
            "#%d taskId=%s context=%s states=%s expectedResultFloor=%d results=%d "
                + "oldMs=%s newMs=%s savedMs=%s speedup=%sx",
            scenario.scenarioNo(),
            scenario.taskId(),
            scenario.requestContext(),
            scenario.statesLabel(),
            scenario.expectedResultFloor(),
            comparison.resultCount(),
            formatMillis(comparison.oldSearchDurationNanos()),
            formatMillis(comparison.newSearchDurationNanos()),
            formatMillis(comparison.oldSearchMinusNewSearchNanos()),
            formatDecimal(comparison.searchSpeedupRatio())
        );
    }

    private String formatGenericAllWorkComparison(GenericAllWorkComparison comparison) {
        SearchScenario scenario = comparison.scenario();
        return String.format(
            Locale.ROOT,
            "#%d taskId=%s roleSignatures=%s total=%d searchMs=%s countMs=%s",
            scenario.scenarioNo(),
            scenario.taskId(),
            scenario.roleSignatures(),
            comparison.totalCount(),
            formatMillis(comparison.searchDurationNanos()),
            formatMillis(comparison.countDurationNanos())
        );
    }

    private String formatMillis(long durationNanos) {
        return formatMillis((double) durationNanos);
    }

    private String formatMillis(double durationNanos) {
        return formatDecimal(durationNanos / NANOS_PER_MILLISECOND);
    }

    private String formatDecimal(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    private record TimedValue<T>(T value, long durationNanos) {
    }

    private record ScenarioComparison(SearchScenario scenario,
                                      TimedValue<List<String>> oldResult,
                                      TimedValue<List<String>> newResult,
                                      TimedValue<Long> oldCount,
                                      TimedValue<Long> newCount) {

        private int resultCount() {
            return newResult.value().size();
        }

        private long oldSearchDurationNanos() {
            return oldResult.durationNanos();
        }

        private long newSearchDurationNanos() {
            return newResult.durationNanos();
        }

        private long oldCountDurationNanos() {
            return oldCount.durationNanos();
        }

        private long newCountDurationNanos() {
            return newCount.durationNanos();
        }

        private long oldSearchMinusNewSearchNanos() {
            return oldSearchDurationNanos() - newSearchDurationNanos();
        }

        private long newSearchMinusOldSearchNanos() {
            return newSearchDurationNanos() - oldSearchDurationNanos();
        }

        private double searchSpeedupRatio() {
            return oldSearchDurationNanos() / (double) newSearchDurationNanos();
        }
    }

    private record GenericAllWorkComparison(SearchScenario scenario,
                                            TimedValue<List<String>> searchResult,
                                            TimedValue<Long> countResult) {

        private long searchDurationNanos() {
            return searchResult.durationNanos();
        }

        private long countDurationNanos() {
            return countResult.durationNanos();
        }

        private long totalCount() {
            return countResult.value();
        }
    }

    private record TimingSummary(String implementation,
                                 int count,
                                 long totalNanos,
                                 long minNanos,
                                 long p50Nanos,
                                 long p90Nanos,
                                 long p95Nanos,
                                 long p99Nanos,
                                 long maxNanos,
                                 double averageNanos,
                                 double standardDeviationNanos) {
    }

    private record SearchScenario(int scenarioNo,
                                  String taskId,
                                  List<CFTTaskState> states,
                                  List<String> caseIds,
                                  String assignee,
                                  RequestContext requestContext,
                                  int expectedResultFloor,
                                  Set<String> filterSignatures,
                                  Set<String> roleSignatures) {

        private SearchRequest toSearchRequest() {
            SearchFilterCriteria filterCriteria = SearchFilterCriteria.from(filterSignatures);
            SearchRequest.SearchRequestBuilder builder = SearchRequest.builder()
                .jurisdictions(filterCriteria.jurisdictions())
                .roleCategories(filterCriteria.roleCategories())
                .workTypes(filterCriteria.workTypes())
                .regions(filterCriteria.regions())
                .locations(filterCriteria.locations());

            if (!states.isEmpty()) {
                builder.cftTaskStates(states);
            }

            if (!caseIds.isEmpty()) {
                builder.caseIds(caseIds);
            }

            if (requestContext != null) {
                builder.requestContext(requestContext);
            }

            if (assignee != null && !assignee.isBlank()) {
                builder.users(List.of(assignee));
            }

            return builder.build();
        }

        private String statesLabel() {
            return states.isEmpty() ? "DEFAULT_ACTIVE" : states.toString();
        }

        private List<TaskSearchRoleCriteria> toRoleCriteria() {
            return roleSignatures.stream()
                .map(SearchScenario::toRoleCriteria)
                .toList();
        }

        private static TaskSearchRoleCriteria toRoleCriteria(String roleSignature) {
            String[] parts = roleSignature.split(":", 8);
            if (parts.length != 8) {
                throw new IllegalArgumentException("Invalid role signature: " + roleSignature);
            }

            return new TaskSearchRoleCriteria(
                nullIfWildcard(parts[0]),
                nullIfWildcard(parts[1]),
                nullIfWildcard(parts[2]),
                parts[3],
                nullIfWildcard(parts[4]),
                parts[5],
                parts[6],
                nullIfWildcard(parts[7])
            );
        }

        private static String nullIfWildcard(String value) {
            return "*".equals(value) ? null : value;
        }
    }

    private record SearchFilterCriteria(List<String> jurisdictions,
                                        List<RoleCategory> roleCategories,
                                        List<String> workTypes,
                                        List<String> regions,
                                        List<String> locations) {

        private static SearchFilterCriteria from(Set<String> filterSignatures) {
            Set<String> jurisdictions = new LinkedHashSet<>();
            Set<RoleCategory> roleCategories = new LinkedHashSet<>();
            Set<String> workTypes = new LinkedHashSet<>();
            Set<String> regions = new LinkedHashSet<>();
            Set<String> locations = new LinkedHashSet<>();

            for (String filterSignature : filterSignatures) {
                String[] parts = filterSignature.split(":", -1);
                if (parts.length != 6) {
                    throw new IllegalArgumentException("Invalid filter signature: " + filterSignature);
                }

                addIfConstrained(jurisdictions, parts[1]);
                if (!"*".equals(parts[2])) {
                    roleCategories.add(expandRoleCategory(parts[2]));
                }
                addIfConstrained(workTypes, parts[3]);
                addIfConstrained(regions, parts[4]);
                addIfConstrained(locations, parts[5]);
            }

            return new SearchFilterCriteria(
                List.copyOf(jurisdictions),
                List.copyOf(roleCategories),
                List.copyOf(workTypes),
                List.copyOf(regions),
                List.copyOf(locations)
            );
        }

        private static void addIfConstrained(Set<String> values, String value) {
            if (!"*".equals(value)) {
                values.add(value);
            }
        }

        private static RoleCategory expandRoleCategory(String value) {
            return switch (value) {
                case "J" -> RoleCategory.JUDICIAL;
                case "L" -> RoleCategory.LEGAL_OPERATIONS;
                case "A" -> RoleCategory.ADMIN;
                case "C" -> RoleCategory.CTSC;
                case "E" -> RoleCategory.ENFORCEMENT;
                default -> throw new IllegalArgumentException("Invalid filter role category: " + value);
            };
        }
    }
}
