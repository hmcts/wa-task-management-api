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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;

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
    private static final int MAX_RESULTS = 25;
    private static final double NANOS_PER_SECOND = 1_000_000_000.0;

    @Autowired
    private TaskResourceRepository taskResourceRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void should_compare_old_and_new_searches_for_real_world_scenarios() {
        List<SearchScenario> scenarios = loadRealWorldSearchScenarios();

        assertThat(scenarios)
            .as("search scenario script must provide at least one scenario")
            .isNotEmpty();

        List<Long> oldSearchDurations = new ArrayList<>(scenarios.size());
        List<Long> newSearchDurations = new ArrayList<>(scenarios.size());
        List<Long> oldCountDurations = new ArrayList<>(scenarios.size());
        List<Long> newCountDurations = new ArrayList<>(scenarios.size());

        for (SearchScenario scenario : scenarios) {
            boolean oldFirst = scenario.number() % 2 == 0;
            TimedComparison<List<String>> search = timeBoth(
                () -> searchOld(scenario),
                () -> searchNew(scenario),
                oldFirst
            );

            assertThat(search.newResult().value())
                .as("scenario %s taskId=%s should return the same task IDs in the same order",
                    scenario.number(), scenario.taskId())
                .isEqualTo(search.oldResult().value());
            assertThat(search.oldResult().value())
                .as("scenario %s taskId=%s should return more than the expected result floor",
                    scenario.number(), scenario.taskId())
                .hasSizeGreaterThan(scenario.expectedResultFloor());

            TimedComparison<Long> count = timeBoth(
                () -> countOld(scenario),
                () -> countNew(scenario),
                oldFirst
            );

            assertThat(count.newResult().value())
                .as("scenario %s taskId=%s should return the same count", scenario.number(), scenario.taskId())
                .isEqualTo(count.oldResult().value());
            assertThat(count.oldResult().value())
                .as("scenario %s taskId=%s count should exceed the expected result floor",
                    scenario.number(), scenario.taskId())
                .isGreaterThan(scenario.expectedResultFloor());

            oldSearchDurations.add(search.oldResult().durationNanos());
            newSearchDurations.add(search.newResult().durationNanos());
            oldCountDurations.add(count.oldResult().durationNanos());
            newCountDurations.add(count.newResult().durationNanos());

            logScenarioPerformance(
                "Task ID search",
                scenario,
                search.oldResult().durationNanos(),
                search.newResult().durationNanos()
            );
            logScenarioPerformance(
                "Count",
                scenario,
                count.oldResult().durationNanos(),
                count.newResult().durationNanos()
            );
        }

        logAveragePerformance("Task ID search", oldSearchDurations, newSearchDurations);
        logAveragePerformance("Count", oldCountDurations, newCountDurations);
    }

    private List<SearchScenario> loadRealWorldSearchScenarios() {
        return jdbcTemplate.query(
            loadSearchScenariosSql(),
            (rs, rowNum) -> new SearchScenario(
                rs.getInt("scenario_no"),
                rs.getString("task_id"),
                getStateList(rs, "states"),
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

    private List<CFTTaskState> getStateList(ResultSet resultSet, String columnName) throws SQLException {
        return getStringList(resultSet, columnName).stream()
            .map(state -> CFTTaskState.from(state).orElseThrow())
            .toList();
    }

    private Set<String> getStringSet(ResultSet resultSet, String columnName) throws SQLException {
        return new LinkedHashSet<>(Arrays.asList((String[]) resultSet.getArray(columnName).getArray()));
    }

    private String loadSearchScenariosSql() {
        try {
            return new ClassPathResource(SEARCH_SCENARIOS_SQL)
                .getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load search scenarios", exception);
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

    private <T> TimedValue<T> time(Supplier<T> operation) {
        long started = System.nanoTime();
        return new TimedValue<>(operation.get(), System.nanoTime() - started);
    }

    private <T> TimedComparison<T> timeBoth(Supplier<T> oldOperation,
                                            Supplier<T> newOperation,
                                            boolean oldFirst) {
        if (oldFirst) {
            return new TimedComparison<>(time(oldOperation), time(newOperation));
        }
        TimedValue<T> newResult = time(newOperation);
        return new TimedComparison<>(time(oldOperation), newResult);
    }

    private void logScenarioPerformance(String operation,
                                        SearchScenario scenario,
                                        long oldDurationNanos,
                                        long newDurationNanos) {
        log.info(
            "{} scenario={} taskId={}: oldSeconds={}, newSeconds={}, speedup={}, speedupPercent={}",
            operation,
            scenario.number(),
            scenario.taskId(),
            formatSeconds(oldDurationNanos),
            formatSeconds(newDurationNanos),
            formatSpeedup(speedupRatio(oldDurationNanos, newDurationNanos)),
            formatPercent(speedupPercent(oldDurationNanos, newDurationNanos))
        );
    }

    private void logAveragePerformance(String operation, List<Long> oldDurations, List<Long> newDurations) {
        double oldAverageNanos = oldDurations.stream().mapToLong(Long::longValue).average().orElse(0);
        double newAverageNanos = newDurations.stream().mapToLong(Long::longValue).average().orElse(0);

        log.info(
            "{} average: scenarios={}, oldAverageSeconds={}, newAverageSeconds={}, "
                + "speedup={}, speedupPercent={}",
            operation,
            oldDurations.size(),
            formatSeconds(oldAverageNanos),
            formatSeconds(newAverageNanos),
            formatSpeedup(speedupRatio(oldAverageNanos, newAverageNanos)),
            formatPercent(speedupPercent(oldAverageNanos, newAverageNanos))
        );
    }

    private double speedupRatio(double oldDurationNanos, double newDurationNanos) {
        return newDurationNanos == 0
            ? 0
            : oldDurationNanos / newDurationNanos;
    }

    private double speedupPercent(double oldDurationNanos, double newDurationNanos) {
        return (speedupRatio(oldDurationNanos, newDurationNanos) - 1.0) * 100.0;
    }

    private String formatSeconds(double nanos) {
        return formatDecimal(nanos / NANOS_PER_SECOND);
    }

    private String formatPercent(double value) {
        return formatDecimal(value) + "%";
    }

    private String formatSpeedup(double value) {
        return formatDecimal(value) + "x";
    }

    private String formatDecimal(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    private record TimedValue<T>(T value, long durationNanos) {
    }

    private record TimedComparison<T>(TimedValue<T> oldResult, TimedValue<T> newResult) {
    }

    private record SearchScenario(int number,
                                  String taskId,
                                  List<CFTTaskState> states,
                                  List<String> caseIds,
                                  String assignee,
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
                .locations(filterCriteria.locations())
                .requestContext(requestContext());

            if (!states.isEmpty()) {
                builder.cftTaskStates(states);
            }

            if (!caseIds.isEmpty()) {
                builder.caseIds(caseIds);
            }

            if (assignee != null && !assignee.isBlank()) {
                builder.users(List.of(assignee));
            }

            return builder.build();
        }

        private List<TaskSearchRoleCriteria> toRoleCriteria() {
            return roleSignatures.stream()
                .map(SearchScenario::toRoleCriteria)
                .toList();
        }

        private static TaskSearchRoleCriteria toRoleCriteria(String roleSignature) {
            String[] parts = roleSignatureParts(roleSignature);

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

        private RequestContext requestContext() {
            List<String> permissions = roleSignatures.stream()
                .map(SearchScenario::roleSignatureParts)
                .map(parts -> parts[5])
                .distinct()
                .toList();

            if (permissions.size() != 1) {
                throw new IllegalArgumentException(
                    "Scenario " + number + " must contain exactly one role permission: " + permissions
                );
            }

            return switch (permissions.get(0)) {
                case "a" -> RequestContext.AVAILABLE_TASKS;
                case "m" -> RequestContext.ALL_WORK;
                case "r" -> null;
                default -> throw new IllegalArgumentException(
                    "Invalid role permission in scenario " + number + ": " + permissions.get(0)
                );
            };
        }

        private static String[] roleSignatureParts(String roleSignature) {
            String[] parts = roleSignature.split(":", 8);
            if (parts.length != 8) {
                throw new IllegalArgumentException("Invalid role signature: " + roleSignature);
            }
            return parts;
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
