package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.ConfigurationDmnEvaluationResponse;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateType.NEXT_HEARING_DATE;

@ExtendWith(MockitoExtension.class)
class NextHearingDateReCalculatorTest {

    public static final LocalDateTime GIVEN_DATE = LocalDateTime.of(2022, 10, 13, 18, 00, 00);
    public static final boolean IS_RECONFIGURE_REQUEST = true;

    private NextHearingDateCalculator nextHearingDateReCalculator;

    @BeforeEach
    public void before() {
        nextHearingDateReCalculator = new NextHearingDateCalculator();
    }

    @Test
    void should_not_supports_when_responses_contains_due_date_origin_and_time() {
        String expectedNextHearingDate = GIVEN_DATE.plusDays(0)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateOrigin"))
            .value(CamundaValue.stringValue(expectedNextHearingDate + "T16:00"))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateTime"))
            .value(CamundaValue.stringValue("16:00"))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(nextHearingDate, nextHearingDateTime);

        assertThat(nextHearingDateReCalculator.supports(
            evaluationResponses,
            NEXT_HEARING_DATE,
            IS_RECONFIGURE_REQUEST
        )).isFalse();
    }

    @Test
    void should_not_supports_when_responses_contains_only_due_date_time() {

        ConfigurationDmnEvaluationResponse nextHearingDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateTime"))
            .value(CamundaValue.stringValue("16:00"))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(nextHearingDateTime);

        assertThat(nextHearingDateReCalculator.supports(
            evaluationResponses,
            NEXT_HEARING_DATE,
            IS_RECONFIGURE_REQUEST
        )).isFalse();
    }

    @Test
    void should_not_supports_when_responses_only_contains_due_date_with_can_configure_false_but_not_origin() {
        String expectedNextHearingDate = GIVEN_DATE.plusDays(0)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(expectedNextHearingDate + "T16:00"))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateTime"))
            .value(CamundaValue.stringValue("16:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(nextHearingDate, nextHearingDateTime);

        assertThat(nextHearingDateReCalculator.supports(
            evaluationResponses,
            NEXT_HEARING_DATE,
            IS_RECONFIGURE_REQUEST
        )).isFalse();
    }

    @Test
    void should_supports_when_responses_only_contains_due_date_but_not_origin() {
        String expectedNextHearingDate = GIVEN_DATE.plusDays(0)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(expectedNextHearingDate + "T16:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateTime"))
            .value(CamundaValue.stringValue("16:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(nextHearingDate, nextHearingDateTime);

        assertThat(nextHearingDateReCalculator.supports(
            evaluationResponses,
            NEXT_HEARING_DATE,
            IS_RECONFIGURE_REQUEST
        )).isTrue();
    }

    @Test
    void should_calculate_due_date_when_due_date_is_given() {

        String expectedNextHearingDate = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(expectedNextHearingDate + "T16:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(nextHearingDate);

        String dateValue = nextHearingDateReCalculator.calculateDate(
            evaluationResponses,
            NEXT_HEARING_DATE, true
        ).getValue().getValue();
        assertThat(LocalDateTime.parse(dateValue)).isEqualTo(expectedNextHearingDate + "T16:00");
    }

    @Test
    void should_consider_only_due_date_when_given_configurable_due_date_and_un_configurable_time() {

        String expectedNextHearingDate = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(expectedNextHearingDate + "T16:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateTime"))
            .value(CamundaValue.stringValue("20:00"))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(nextHearingDate, nextHearingDateTime);

        String dateValue = nextHearingDateReCalculator.calculateDate(
            evaluationResponses,
            NEXT_HEARING_DATE, true
        ).getValue().getValue();
        assertThat(LocalDateTime.parse(dateValue)).isEqualTo(expectedNextHearingDate + "T16:00");
    }

    @Test
    void should_calculate_due_date_when_due_date_and_time_are_given() {

        String expectedNextHearingDate = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(expectedNextHearingDate + "T16:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateTime"))
            .value(CamundaValue.stringValue("20:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(nextHearingDate, nextHearingDateTime);

        String dateValue = nextHearingDateReCalculator.calculateDate(
            evaluationResponses,
            NEXT_HEARING_DATE, true
        ).getValue().getValue();
        assertThat(LocalDateTime.parse(dateValue)).isEqualTo(expectedNextHearingDate + "T20:00");
    }


    @Test
    void should_calculate_due_date_from_last_entry_when_multiple_time_is_given() {
        String expectedNextHearingDate = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String expectedNextHearingDate2 = GIVEN_DATE.plusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(expectedNextHearingDate + "T16:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDate2 = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(expectedNextHearingDate2 + "T19:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(nextHearingDate, nextHearingDate2);

        String dateValue = nextHearingDateReCalculator.calculateDate(
            evaluationResponses,
            NEXT_HEARING_DATE, true
        ).getValue().getValue();
        assertThat(LocalDateTime.parse(dateValue)).isEqualTo(expectedNextHearingDate2 + "T19:00");
    }
}
