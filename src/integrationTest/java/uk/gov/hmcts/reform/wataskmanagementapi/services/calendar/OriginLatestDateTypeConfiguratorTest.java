package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.ConfigurationDmnEvaluationResponse;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.calendar.DateTypeIntervalData.DATE_TYPE_MUST_BE_WORKING_DAY_NEXT;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.PublicHolidaysCollectionTest.CALENDAR_URI;

@SpringBootTest
@ActiveProfiles({"integration"})
public class OriginLatestDateTypeConfiguratorTest {

    public static final LocalDateTime GIVEN_DATE = LocalDateTime.of(2022, 10, 13, 18, 0, 0);

    public static final LocalDateTime BST_DATE_BACKWARD = LocalDateTime.of(2022, 10, 26, 18, 0, 0);
    public static final LocalDateTime BST_DATE_FORWARD = LocalDateTime.of(2023, 3, 26, 18, 0, 0);

    public static final String PRIORITY_DATE_VALUE = GIVEN_DATE.plusDays(2).format(DateTimeFormatter.ofPattern(
        "yyyy-MM-dd")) + "T16:00";
    public static final String DUE_DATE_VALUE = GIVEN_DATE.plusDays(1).format(DateTimeFormatter.ofPattern(
        "yyyy-MM-dd")) + "T16:00";
    public static final String NEXT_HEARING_DATE_VALUE = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        + "T16:00";
    private final Map<String, Object> taskAttributes = new HashMap<>();

    @Autowired
    private DateTypeConfigurator dateTypeConfigurator;

    @Test
    public void should_calculate_both_dates_when_multiple_origin_latest_exist_for_two_date_types() {
        String nextHearingDateValue = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + "T16:00";
        String nextHearingDurationValue = GIVEN_DATE.plusDays(4)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + "T16:00";

        ConfigurationDmnEvaluationResponse dueDateOriginLatest = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOriginLatest"))
            .value(CamundaValue.stringValue("nextHearingDate,nextHearingDuration"))
            .build();

        ConfigurationDmnEvaluationResponse priorityDateOriginLatest = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateOriginLatest"))
            .value(CamundaValue.stringValue("nextHearingDate,dueDate"))
            .build();

        ConfigurationDmnEvaluationResponse calculatedDates = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("calculatedDates"))
            .value(CamundaValue.stringValue("nextHearingDate,nextHearingDuration,dueDate,priorityDate"))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(nextHearingDateValue))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDuration = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDuration"))
            .value(CamundaValue.stringValue(nextHearingDurationValue))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = readDueAndPriorityDateOriginFields(
            dueDateOriginLatest, priorityDateOriginLatest, calculatedDates, nextHearingDate, nextHearingDuration);

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses
            = dateTypeConfigurator.configureDates(evaluationResponses,
                                                  false,
                                                  false,
                                                  taskAttributes);

        assertThat(configurationDmnEvaluationResponses)
            .hasSize(4)
            .isEqualTo(List.of(
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("nextHearingDate"))
                    .value(CamundaValue.stringValue("2022-10-13T16:00"))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("nextHearingDuration"))
                    .value(CamundaValue.stringValue("2022-10-17T16:00"))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("dueDate"))
                    .value(CamundaValue.stringValue("2022-10-20T17:00"))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("priorityDate"))
                    .value(CamundaValue.stringValue("2022-10-28T21:00"))
            .build()
            ));
    }

    @Test
    public void should_calculate_date_when_single_origin_latest_exist() {
        ConfigurationDmnEvaluationResponse dueDateOriginLatest = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateOriginLatest"))
            .value(CamundaValue.stringValue("nextHearingDate,dueDate"))
            .build();

        ConfigurationDmnEvaluationResponse dueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(DUE_DATE_VALUE))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(NEXT_HEARING_DATE_VALUE))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = readDueDateOriginFields(
            dueDateOriginLatest, dueDate, nextHearingDate);

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses
            = dateTypeConfigurator.configureDates(evaluationResponses,
                                                  false,
                                                  false,
                                                  taskAttributes);

        assertThat(configurationDmnEvaluationResponses)
            .hasSize(3)
            .isEqualTo(List.of(
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("nextHearingDate"))
                    .value(CamundaValue.stringValue("2022-10-13T16:00"))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("dueDate"))
                    .value(CamundaValue.stringValue("2022-10-14T17:00"))
            .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(CamundaValue.stringValue("priorityDate"))
                .value(CamundaValue.stringValue("2022-10-14T17:00"))
                .build()
            ));
    }

    @Test
    public void should_not_calculate_date_when_single_origin_latest_containing_two_dates_with_first_empty_exist() {
        ConfigurationDmnEvaluationResponse dueDateOriginLatest = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOriginLatest"))
            .value(CamundaValue.stringValue("nextHearingDate,nextHearingDuration"))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDuration = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDuration"))
            .value(CamundaValue.stringValue(PRIORITY_DATE_VALUE))
            .build();

        var calculatedDates = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("calculatedDates"))
            .value(CamundaValue.stringValue("nextHearingDate,nextHearingDuration,dueDate,priorityDate"))
            .build();


        List<ConfigurationDmnEvaluationResponse> evaluationResponses = readDueDateOriginFields(
            dueDateOriginLatest, nextHearingDuration, calculatedDates);

        assertThatThrownBy(() -> dateTypeConfigurator
            .configureDates(evaluationResponses,
                false,
                false,
                taskAttributes
            ))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Calculates dates orders are incorrect based on.");
    }

    private List<ConfigurationDmnEvaluationResponse> readDueAndPriorityDateOriginFields(
        ConfigurationDmnEvaluationResponse... fields) {
        List<ConfigurationDmnEvaluationResponse> allFields = new ArrayList<>();
        allFields.addAll(getDueDateConfigurations());
        allFields.addAll(getPriorityDateConfigurations());
        allFields.addAll(List.of(fields));
        return allFields;
    }

    private List<ConfigurationDmnEvaluationResponse> readDueDateOriginFields(
        ConfigurationDmnEvaluationResponse... fields) {
        List<ConfigurationDmnEvaluationResponse> allFields = new ArrayList<>();
        allFields.addAll(getDueDateConfigurations());
        allFields.addAll(List.of(fields));
        return allFields;
    }

    private List<ConfigurationDmnEvaluationResponse> getDueDateConfigurations() {
        return List.of(
            ConfigurationDmnEvaluationResponse.builder()
                .name(CamundaValue.stringValue("dueDateIntervalDays"))
                .value(CamundaValue.stringValue("3"))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(CamundaValue.stringValue("dueDateNonWorkingCalendar"))
                .value(CamundaValue.stringValue(CALENDAR_URI))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(CamundaValue.stringValue("dueDateNonWorkingDaysOfWeek"))
                .value(CamundaValue.stringValue("SATURDAY,SUNDAY"))
            .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(CamundaValue.stringValue("dueDateSkipNonWorkingDays"))
                .value(CamundaValue.stringValue("true"))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(CamundaValue.stringValue("dueDateMustBeWorkingDay"))
                .value(CamundaValue.stringValue(DATE_TYPE_MUST_BE_WORKING_DAY_NEXT))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(CamundaValue.stringValue("dueDateTime"))
                .value(CamundaValue.stringValue("17:00"))
                .build()
        );
    }

    private List<ConfigurationDmnEvaluationResponse> getPriorityDateConfigurations() {
        return List.of(
            ConfigurationDmnEvaluationResponse.builder()
                .name(CamundaValue.stringValue("priorityDateIntervalDays"))
                .value(CamundaValue.stringValue("6"))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(CamundaValue.stringValue("priorityDateNonWorkingCalendar"))
                .value(CamundaValue.stringValue(CALENDAR_URI))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(CamundaValue.stringValue("priorityDateNonWorkingDaysOfWeek"))
                .value(CamundaValue.stringValue("SATURDAY,SUNDAY"))
            .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(CamundaValue.stringValue("priorityDateSkipNonWorkingDays"))
                .value(CamundaValue.stringValue("true"))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(CamundaValue.stringValue("priorityDateMustBeWorkingDay"))
                .value(CamundaValue.stringValue(DATE_TYPE_MUST_BE_WORKING_DAY_NEXT))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(CamundaValue.stringValue("priorityDateTime"))
                .value(CamundaValue.stringValue("21:00"))
                .build()
        );
    }

    @Test
    public void shouldCalculateDateWhenOriginDateIsBSTDueDateNonBSTBackword() {
        String localDateTime = BST_DATE_BACKWARD.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        //Clocks go back an hour at 2:00am
        ConfigurationDmnEvaluationResponse dueDateOriginRef = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOriginLatest"))
            .value(CamundaValue.stringValue("nextHearingDate"))
            .build();

        String priorityDateValue = localDateTime + "T01:30";
        ConfigurationDmnEvaluationResponse priorityDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDate"))
            .value(CamundaValue.stringValue(priorityDateValue))
            .build();
        String nextHearingDateValue = localDateTime + "T16:00";
        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(nextHearingDateValue))
            .build();
        ConfigurationDmnEvaluationResponse dueDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateIntervalDays"))
            .value(CamundaValue.stringValue("4"))
            .build();
        ConfigurationDmnEvaluationResponse dueDateNonWorkingCalendar = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateNonWorkingCalendar"))
            .value(CamundaValue.stringValue("https://www.gov.uk/bank-holidays/england-and-wales.json"))
            .build();

        ConfigurationDmnEvaluationResponse dueDateNonWorkingDaysOfWeek = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateNonWorkingDaysOfWeek"))
            .value(CamundaValue.stringValue(""))
            .build();
        ConfigurationDmnEvaluationResponse dueDateSkipNonWorkingDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateSkipNonWorkingDays"))
            .value(CamundaValue.stringValue("false"))
            .build();
        ConfigurationDmnEvaluationResponse dueDateMustBeWorkingDay = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateMustBeWorkingDay"))
            .value(CamundaValue.stringValue("false"))
            .build();

        ConfigurationDmnEvaluationResponse dueDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateTime"))
            .value(CamundaValue.stringValue("02:30"))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDates(
                List.of(dueDateIntervalDays, dueDateNonWorkingCalendar, dueDateMustBeWorkingDay, priorityDate,
                        dueDateNonWorkingDaysOfWeek, dueDateSkipNonWorkingDays, dueDateOriginRef, dueDateTime,
                        nextHearingDate
                ),
                false, false,
                    taskAttributes);

        assertThat(configurationDmnEvaluationResponses).hasSize(3)
            .isEqualTo(List.of(
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("nextHearingDate"))
                    .value(CamundaValue.stringValue(nextHearingDateValue))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("dueDate"))
                    .value(CamundaValue.stringValue("2022-10-30T02:30"))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("priorityDate"))
                    .value(CamundaValue.stringValue(priorityDateValue))
                    .build()
            ));
    }

    @Test
    public void shouldCalculateDateWhenOriginDateIsBSTDueDateNonBSTForward() {
        String localDateTime = BST_DATE_FORWARD.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        //Clocks go forward an hour at 1:00am
        ConfigurationDmnEvaluationResponse dueDateOriginRef = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOriginLatest"))
            .value(CamundaValue.stringValue("nextHearingDate,nextHearingDuration"))
            .build();

        String priorityDateValue = localDateTime + "T00:30";
        ConfigurationDmnEvaluationResponse nextHearingDuration = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDuration"))
            .value(CamundaValue.stringValue(priorityDateValue))
            .build();
        String nextHearingDateValue = localDateTime + "T16:00";
        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(nextHearingDateValue))
            .build();
        ConfigurationDmnEvaluationResponse dueDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateIntervalDays"))
            .value(CamundaValue.stringValue("4"))
            .build();
        ConfigurationDmnEvaluationResponse dueDateNonWorkingCalendar = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateNonWorkingCalendar"))
            .value(CamundaValue.stringValue("https://www.gov.uk/bank-holidays/england-and-wales.json"))
            .build();

        ConfigurationDmnEvaluationResponse dueDateNonWorkingDaysOfWeek = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateNonWorkingDaysOfWeek"))
            .value(CamundaValue.stringValue(""))
            .build();
        ConfigurationDmnEvaluationResponse dueDateSkipNonWorkingDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateSkipNonWorkingDays"))
            .value(CamundaValue.stringValue("false"))
            .build();
        ConfigurationDmnEvaluationResponse dueDateMustBeWorkingDay = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateMustBeWorkingDay"))
            .value(CamundaValue.stringValue("false"))
            .build();

        ConfigurationDmnEvaluationResponse dueDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateTime"))
            .value(CamundaValue.stringValue("01:30"))
            .build();

        var calculatedDates = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("calculatedDates"))
            .value(CamundaValue.stringValue("nextHearingDate,nextHearingDuration,dueDate,priorityDate"))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDates(
                List.of(dueDateIntervalDays, dueDateNonWorkingCalendar, dueDateMustBeWorkingDay, nextHearingDuration,
                        dueDateNonWorkingDaysOfWeek, dueDateSkipNonWorkingDays, dueDateOriginRef, dueDateTime,
                        nextHearingDate, calculatedDates
                ),
                false, false,
                    taskAttributes);

        assertThat(configurationDmnEvaluationResponses).hasSize(4)
            .isEqualTo(List.of(
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("nextHearingDate"))
                    .value(CamundaValue.stringValue(nextHearingDateValue))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("nextHearingDuration"))
                    .value(CamundaValue.stringValue("2023-03-26T00:30"))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("dueDate"))
                    .value(CamundaValue.stringValue("2023-03-30T01:30"))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("priorityDate"))
                    .value(CamundaValue.stringValue("2023-03-30T01:30"))
                    .build()
            ));
    }


    @Test
    public void shouldReCalculateDateWhenBothDueDateAndDueDateOriginLatestAreSetForReconfiguration() {
        String dueDateValue = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String priorityDateValue = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String nextHearingDateValue = GIVEN_DATE.plusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse dueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(dueDateValue + "T18:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse dueDateOriginLatest = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOriginLatest"))
            .value(CamundaValue.stringValue("nextHearingDate,priorityDate"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse priorityDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDate"))
            .value(CamundaValue.stringValue(priorityDateValue + "T18:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(nextHearingDateValue + "T21:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDates(List.of(dueDate, dueDateOriginLatest, nextHearingDate, priorityDate),
                            false, true,
                    taskAttributes);

        assertThat(configurationDmnEvaluationResponses).hasSize(3)
            .isEqualTo(List.of(
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("nextHearingDate"))
                    .value(CamundaValue.stringValue(nextHearingDateValue + "T21:00"))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("dueDate"))
                    .value(CamundaValue.stringValue(dueDateValue + "T18:00"))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("priorityDate"))
                    .value(CamundaValue.stringValue(dueDateValue + "T18:00"))
                    .build()
            ));
    }

    @Test
    public void shouldReCalculateDateWhenBothPriorityDateAndPriorityDateOriginLatestAreSetForReconfiguration() {
        String priorityDateValue = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String dueDateValue = GIVEN_DATE.minusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String nextHearingDateValue = GIVEN_DATE.plusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse priorityDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDate"))
            .value(CamundaValue.stringValue(priorityDateValue + "T18:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse dueDateOriginRef = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateOriginLatest"))
            .value(CamundaValue.stringValue("nextHearingDate,dueDate"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(nextHearingDateValue + "T21:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();
        ConfigurationDmnEvaluationResponse dueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(dueDateValue + "T21:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDates(List.of(priorityDate, dueDate, dueDateOriginRef, nextHearingDate),
                            false,
                            true,
                            taskAttributes);

        String calculatedDueDate = priorityDateValue + "T18:00";

        assertThat(configurationDmnEvaluationResponses).hasSize(2)
            .isEqualTo(List.of(
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("nextHearingDate"))
                    .value(CamundaValue.stringValue(nextHearingDateValue + "T21:00"))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("priorityDate"))
                    .value(CamundaValue.stringValue(calculatedDueDate))
                    .build()
            ));
    }

    @Test
    public void shouldReCalculateDateWhenBothNextHearingDateAndNextHearingDateOriginLatestAreSetForReconfiguration() {
        String dueDateValue = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String nextHearingDateValue = GIVEN_DATE.plusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse dueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(dueDateValue + "T18:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDateOriginLatest = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateOriginLatest"))
            .value(CamundaValue.stringValue("dueDate"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(nextHearingDateValue + "T21:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDates(List.of(dueDate, nextHearingDateOriginLatest, nextHearingDate),
                            false,
                            true,
                            taskAttributes);

        String calculatedDueDate = dueDateValue + "T18:00";

        assertThat(configurationDmnEvaluationResponses).hasSize(2)
            .isEqualTo(List.of(
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("nextHearingDate"))
                    .value(CamundaValue.stringValue(calculatedDueDate))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("dueDate"))
                    .value(CamundaValue.stringValue(nextHearingDateValue + "T21:00"))
                    .build()
            ));
    }

    @Test
    public void shouldNotReCalculateDateWhenBothDueDateAndDueDateOriginLatestAreNotSetForReconfiguration() {
        String dueDateValue = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String priorityDateValue = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String nextHearingDateValue = GIVEN_DATE.plusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse dueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(dueDateValue + "T18:00"))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        ConfigurationDmnEvaluationResponse dueDateOriginLatest = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOriginLatest"))
            .value(CamundaValue.stringValue("nextHearingDate,priorityDate"))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        ConfigurationDmnEvaluationResponse priorityDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDate"))
            .value(CamundaValue.stringValue(priorityDateValue + "T18:00"))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(nextHearingDateValue + "T21:00"))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDates(List.of(dueDate, dueDateOriginLatest, nextHearingDate, priorityDate),
                            false, true,
                    taskAttributes);

        assertThat(configurationDmnEvaluationResponses).isEmpty();
    }

    @Test
    public void shouldNotReCalculateDateWhenBothPriorityDateAndPriorityDateOriginLatestAreNotSetForReconfiguration() {
        String priorityDateValue = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String dueDateValue = GIVEN_DATE.minusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String nextHearingDateValue = GIVEN_DATE.plusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse priorityDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDate"))
            .value(CamundaValue.stringValue(priorityDateValue + "T18:00"))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        ConfigurationDmnEvaluationResponse dueDateOriginRef = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateOriginLatest"))
            .value(CamundaValue.stringValue("nextHearingDate,dueDate"))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(nextHearingDateValue + "T21:00"))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();
        ConfigurationDmnEvaluationResponse dueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(dueDateValue + "T21:00"))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDates(List.of(priorityDate, dueDate, dueDateOriginRef, nextHearingDate),
                            false,
                            true,
                            taskAttributes);

        assertThat(configurationDmnEvaluationResponses).isEmpty();
    }

    @Test
    public void shouldNotReCalculateDateWhenBothNextHearingDateAndOriginLatestAreNotSetForReconfiguration() {
        String dueDateValue = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String nextHearingDateValue = GIVEN_DATE.plusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse dueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(dueDateValue + "T18:00"))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        ConfigurationDmnEvaluationResponse dueDateOriginLatest = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateOriginLatest"))
            .value(CamundaValue.stringValue("dueDate"))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(nextHearingDateValue + "T21:00"))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDates(List.of(dueDate, dueDateOriginLatest, nextHearingDate),
                            false,
                            true,
                            taskAttributes);

        assertThat(configurationDmnEvaluationResponses).isEmpty();
    }

    @Test
    public void shouldNotRecalculateDateWhenDueDateIsUnconfigurableButOriginLatestIsConfigurable() {
        String dueDateValue = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse dueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(dueDateValue + "T18:00"))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        ConfigurationDmnEvaluationResponse dueDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOriginLatest"))
            .value(CamundaValue.stringValue("nextHearingDate,priorityDate"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDates(List.of(dueDate, dueDateOrigin),
                            false,
                            true,
                            taskAttributes);

        assertThat(configurationDmnEvaluationResponses).isEmpty();
    }

    @Test
    public void shouldNotRecalculateDateWhenPriorityDateIsUnconfigurableButOriginLatestIsConfigurable() {
        String priorityDateValue = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse priorityDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDate"))
            .value(CamundaValue.stringValue(priorityDateValue + "T18:00"))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        ConfigurationDmnEvaluationResponse priorityDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateOriginLatest"))
            .value(CamundaValue.stringValue("nextHearingDate,priorityDate"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDates(List.of(priorityDate, priorityDateOrigin),
                            false,
                            true,
                            taskAttributes);

        assertThat(configurationDmnEvaluationResponses).isEmpty();
    }

    @Test
    public void shouldNotRecalculateDateWhenNextHearingDateIsUnconfigurableButOriginLatestIsConfigurable() {
        String nextHearingDateValue = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(nextHearingDateValue + "T18:00"))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateOriginLatest"))
            .value(CamundaValue.stringValue("nextHearingDate,priorityDate"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDates(List.of(nextHearingDate, nextHearingDateOrigin),
                            false,
                            true,
                            taskAttributes);

        assertThat(configurationDmnEvaluationResponses).isEmpty();
    }

    @Test
    public void shouldRecalculateDateWhenDueDateIsConfigurableButOriginLatestIsUnConfigurable() {
        String dueDateValue = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse dueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(dueDateValue + "T18:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse dueDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOriginLatest"))
            .value(CamundaValue.stringValue("nextHearingDate,priorityDate"))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDates(List.of(dueDate, dueDateOrigin),
                            false,
                            true,
                            taskAttributes);

        assertThat(configurationDmnEvaluationResponses).hasSize(1)
            .isEqualTo(List.of(
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("dueDate"))
                    .value(CamundaValue.stringValue(dueDateValue + "T18:00"))
                    .build()
            ));
    }

    @Test
    public void shouldNotRecalculateDateWhenPriorityDateIsConfigurableButOriginLatestIsUnConfigurable() {
        String priorityDateValue = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse priorityDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDate"))
            .value(CamundaValue.stringValue(priorityDateValue + "T18:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse priorityDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateOriginLatest"))
            .value(CamundaValue.stringValue("nextHearingDate,priorityDate"))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDates(List.of(priorityDate, priorityDateOrigin),
                            false,
                            true,
                            taskAttributes);

        assertThat(configurationDmnEvaluationResponses).hasSize(1)
            .isEqualTo(List.of(
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("priorityDate"))
                    .value(CamundaValue.stringValue(priorityDate.getValue().getValue()))
                    .build()
            ));
    }

    @Test
    public void shouldNotRecalculateDateWhenNextHearingDateIsConfigurableButOriginLatestIsUnConfigurable() {
        String nextHearingDateValue = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(nextHearingDateValue + "T18:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateOriginLatest"))
            .value(CamundaValue.stringValue("nextHearingDate,priorityDate"))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDates(List.of(nextHearingDate, nextHearingDateOrigin),
                            false,
                            true,
                            taskAttributes);

        assertThat(configurationDmnEvaluationResponses).hasSize(1)
            .isEqualTo(List.of(
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("nextHearingDate"))
                    .value(CamundaValue.stringValue(nextHearingDateValue + "T18:00"))
                    .build()
            ));
    }

    @Test
    public void shouldCalculatePriorityDateFromTaskResourceWhenReferenceDatesAreNotProvidedForReconfigure() {
        String nextHearingDateValue = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String dueDateValue = GIVEN_DATE.minusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        LocalDateTime taskResourceDueDate = GIVEN_DATE.plusDays(4);
        taskAttributes.put("dueDateTime", taskResourceDueDate.atZone(ZoneId.systemDefault()).toOffsetDateTime());

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(nextHearingDateValue + "T18:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse dueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(dueDateValue + "T21:00"))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        ConfigurationDmnEvaluationResponse priorityDateOriginLatest = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateOriginLatest"))
            .value(CamundaValue.stringValue("nextHearingDate,dueDate"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        List<ConfigurationDmnEvaluationResponse> configResponses = dateTypeConfigurator
            .configureDates(
                List.of(
                    nextHearingDate,
                    dueDate,
                    priorityDateOriginLatest
                ),
                false,
                true,
                taskAttributes
            );

        assertThat(configResponses).hasSize(2)
            .isEqualTo(List.of(
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("nextHearingDate"))
                    .value(CamundaValue.stringValue(nextHearingDateValue + "T18:00"))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("priorityDate"))
                    .value(CamundaValue.stringValue(taskResourceDueDate
                                                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                                                        + "T18:00"))
                    .build()
            ));
    }

    @Test
    public void shouldCalculateDueDateFromTaskResourceWhenReferenceDatesAreNotProvidedForReconfigure() {
        String nextHearingDateValue = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String dueDateValue = GIVEN_DATE.minusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        LocalDateTime taskResourceDueDate = GIVEN_DATE.plusDays(4);
        taskAttributes.put("dueDateTime", taskResourceDueDate.atZone(ZoneId.systemDefault()).toOffsetDateTime());

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(nextHearingDateValue + "T18:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse dueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(dueDateValue + "T21:00"))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        ConfigurationDmnEvaluationResponse priorityDateOriginLatest = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateOriginLatest"))
            .value(CamundaValue.stringValue("nextHearingDate,dueDate"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        List<ConfigurationDmnEvaluationResponse> configResponses = dateTypeConfigurator
            .configureDates(
                List.of(
                    nextHearingDate,
                    dueDate,
                    priorityDateOriginLatest
                ),
                false,
                true,
                taskAttributes
            );

        assertThat(configResponses).hasSize(2)
            .isEqualTo(List.of(
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("nextHearingDate"))
                    .value(CamundaValue.stringValue(nextHearingDateValue + "T18:00"))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("priorityDate"))
                    .value(CamundaValue.stringValue(taskResourceDueDate
                                                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                                                        + "T18:00"))
                    .build()
            ));
    }

    @Test
    public void shouldCalculateNextHearingDateFromTaskResourceWhenReferenceDatesAreNotProvidedForReconfigure() {
        String dueDateValue = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String nextHearingDateValue = GIVEN_DATE.minusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        LocalDateTime taskResourceDueDate = GIVEN_DATE.plusDays(4);
        taskAttributes.put("nextHearingDate", taskResourceDueDate.atZone(ZoneId.systemDefault()).toOffsetDateTime());


        ConfigurationDmnEvaluationResponse dueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(dueDateValue + "T18:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(nextHearingDateValue + "T21:00"))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDateOriginLatest = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateOriginLatest"))
            .value(CamundaValue.stringValue("nextHearingDate,dueDate"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        List<ConfigurationDmnEvaluationResponse> configResponses = dateTypeConfigurator
            .configureDates(
                List.of(
                    dueDate,
                    nextHearingDate,
                    nextHearingDateOriginLatest
                ),
                false,
                true,
                taskAttributes
            );

        assertThat(configResponses).hasSize(2)
            .isEqualTo(List.of(
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("dueDate"))
                    .value(CamundaValue.stringValue(dueDateValue + "T18:00"))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("priorityDate"))
                    .value(CamundaValue.stringValue(taskResourceDueDate
                                                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                                                        + "T18:00"))
                    .build()
            ));
    }

    @Test
    public void should_not_calculate_date_when_multiple_origin_date_types_for_due_date_exist() {
        ConfigurationDmnEvaluationResponse dueDateOriginLatest = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOriginLatest"))
            .value(CamundaValue.stringValue("nextHearingDate,nextHearingDuration"))
            .build();

        ConfigurationDmnEvaluationResponse dueDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOrigin"))
            .value(CamundaValue.stringValue(DUE_DATE_VALUE))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(dueDateOriginLatest, dueDateOrigin);

        assertThatThrownBy(() -> dateTypeConfigurator
            .configureDates(evaluationResponses,
                            false,
                            false,
                            taskAttributes
            ))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Origin dates have multiple occurrence, Date type can't be calculated.");
    }


    @Test
    public void should_not_calculate_date_when_multiple_origin_date_types_for_priority_date_exist() {
        ConfigurationDmnEvaluationResponse priorityDateOriginLatest = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateOriginLatest"))
            .value(CamundaValue.stringValue("nextHearingDate,nextHearingDuration"))
            .build();

        ConfigurationDmnEvaluationResponse priorityDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateOrigin"))
            .value(CamundaValue.stringValue(PRIORITY_DATE_VALUE))
            .build();


        List<ConfigurationDmnEvaluationResponse> evaluationResponses
            = List.of(priorityDateOriginLatest, priorityDateOrigin);

        assertThatThrownBy(() -> dateTypeConfigurator
            .configureDates(evaluationResponses,
                            false,
                            false,
                            taskAttributes
            ))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Origin dates have multiple occurrence, Date type can't be calculated.");
    }

    @Test
    public void should_not_calculate_date_when_multiple_origin_date_types_for_next_hearing_date_exist() {
        ConfigurationDmnEvaluationResponse nextHearingDateOriginLatest = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateOriginLatest"))
            .value(CamundaValue.stringValue("nextHearingDate,nextHearingDuration"))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateOrigin"))
            .value(CamundaValue.stringValue(NEXT_HEARING_DATE_VALUE))
            .build();


        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(
            nextHearingDateOriginLatest, nextHearingDateOrigin);

        assertThatThrownBy(() -> dateTypeConfigurator
            .configureDates(evaluationResponses,
                            false,
                            false,
                            taskAttributes
            ))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Origin dates have multiple occurrence, Date type can't be calculated.");
    }


    @Test
    public void should_not_calculate_date_when_multiple_origin_date_types_for_intermediate_date_exist() {
        ConfigurationDmnEvaluationResponse nextHearingDurationOriginLatest = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDurationOriginLatest"))
            .value(CamundaValue.stringValue("nextHearingDate,nextHearingDuration"))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDurationOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDurationOrigin"))
            .value(CamundaValue.stringValue(DUE_DATE_VALUE))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(
            nextHearingDurationOriginLatest, nextHearingDurationOrigin);

        assertThatThrownBy(() -> dateTypeConfigurator
            .configureDates(evaluationResponses,
                            false,
                            false,
                            taskAttributes
            ))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Origin dates have multiple occurrence, Date type can't be calculated.");
    }
}

