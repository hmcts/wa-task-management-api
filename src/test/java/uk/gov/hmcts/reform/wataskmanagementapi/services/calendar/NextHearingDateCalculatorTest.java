package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateTypeConfigurator.DateTypeObject;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateType.NEXT_HEARING_DATE;

@ExtendWith(MockitoExtension.class)
class NextHearingDateCalculatorTest {

    public static final LocalDateTime GIVEN_DATE = LocalDateTime.of(2022, 10, 13, 18, 00, 00);
    public static final boolean IS_NOT_RECONFIGURE_REQUEST = false;
    public static final DateTypeObject NEXT_HEARING_DATE_TYPE = new DateTypeObject(
        NEXT_HEARING_DATE,
        NEXT_HEARING_DATE.getType()
    );

    private NextHearingDateCalculator nextHearingDateCalculator;


    @BeforeEach
    public void before() {
        nextHearingDateCalculator = new NextHearingDateCalculator();
    }

    @Test
    void should_not_supports_when_responses_contains_next_hearing_date_origin_and_time() {
        String expectedDueDate = GIVEN_DATE.plusDays(0)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateOrigin"))
            .value(CamundaValue.stringValue(expectedDueDate + "T16:00"))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateTime"))
            .value(CamundaValue.stringValue("16:00"))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(nextHearingDate, nextHearingDateTime);

        assertThat(nextHearingDateCalculator.supports(
            evaluationResponses,
            NEXT_HEARING_DATE_TYPE,
            IS_NOT_RECONFIGURE_REQUEST
        )).isFalse();
    }

    @Test
    void should_not_supports_when_responses_contains_only_next_hearing_date_time() {

        ConfigurationDmnEvaluationResponse nextHearingDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateTime"))
            .value(CamundaValue.stringValue("16:00"))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(nextHearingDateTime);

        assertThat(nextHearingDateCalculator.supports(
            evaluationResponses,
            NEXT_HEARING_DATE_TYPE,
            IS_NOT_RECONFIGURE_REQUEST
        )).isFalse();
    }

    @Test
    void should_supports_when_responses_only_contains_next_hearing_date_but_not_origin() {
        String expectedDueDate = GIVEN_DATE.plusDays(0)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(expectedDueDate + "T16:00"))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateTime"))
            .value(CamundaValue.stringValue("16:00"))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(nextHearingDate, nextHearingDateTime);

        assertThat(nextHearingDateCalculator.supports(
            evaluationResponses,
            NEXT_HEARING_DATE_TYPE,
            IS_NOT_RECONFIGURE_REQUEST
        )).isTrue();
    }

    @Test
    void should_calculate_next_hearing_date_when_next_hearing_date_is_given() {

        String expectedDueDate = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(expectedDueDate + "T16:00"))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(nextHearingDate);

        String dateValue = nextHearingDateCalculator.calculateDate(
                NEXT_HEARING_DATE_TYPE,
                evaluationResponses
            )
            .getValue().getValue();
        assertThat(LocalDateTime.parse(dateValue)).isEqualTo(expectedDueDate + "T16:00");
    }

    @Test
    void should_calculate_next_hearing_date_when_next_hearing_date_and_time_are_given() {

        String expectedDueDate = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(expectedDueDate + "T16:00"))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateTime"))
            .value(CamundaValue.stringValue("20:00"))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(nextHearingDate, nextHearingDateTime);

        String dateValue = nextHearingDateCalculator.calculateDate(
                NEXT_HEARING_DATE_TYPE,
                evaluationResponses
            )
            .getValue().getValue();
        assertThat(LocalDateTime.parse(dateValue)).isEqualTo(expectedDueDate + "T20:00");
    }


    @Test
    void should_calculate_next_hearing_date_from_last_entry_when_multiple_time_is_given() {
        String expectedDueDate = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String expectedDueDate2 = GIVEN_DATE.plusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(expectedDueDate + "T16:00"))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDate2 = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(expectedDueDate2 + "T19:00"))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(nextHearingDate, nextHearingDate2);

        String dateValue = nextHearingDateCalculator.calculateDate(
                NEXT_HEARING_DATE_TYPE,
                evaluationResponses
            )
            .getValue().getValue();
        assertThat(LocalDateTime.parse(dateValue)).isEqualTo(expectedDueDate2 + "T19:00");
    }
}
