package uk.gov.hmcts.reform.wataskmanagementapi.repository;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.config.IntegrationTest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.RequestContext;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SearchRequest;

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
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest(properties = {
    "spring.datasource.driver-class-name=org.postgresql.Driver",
    "spring.datasource.driverClassName=org.postgresql.Driver",
    "spring.datasource.url=jdbc:postgresql://localhost:5432/cft_task_db",
    "spring.datasource.jdbcUrl=jdbc:postgresql://localhost:5432/cft_task_db",
    "spring.datasource.username=postgres",
    "spring.datasource.password=postgres",
    "spring.datasource.hikari.maximum-pool-size=2",
    "spring.flyway.enabled=false",
    "logging.level.uk.gov.hmcts.reform.wataskmanagementapi.repository.TaskResourceCustomRepositoryImpl=WARN"
})
@Slf4j
class TaskResourceSearchIndexComparisonTest {

    private static final String SEARCH_SCENARIOS_SQL =
        "scripts/search-index-comparison/real_search_scenarios.sql";
    private static final int REQUIRED_SCENARIOS = 500;
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

        for (int index = 0; index < scenarios.size(); index++) {
            SearchScenario scenario = scenarios.get(index);

            TimedResult oldSearchResult = time(() -> searchOld(scenario));
            TimedResult newSearchResult = time(() -> searchNew(scenario));

            comparisons.add(new ScenarioComparison(scenario, oldSearchResult, newSearchResult));

            assertThat(newSearchResult.taskIds())
                .as("scenario %s taskId=%s should match old search results", scenario.scenarioNo(), scenario.taskId())
                .containsExactlyElementsOf(oldSearchResult.taskIds());

            assertThat(newSearchResult.taskIds())
                .as("scenario %s should return more than %s tasks using filterSignatures=%s roleSignatures=%s",
                    scenario.scenarioNo(),
                    MINIMUM_RESULT_COUNT,
                    scenario.filterSignatures().size(),
                    scenario.roleSignatures().size())
                .hasSizeGreaterThan(MINIMUM_RESULT_COUNT);

            if (newSearchResult.taskIds().size() == MAX_RESULTS) {
                fullPageScenarios++;
                Long oldSearchCount = countOld(scenario);
                Long newSearchCount = countNew(scenario);

                assertThat(newSearchCount)
                    .as("scenario %s taskId=%s should match old search count",
                        scenario.scenarioNo(),
                        scenario.taskId())
                    .isEqualTo(oldSearchCount);
            }
        }

        assertThat(fullPageScenarios)
            .as("at least one scenario should return more than the %s task IDs visible on the first page", MAX_RESULTS)
            .isPositive();

        logPerformanceComparison(comparisons, fullPageScenarios);
    }

    private List<SearchScenario> loadRealWorldSearchScenarios() {
        return jdbcTemplate.query(
            loadSearchScenariosSql(),
            (rs, rowNum) -> new SearchScenario(
                rs.getInt("scenario_no"),
                rs.getString("task_id"),
                CFTTaskState.from(rs.getString("state")).orElseThrow(),
                getStringList(rs, "case_ids"),
                rs.getString("assignee"),
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
            scenario.filterSignatures(),
            scenario.roleSignatures(),
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
            scenario.filterSignatures(),
            scenario.roleSignatures(),
            List.of(),
            scenario.toSearchRequest()
        );
    }

    private TimedResult time(Supplier<List<String>> search) {
        long started = System.nanoTime();
        List<String> taskIds = search.get();
        return new TimedResult(taskIds, System.nanoTime() - started);
    }

    private void logPerformanceComparison(List<ScenarioComparison> comparisons, int fullPageScenarios) {
        TimingSummary oldSummary = summarize(
            "old_search_index",
            comparisons.stream().map(ScenarioComparison::oldDurationNanos).toList()
        );
        TimingSummary newSummary = summarize(
            "new_signature_search",
            comparisons.stream().map(ScenarioComparison::newDurationNanos).toList()
        );

        int partialPageScenarios = comparisons.size() - fullPageScenarios;
        int minResultCount = comparisons.stream().mapToInt(ScenarioComparison::resultCount).min().orElse(0);
        int maxResultCount = comparisons.stream().mapToInt(ScenarioComparison::resultCount).max().orElse(0);
        double averageResultCount = comparisons.stream().mapToInt(ScenarioComparison::resultCount).average().orElse(0);
        long newFasterScenarios = comparisons.stream()
            .filter(comparison -> comparison.newDurationNanos() < comparison.oldDurationNanos())
            .count();
        long oldFasterScenarios = comparisons.stream()
            .filter(comparison -> comparison.oldDurationNanos() < comparison.newDurationNanos())
            .count();
        long equalTimingScenarios = comparisons.size() - newFasterScenarios - oldFasterScenarios;
        long totalSavedNanos = oldSummary.totalNanos() - newSummary.totalNanos();
        double averageSpeedupRatio = comparisons.stream()
            .mapToDouble(ScenarioComparison::speedupRatio)
            .average()
            .orElse(0);

        log.info(
            "Search index comparison results: scenarios={}, noCaseIdScenarios={}, fullPageScenarios={}, "
                + "partialPageScenarios={}, resultCountMin={}, resultCountAvg={}, resultCountMax={}",
            comparisons.size(),
            comparisons.size(),
            fullPageScenarios,
            partialPageScenarios,
            minResultCount,
            formatDecimal(averageResultCount),
            maxResultCount
        );
        logTimingSummary(oldSummary);
        logTimingSummary(newSummary);
        log.info(
            "Search index relative performance: newFasterScenarios={}, oldFasterScenarios={}, "
                + "equalTimingScenarios={}, totalSavedMs={}, averageSavedMs={}, averageSpeedupRatio={}x",
            newFasterScenarios,
            oldFasterScenarios,
            equalTimingScenarios,
            formatMillis(totalSavedNanos),
            formatMillis(totalSavedNanos / (double) comparisons.size()),
            formatDecimal(averageSpeedupRatio)
        );

        logScenarios(
            "Search index slowest old_search_index scenarios",
            comparisons.stream()
                .sorted(Comparator.comparingLong(ScenarioComparison::oldDurationNanos).reversed())
                .limit(TOP_SCENARIO_LOG_LIMIT)
                .toList()
        );
        logScenarios(
            "Search index fastest old_search_index scenarios",
            comparisons.stream()
                .sorted(Comparator.comparingLong(ScenarioComparison::oldDurationNanos))
                .limit(TOP_SCENARIO_LOG_LIMIT)
                .toList()
        );
        logScenarios(
            "Search index slowest new_signature_search scenarios",
            comparisons.stream()
                .sorted(Comparator.comparingLong(ScenarioComparison::newDurationNanos).reversed())
                .limit(TOP_SCENARIO_LOG_LIMIT)
                .toList()
        );
        logScenarios(
            "Search index fastest new_signature_search scenarios",
            comparisons.stream()
                .sorted(Comparator.comparingLong(ScenarioComparison::newDurationNanos))
                .limit(TOP_SCENARIO_LOG_LIMIT)
                .toList()
        );
        logScenarios(
            "Search index largest new_signature_search improvements",
            comparisons.stream()
                .filter(comparison -> comparison.oldDurationNanos() > comparison.newDurationNanos())
                .sorted(Comparator.comparingLong(ScenarioComparison::oldMinusNewNanos).reversed())
                .limit(TOP_SCENARIO_LOG_LIMIT)
                .toList()
        );
        logScenarios(
            "Search index largest new_signature_search regressions",
            comparisons.stream()
                .filter(comparison -> comparison.newDurationNanos() > comparison.oldDurationNanos())
                .sorted(Comparator.comparingLong(ScenarioComparison::newMinusOldNanos).reversed())
                .limit(TOP_SCENARIO_LOG_LIMIT)
                .toList()
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
            "#%d taskId=%s state=%s expectedResultFloor=%d results=%d oldMs=%s newMs=%s savedMs=%s speedup=%sx",
            scenario.scenarioNo(),
            scenario.taskId(),
            scenario.state(),
            scenario.expectedResultFloor(),
            comparison.resultCount(),
            formatMillis(comparison.oldDurationNanos()),
            formatMillis(comparison.newDurationNanos()),
            formatMillis(comparison.oldMinusNewNanos()),
            formatDecimal(comparison.speedupRatio())
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

    private record TimedResult(List<String> taskIds, long durationNanos) {
    }

    private record ScenarioComparison(SearchScenario scenario,
                                      TimedResult oldResult,
                                      TimedResult newResult) {

        private int resultCount() {
            return newResult.taskIds().size();
        }

        private long oldDurationNanos() {
            return oldResult.durationNanos();
        }

        private long newDurationNanos() {
            return newResult.durationNanos();
        }

        private long oldMinusNewNanos() {
            return oldDurationNanos() - newDurationNanos();
        }

        private long newMinusOldNanos() {
            return newDurationNanos() - oldDurationNanos();
        }

        private double speedupRatio() {
            return oldDurationNanos() / (double) newDurationNanos();
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
                                  CFTTaskState state,
                                  List<String> caseIds,
                                  String assignee,
                                  int expectedResultFloor,
                                  Set<String> filterSignatures,
                                  Set<String> roleSignatures) {

        private SearchRequest toSearchRequest() {
            SearchRequest.SearchRequestBuilder builder = SearchRequest.builder()
                .cftTaskStates(List.of(state));

            if (!caseIds.isEmpty()) {
                builder.caseIds(caseIds);
            }

            if (assignee == null || assignee.isBlank()) {
                builder.requestContext(RequestContext.AVAILABLE_TASKS);
            } else {
                builder.users(List.of(assignee));
            }

            return builder.build();
        }
    }
}
