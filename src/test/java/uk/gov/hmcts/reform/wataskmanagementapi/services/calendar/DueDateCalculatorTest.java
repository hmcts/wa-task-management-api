package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.ConfigurationDmnEvaluationResponse;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateType.DUE_DATE;

@ExtendWith(MockitoExtension.class)
class DueDateCalculatorTest {

    public static final LocalDateTime GIVEN_DATE = LocalDateTime.of(2022, 10, 13, 18, 0, 0);
    public static final DateTypeConfigurator.DateTypeObject DUE_DATE_TYPE = new DateTypeConfigurator.DateTypeObject(
        DUE_DATE,
        DUE_DATE.getType()
    );
    private DueDateCalculator dueDateCalculator;

    public static Stream<ConfigurableScenario> getPossibleDates() {
        return Stream.of(
            new ConfigurableScenario(
                LocalDateTime.of(2023, 10, 12, 16, 12, 13).format(DateTimeFormatter.ISO_DATE_TIME),
                LocalDateTime.of(2023, 10, 12, 16, 12).format(DateCalculator.DATE_TIME_FORMATTER)
            ),
            new ConfigurableScenario(
                LocalDateTime.of(2023, 10, 12, 16, 12, 13, 123)
                    .format(DateTimeFormatter.ISO_DATE_TIME),
                LocalDateTime.of(2023, 10, 12, 16, 12).format(DateCalculator.DATE_TIME_FORMATTER)
            ),
            new ConfigurableScenario(
                LocalDateTime.of(2023, 10, 12, 16, 12).format(DateTimeFormatter.ISO_DATE_TIME),
                LocalDateTime.of(2023, 10, 12, 16, 12).format(DateCalculator.DATE_TIME_FORMATTER)
            ),
            new ConfigurableScenario(
                LocalDateTime.of(2023, 10, 12, 16, 0, 0).format(DateTimeFormatter.ISO_DATE_TIME),
                LocalDateTime.of(2023, 10, 12, 16, 0, 0).format(DateCalculator.DATE_TIME_FORMATTER)
            ),
            new ConfigurableScenario(
                ZonedDateTime.of(2023, 10, 12, 16, 12, 16, 123, ZoneId.systemDefault())
                    .format(DateTimeFormatter.ISO_DATE_TIME),
                LocalDateTime.of(2023, 10, 12, 16, 12, 0)
                    .format(DateCalculator.DATE_TIME_FORMATTER)
            )
        );
    }

    @BeforeEach
    void before() {
        dueDateCalculator = new DueDateCalculator();
    }

    @ParameterizedTest
    @CsvSource({
        "true", "false"
    })
    void should_not_supports_when_responses_contains_due_date_origin_and_time(boolean configurable) {
        String expectedDueDate = GIVEN_DATE.plusDays(0)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse dueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOrigin"))
            .value(CamundaValue.stringValue(expectedDueDate + "T16:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        ConfigurationDmnEvaluationResponse dueDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateTime"))
            .value(CamundaValue.stringValue("16:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(dueDate, dueDateTime);

        assertThat(dueDateCalculator.supports(evaluationResponses, DUE_DATE_TYPE, configurable)).isFalse();
    }

    @ParameterizedTest
    @CsvSource({
        "true", "false"
    })
    void should_not_supports_when_responses_contains_only_due_date_time(boolean configurable) {

        ConfigurationDmnEvaluationResponse dueDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateTime"))
            .value(CamundaValue.stringValue("16:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(dueDateTime);

        assertThat(dueDateCalculator.supports(evaluationResponses, DUE_DATE_TYPE, configurable)).isFalse();
    }

    @ParameterizedTest
    @CsvSource({
        "true", "false"
    })
    void should_supports_when_responses_only_contains_due_date_but_not_origin(boolean configurable) {
        String expectedDueDate = GIVEN_DATE.plusDays(0)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse dueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(expectedDueDate + "T16:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        ConfigurationDmnEvaluationResponse dueDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateTime"))
            .value(CamundaValue.stringValue("16:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(dueDate, dueDateTime);

        assertThat(dueDateCalculator.supports(evaluationResponses, DUE_DATE_TYPE, configurable)).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
        "true, T16:00", "false, T16:00"
    })
    void should_calculate_due_date_when_due_date_is_given(boolean configurable, String time) {

        String expectedDueDate = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse dueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(expectedDueDate + "T16:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(dueDate);

        String dateValue = dueDateCalculator.calculateDate(
            evaluationResponses,
            DUE_DATE_TYPE,
            configurable,
            new HashMap<>(),
            new ArrayList<>()
        ).getValue().getValue();
        assertThat(LocalDateTime.parse(dateValue)).isEqualTo(expectedDueDate + time);
    }

    @ParameterizedTest
    @MethodSource("getPossibleDates")
    void should_calculate_due_date_for_different_formats(ConfigurableScenario configurableScenario) {

        ConfigurationDmnEvaluationResponse dueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(configurableScenario.inputDate))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(dueDate);

        String dateValue = dueDateCalculator.calculateDate(
            evaluationResponses,
            DUE_DATE_TYPE,
            false,
            new HashMap<>(),
            new ArrayList<>()
        ).getValue().getValue();
        assertThat(dateValue).isEqualTo(configurableScenario.expectedDate);
    }

    @ParameterizedTest
    @CsvSource({
        "true, T20:00", "false, T20:00"
    })
    void should_consider_only_due_date_when_given_configurable_due_date_and_un_configurable_time(
        boolean configurable, String time) {

        String expectedDueDate = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse dueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(expectedDueDate + "T16:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        ConfigurationDmnEvaluationResponse dueDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateTime"))
            .value(CamundaValue.stringValue("20:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(dueDate, dueDateTime);

        String dateValue = dueDateCalculator.calculateDate(evaluationResponses, DUE_DATE_TYPE, configurable,
                new HashMap<>(),
                new ArrayList<>()
            )
            .getValue().getValue();
        assertThat(LocalDateTime.parse(dateValue)).isEqualTo(expectedDueDate + time);
    }

    @ParameterizedTest
    @CsvSource({
        "true, T20:00", "false, T20:00"
    })
    void should_calculate_due_date_when_due_date_and_time_are_given(boolean configurable, String time) {
        should_consider_only_due_date_when_given_configurable_due_date_and_un_configurable_time(configurable, time);
    }

    @ParameterizedTest
    @CsvSource({
        "true, T19:00", "false, T19:00"
    })
    void should_calculate_due_date_from_last_entry_when_multiple_time_is_given(boolean configurable, String time) {
        String expectedDueDate = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String expectedDueDate2 = GIVEN_DATE.plusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse dueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(expectedDueDate + "T16:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        ConfigurationDmnEvaluationResponse dueDate2 = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(expectedDueDate2 + "T19:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(dueDate, dueDate2);

        String dateValue = dueDateCalculator.calculateDate(evaluationResponses, DUE_DATE_TYPE, configurable,
                new HashMap<>(),
                new ArrayList<>()
            )
            .getValue().getValue();
        assertThat(LocalDateTime.parse(dateValue)).isEqualTo(expectedDueDate2 + time);
    }

    static class ConfigurableScenario {
        String inputDate;
        String expectedDate;

        public ConfigurableScenario(String inputDate, String expectedDate) {
            this.inputDate = inputDate;
            this.expectedDate = expectedDate;
        }
    }
}
