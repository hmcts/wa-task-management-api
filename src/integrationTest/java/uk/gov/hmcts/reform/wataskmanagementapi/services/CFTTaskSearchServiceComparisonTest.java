package uk.gov.hmcts.reform.wataskmanagementapi.services;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.ActorIdType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleCategory;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.config.IntegrationTest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.RequestContext;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SearchRequest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.LongSummaryStatistics;
import java.util.Map;
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
class CFTTaskSearchServiceComparisonTest {

    private static final String SEARCH_REQUESTS_SQL =
        "scripts/search-index-comparison/real_search_scenarios-levelup.sql";
    private static final int REQUIRED_SCENARIOS = 1;
    private static final long COUNT_LIMIT = 10_001L;
    private static final double NANOS_PER_SECOND = 1_000_000_000.0;

    @Autowired
    private CFTTaskSearchService cftTaskSearchService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void should_execute_role_criteria_search_and_report_statistics() {
        List<SearchExecutionScenario> scenarios = loadSearchExecutionScenarios();
        List<Long> durationsNanos = new ArrayList<>(scenarios.size());

        assertThat(scenarios)
            .as("search request SQL must contain at least %s scenario", REQUIRED_SCENARIOS)
            .hasSizeGreaterThanOrEqualTo(REQUIRED_SCENARIOS);

        for (SearchExecutionScenario scenario : scenarios) {
            assertThat(scenario.roleAssignments())
                .as("scenario %s must contain at least one role assignment", scenario.description())
                .isNotEmpty();

            TimedValue<CFTTaskSearchService.SearchResult> roleCriteriaResult = time(
                () -> cftTaskSearchService.searchUsingRoleCriteria(
                    scenario.firstResult(),
                    scenario.maxResults(),
                    scenario.searchRequest(),
                    scenario.roleAssignments(),
                    List.of()
                )
            );
            durationsNanos.add(roleCriteriaResult.durationNanos());

            assertThat(roleCriteriaResult.value().totalRecords())
                .as("scenario %s should not exceed the hard count limit", scenario.description())
                .isLessThanOrEqualTo(COUNT_LIMIT);

            log.info(
                "CFTTaskSearchService role criteria result: scenarioNo={}, description={}, "
                    + "searchRequest={}, firstResult={}, maxResults={}, roleAssignmentSetId={}, "
                    + "roleAssignmentCount={}, returnedTaskCount={}, count={}, "
                    + "roleCriteriaSeconds={}",
                scenario.scenarioNo(),
                scenario.description(),
                scenario.searchRequest(),
                scenario.firstResult(),
                scenario.maxResults(),
                scenario.roleAssignmentSetId(),
                scenario.roleAssignments().size(),
                roleCriteriaResult.value().taskIds().size(),
                roleCriteriaResult.value().totalRecords(),
                formatSeconds(roleCriteriaResult.durationNanos())
            );
        }

        LongSummaryStatistics statistics = durationsNanos.stream()
            .mapToLong(Long::longValue)
            .summaryStatistics();
        log.info(
            "CFTTaskSearchService role criteria summary: scenarios={}, totalSeconds={}, averageSeconds={}, "
                + "minSeconds={}, maxSeconds={}",
            statistics.getCount(),
            formatSeconds(statistics.getSum()),
            formatSeconds(statistics.getAverage()),
            formatSeconds(statistics.getMin()),
            formatSeconds(statistics.getMax())
        );
    }

    private List<SearchExecutionScenario> loadSearchExecutionScenarios() {
        return jdbcTemplate.query(
            loadSearchRequestsSql(),
            (rs, rowNum) -> new SearchExecutionScenario(
                rs.getInt("scenario_no"),
                rs.getString("description"),
                SearchRequest.builder()
                    .cftTaskStates(getStateList(rs, "states"))
                    .jurisdictions(getNullableStringList(rs, "jurisdictions"))
                    .locations(getNullableStringList(rs, "locations"))
                    .regions(getNullableStringList(rs, "regions"))
                    .caseIds(getNullableStringList(rs, "case_ids"))
                    .users(getNullableStringList(rs, "users"))
                    .taskTypes(getNullableStringList(rs, "task_types"))
                    .workTypes(getNullableStringList(rs, "work_types"))
                    .roleCategories(getRoleCategories(rs, "role_categories"))
                    .requestContext(RequestContext.valueOf(rs.getString("request_context")))
                    .sortingParameters(List.of())
                    .build(),
                rs.getInt("role_assignment_set_id"),
                parseRoleAssignments(rs.getString("role_assignments")),
                rs.getInt("first_result"),
                rs.getInt("max_results")
            )
        );
    }

    private String loadSearchRequestsSql() {
        try {
            return new ClassPathResource(SEARCH_REQUESTS_SQL)
                .getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load " + SEARCH_REQUESTS_SQL, exception);
        }
    }

    private List<String> getNullableStringList(ResultSet resultSet, String columnName) throws SQLException {
        if (resultSet.getArray(columnName) == null) {
            return null;
        }
        return Arrays.asList((String[]) resultSet.getArray(columnName).getArray());
    }

    private List<CFTTaskState> getStateList(ResultSet resultSet, String columnName) throws SQLException {
        return getNullableStringList(resultSet, columnName).stream()
            .map(state -> CFTTaskState.from(state).orElseThrow())
            .toList();
    }

    private List<RoleCategory> getRoleCategories(ResultSet resultSet, String columnName) throws SQLException {
        List<String> values = getNullableStringList(resultSet, columnName);
        if (values == null) {
            return null;
        }
        return values.stream()
            .map(RoleCategory::valueOf)
            .toList();
    }

    private <T> TimedValue<T> time(Supplier<T> supplier) {
        long started = System.nanoTime();
        T value = supplier.get();
        return new TimedValue<>(value, System.nanoTime() - started);
    }

    private Map<String, String> toFieldMap(String body) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (String part : splitTopLevel(body, ", ")) {
            int separatorIndex = part.indexOf('=');
            if (separatorIndex < 0) {
                throw new IllegalArgumentException("Invalid field: " + part);
            }
            fields.put(part.substring(0, separatorIndex), part.substring(separatorIndex + 1));
        }
        return fields;
    }

    private List<String> splitTopLevel(String value, String delimiter) {
        List<String> parts = new ArrayList<>();
        int depthParentheses = 0;
        int depthBrackets = 0;
        int depthBraces = 0;
        int tokenStart = 0;

        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (current == '(') {
                depthParentheses++;
            } else if (current == ')') {
                depthParentheses--;
            } else if (current == '[') {
                depthBrackets++;
            } else if (current == ']') {
                depthBrackets--;
            } else if (current == '{') {
                depthBraces++;
            } else if (current == '}') {
                depthBraces--;
            }

            if (depthParentheses == 0
                && depthBrackets == 0
                && depthBraces == 0
                && value.startsWith(delimiter, index)) {
                parts.add(value.substring(tokenStart, index));
                tokenStart = index + delimiter.length();
                index += delimiter.length() - 1;
            }
        }

        parts.add(value.substring(tokenStart));
        return parts;
    }

    private List<String> parseStringList(String value) {
        String trimmed = nullIfLiteralNull(value);
        if (trimmed == null) {
            return null;
        }
        if ("[]".equals(trimmed)) {
            return List.of();
        }
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
            throw new IllegalArgumentException("Invalid list value: " + value);
        }

        String body = trimmed.substring(1, trimmed.length() - 1);
        if (body.isEmpty()) {
            return List.of();
        }
        return splitTopLevel(body, ", ");
    }

    private List<RoleAssignment> parseRoleAssignments(String serializedRoleAssignments) {
        String trimmed = serializedRoleAssignments.trim();
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
            throw new IllegalArgumentException("Invalid role assignment list format");
        }

        String body = trimmed.substring(1, trimmed.length() - 1).trim();
        if (body.isEmpty()) {
            return List.of();
        }

        List<String> items = splitTopLevel(body, ", ");
        return items.stream()
            .map(this::parseRoleAssignment)
            .toList();
    }

    private RoleAssignment parseRoleAssignment(String item) {
        if (!item.startsWith("RoleAssignment(") || !item.endsWith(")")) {
            throw new IllegalArgumentException("Invalid role assignment entry: " + item);
        }

        String body = item.substring("RoleAssignment(".length(), item.length() - 1);
        Map<String, String> fields = toFieldMap(body);

        return RoleAssignment.builder()
            .id(nullIfLiteralNull(fields.get("id")))
            .actorIdType(enumValue(ActorIdType.class, fields.get("actorIdType")))
            .actorId(nullIfLiteralNull(fields.get("actorId")))
            .roleType(enumValue(RoleType.class, fields.get("roleType")))
            .roleName(nullIfLiteralNull(fields.get("roleName")))
            .classification(enumValue(Classification.class, fields.get("classification")))
            .grantType(enumValue(GrantType.class, fields.get("grantType")))
            .roleCategory(enumValue(RoleCategory.class, fields.get("roleCategory")))
            .readOnly(Boolean.parseBoolean(fields.getOrDefault("readOnly", "false")))
            .beginTime(parseOffsetDateTime(fields.get("beginTime")))
            .endTime(parseOffsetDateTime(fields.get("endTime")))
            .created(parseOffsetDateTime(fields.get("created")))
            .attributes(parseAttributes(fields.get("attributes")))
            .authorisations(parseStringList(fields.get("authorisations")))
            .build();
    }

    private Map<String, String> parseAttributes(String value) {
        String trimmed = nullIfLiteralNull(value);
        if (trimmed == null) {
            return null;
        }
        if ("{}".equals(trimmed)) {
            return Map.of();
        }
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
            throw new IllegalArgumentException("Invalid attributes: " + value);
        }

        Map<String, String> attributes = new LinkedHashMap<>();
        String body = trimmed.substring(1, trimmed.length() - 1);
        String currentKey = null;
        for (String attribute : splitTopLevel(body, ", ")) {
            int separatorIndex = attribute.indexOf('=');
            if (separatorIndex < 0) {
                if (currentKey == null) {
                    throw new IllegalArgumentException("Invalid attribute: " + attribute);
                }
                attributes.put(currentKey, attributes.get(currentKey) + ", " + attribute);
                continue;
            }
            currentKey = attribute.substring(0, separatorIndex);
            attributes.put(currentKey, attribute.substring(separatorIndex + 1));
        }
        return attributes;
    }

    private OffsetDateTime parseOffsetDateTime(String value) {
        String trimmed = nullIfLiteralNull(value);
        return trimmed == null ? null : OffsetDateTime.parse(trimmed);
    }

    private <T extends Enum<T>> T enumValue(Class<T> type, String value) {
        String trimmed = nullIfLiteralNull(value);
        return trimmed == null ? null : Enum.valueOf(type, trimmed);
    }

    private String nullIfLiteralNull(String value) {
        return value == null || "null".equals(value) ? null : value;
    }

    private String formatSeconds(long durationNanos) {
        return formatSeconds((double) durationNanos);
    }

    private String formatSeconds(double durationNanos) {
        return formatDecimal(durationNanos / NANOS_PER_SECOND);
    }

    private String formatDecimal(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    private record SearchExecutionScenario(int scenarioNo,
                                           String description,
                                           SearchRequest searchRequest,
                                           int roleAssignmentSetId,
                                           List<RoleAssignment> roleAssignments,
                                           int firstResult,
                                           int maxResults) {
    }

    private record TimedValue<T>(T value, long durationNanos) {
    }
}
