package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateTypeConfigurator.DateTypeObject;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateType.NEXT_HEARING_DATE;

@ExtendWith(MockitoExtension.class)
class NextHearingDateCalculatorTest {

    public static final LocalDateTime GIVEN_DATE = LocalDateTime.of(2022, 10, 13, 18, 0, 0);
    public static final DateTypeObject NEXT_HEARING_DATE_TYPE = new DateTypeObject(
        NEXT_HEARING_DATE,
        NEXT_HEARING_DATE.getType()
    );

    private NextHearingDateCalculator nextHearingDateCalculator;

    @ParameterizedTest
    @CsvSource({
        "true", "false"
    })
    void should_not_supports_when_responses_contains_next_hearing_date__origin_and_time(boolean configurable) {
        String expectedNextHearingDate = GIVEN_DATE.plusDays(0)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        var nextHearingDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateOrigin"))
            .value(CamundaValue.stringValue(expectedNextHearingDate + "T16:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        var nextHearingDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateTime"))
            .value(CamundaValue.stringValue("16:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses
            = List.of(nextHearingDateOrigin, nextHearingDateTime);

        assertThat(nextHearingDateCalculator.supports(
            evaluationResponses, NEXT_HEARING_DATE_TYPE, configurable)).isFalse();
    }

    @ParameterizedTest
    @CsvSource({
        "true", "false"
    })
    void should_not_supports_when_responses_contains_only_next_hearing_date__time(boolean configurable) {

        var nextHearingDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateTime"))
            .value(CamundaValue.stringValue("16:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(nextHearingDateTime);

        assertThat(nextHearingDateCalculator.supports(
            evaluationResponses, NEXT_HEARING_DATE_TYPE, configurable)).isFalse();
    }

    @ParameterizedTest
    @CsvSource({
        "true", "false"
    })
    void should_supports_when_responses_only_contains_next_hearing_date__but_not_origin(boolean configurable) {
        String expectedNextHearingDate = GIVEN_DATE.plusDays(0)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        var nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(expectedNextHearingDate + "T16:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        var nextHearingDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateTime"))
            .value(CamundaValue.stringValue("16:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(nextHearingDate, nextHearingDateTime);

        assertThat(nextHearingDateCalculator.supports(
            evaluationResponses, NEXT_HEARING_DATE_TYPE, configurable)).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
        "true, T16:00", "false, T16:00"
    })
    void should_calculate_next_hearing_date__when_next_hearing_date__is_given(boolean configurable, String time) {

        String expectedNextHearingDate = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        var nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(expectedNextHearingDate + "T16:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(nextHearingDate);

        String dateValue = nextHearingDateCalculator.calculateDate(
            evaluationResponses,
            NEXT_HEARING_DATE_TYPE,
            configurable,
            new HashMap<>(),
            new ArrayList<>()
        ).getValue().getValue();
        assertThat(LocalDateTime.parse(dateValue)).isEqualTo(expectedNextHearingDate + time);
    }

    @ParameterizedTest
    @CsvSource({
        "true, T16:00", "false, T16:00"
    })
    void should_throw_exception_when_input_date_has_wrong_format(boolean configurable, String time) {

        String expectedNextHearingDate = GIVEN_DATE.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));

        var nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(expectedNextHearingDate + "T16:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();


        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(nextHearingDate);

        Exception exception = assertThrowsExactly(RuntimeException.class, () ->
            nextHearingDateCalculator.calculateDate(
                evaluationResponses,
                NEXT_HEARING_DATE_TYPE,
                configurable,
                new HashMap<>(),
                new ArrayList<>()
            ));
        assertEquals(
            "Provided date has invalid format: " + expectedNextHearingDate + time,
            exception.getMessage()
        );

    }

    @ParameterizedTest
    @CsvSource({
        "true, T16:00:00", "false, T16:00:00"
    })
    void should_calculate_next_hearing_date_when_next_hearing_date_is_given(boolean configurable, String time) {

        String expectedNextHearingDate = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        var nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(expectedNextHearingDate))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(nextHearingDate);

        String dateValue = nextHearingDateCalculator.calculateDate(
            evaluationResponses,
            NEXT_HEARING_DATE_TYPE,
            configurable,
            new HashMap<>(),
            new ArrayList<>()
        ).getValue().getValue();
        assertThat(LocalDateTime.parse(dateValue)).isEqualTo(expectedNextHearingDate + time);
    }

    @ParameterizedTest
    @CsvSource({
        "true, T20:00", "false, T20:00"
    })
    void should_consider_only_next_hearing_date__when_given_configurable_next_hearing_date__and_un_configurable_time(
        boolean configurable, String time) {

        should_calculate_next_hearing_date__when_next_hearing_date__and_time_are_given(configurable, time);
    }

    @ParameterizedTest
    @CsvSource({
        "true, T20:00", "false, T20:00"
    })
    void should_calculate_next_hearing_date__when_next_hearing_date__and_time_are_given(
        boolean configurable, String time) {

        String expectedNextHearingDate = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        var nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(expectedNextHearingDate + "T16:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        var nextHearingDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateTime"))
            .value(CamundaValue.stringValue("20:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(nextHearingDate, nextHearingDateTime);

        String dateValue = nextHearingDateCalculator.calculateDate(
                evaluationResponses, NEXT_HEARING_DATE_TYPE, configurable, new HashMap<>(), new ArrayList<>())
            .getValue().getValue();
        assertThat(LocalDateTime.parse(dateValue)).isEqualTo(expectedNextHearingDate + time);
    }

    @ParameterizedTest
    @CsvSource({
        "true, T20:00:00", "false, T20:00:00"
    })
    void should_calculate_next_hearing_date_when_next_hearing_date_and_time_are_given_with_seconds(
        boolean configurable, String time) {
        should_calculate_next_hearing_date__when_next_hearing_date__and_time_are_given(configurable, time);
    }

    @ParameterizedTest
    @CsvSource({
        "true, T20:00:00+06:00", "false, T20:00:00Z"
    })
    void should_calculate_next_hearing_date_when_next_hearing_date_and_time_are_given_with_timezone(
        boolean configurable, String time) {

        String expectedNextHearingDate = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        var nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(expectedNextHearingDate + time))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        var nextHearingDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateTime"))
            .value(CamundaValue.stringValue("20:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(nextHearingDate, nextHearingDateTime);

        String dateValue = nextHearingDateCalculator.calculateDate(
                evaluationResponses, NEXT_HEARING_DATE_TYPE, configurable, new HashMap<>(), new ArrayList<>())
            .getValue().getValue();
        assertThat(LocalDateTime.parse(dateValue)).isEqualTo(expectedNextHearingDate + "T20:00");
    }

    @ParameterizedTest
    @CsvSource({
        "true, T19:00", "false, T19:00"
    })
    void should_calculate_next_hearing_date__from_last_entry_when_multiple_time_is_given(
        boolean configurable, String time) {
        String expectedNextHearingDate = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String expectedNextHearingDate2 = GIVEN_DATE.plusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        var nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(expectedNextHearingDate + "T16:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        var nextHearingDate2 = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(expectedNextHearingDate2 + "T19:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(nextHearingDate, nextHearingDate2);

        String dateValue = nextHearingDateCalculator.calculateDate(
                evaluationResponses,
                NEXT_HEARING_DATE_TYPE, configurable, new HashMap<>(),
                new ArrayList<>()
            )
            .getValue().getValue();
        assertThat(LocalDateTime.parse(dateValue)).isEqualTo(expectedNextHearingDate2 + time);
    }

    @BeforeEach
    void before() {
        nextHearingDateCalculator = new NextHearingDateCalculator();
    }
}
