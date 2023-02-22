package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.ConfigurationDmnEvaluationResponse;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.calendar.DateTypeIntervalData.DATE_TYPE_MUST_BE_WORKING_DAY_NEXT;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaValue.stringValue;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateCalculator.DEFAULT_DATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateCalculator.INTERVAL_DAYS_SUFFIX;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateCalculator.MUST_BE_WORKING_DAY_SUFFIX;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateCalculator.NON_WORKING_CALENDAR_SUFFIX;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateCalculator.NON_WORKING_DAYS_OF_WEEK_SUFFIX;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateCalculator.ORIGIN_SUFFIX;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateCalculator.SKIP_NON_WORKING_DAYS_SUFFIX;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateCalculator.TIME_SUFFIX;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.PublicHolidaysCollectionTest.CALENDAR_URI;

@SpringBootTest
@ActiveProfiles({"integration"})
public class IntermediateDateTypeConfiguratorTest {

    public static final LocalDateTime GIVEN_DATE = LocalDateTime.of(2022, 10, 13, 18, 0, 0);

    public static final LocalDateTime BST_DATE_BACKWARD = LocalDateTime.of(2022, 10, 26, 18, 0, 0);
    public static final LocalDateTime BST_DATE_FORWARD = LocalDateTime.of(2023, 3, 26, 18, 0, 0);

    public static final String PRIORITY_DATE_VALUE = GIVEN_DATE.plusDays(2)
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + "T16:00";

    public static final String INTERMEDIATE_DATE_VALUE = GIVEN_DATE.minusDays(2)
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + "T16:00";
    public static final String NEXT_HEARING_DATE_VALUE = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        + "T16:00";

    @Autowired
    private DateTypeConfigurator dateTypeConfigurator;

    @Test
    public void should_calculate_based_on_intermediate_date_origin_in_origin_earliest_dates() {
        ConfigurationDmnEvaluationResponse dueDateOriginEarliest = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("dueDateOriginEarliest"))
            .value(stringValue("nextHearingDuration,nextHearingDate,priorityDate"))
            .build();

        ConfigurationDmnEvaluationResponse priorityDateOriginEarliest = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("priorityDateOriginEarliest"))
            .value(stringValue("nextHearingDuration,nextHearingDate,dueDate"))
            .build();

        ConfigurationDmnEvaluationResponse calculatedDates = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("calculatedDates"))
            .value(stringValue("nextHearingDate,nextHearingDuration,dueDate,priorityDate"))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("nextHearingDate"))
            .value(stringValue(NEXT_HEARING_DATE_VALUE))
            .build();

        String intermediateDateName = "nextHearingDuration";
        ConfigurationDmnEvaluationResponse nextHearingDurationOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue(intermediateDateName + ORIGIN_SUFFIX))
            .value(stringValue(INTERMEDIATE_DATE_VALUE))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = readDueAndPriorityDateOriginFields(
            intermediateDateName, dueDateOriginEarliest, priorityDateOriginEarliest, calculatedDates,
            nextHearingDate, nextHearingDurationOrigin
        );

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses
            = dateTypeConfigurator.configureDates(evaluationResponses, false, false);

        assertThat(configurationDmnEvaluationResponses)
            .hasSize(5)
            .isEqualTo(List.of(
                ConfigurationDmnEvaluationResponse.builder()
                    .name(stringValue("calculatedDates"))
                    .value(stringValue("nextHearingDate,nextHearingDuration,dueDate,priorityDate"))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(stringValue("nextHearingDate"))
                    .value(stringValue("2022-10-13T16:00"))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(stringValue("nextHearingDuration"))
                    .value(stringValue("2022-10-19T21:00"))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(stringValue("dueDate"))
                    .value(stringValue("2022-10-18T17:00"))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(stringValue("priorityDate"))
                    .value(stringValue("2022-10-21T21:00"))
                    .build()
            ));
    }

    @Test
    public void should_calculate_based_on_intermediate_date_origin_in_single_origin_earliest_date() {
        ConfigurationDmnEvaluationResponse dueDateOriginEarliest = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("dueDateOriginEarliest"))
            .value(stringValue("nextHearingDate,nextHearingDuration,priorityDate"))
            .build();

        ConfigurationDmnEvaluationResponse priorityDate = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("priorityDate"))
            .value(stringValue(PRIORITY_DATE_VALUE))
            .build();

        var calculatedDates = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("calculatedDates"))
            .value(stringValue("nextHearingDate,nextHearingDuration,dueDate,priorityDate"))
            .build();

        var nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("nextHearingDate"))
            .value(stringValue(NEXT_HEARING_DATE_VALUE))
            .build();

        var intermediateDate = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("nextHearingDuration"))
            .value(stringValue(INTERMEDIATE_DATE_VALUE))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = readDueDateOriginFields(
            dueDateOriginEarliest, priorityDate, calculatedDates, nextHearingDate, intermediateDate);

        var configurationDmnEvaluationResponses
            = dateTypeConfigurator.configureDates(evaluationResponses, false, false);

        assertThat(configurationDmnEvaluationResponses)
            .hasSize(5)
            .isEqualTo(List.of(
                ConfigurationDmnEvaluationResponse.builder()
                    .name(stringValue("calculatedDates"))
                    .value(stringValue("nextHearingDate,nextHearingDuration,dueDate,priorityDate"))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(stringValue("nextHearingDate"))
                    .value(stringValue("2022-10-13T16:00"))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(stringValue("nextHearingDuration"))
                    .value(stringValue("2022-10-11T16:00"))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(stringValue("dueDate"))
                    .value(stringValue("2022-10-14T17:00"))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(stringValue("priorityDate"))
                    .value(stringValue("2022-10-15T16:00"))
                    .build()
            ));
    }

    @Test
    public void should_calculate_based_on_multiple_intermediate_date_origins() {
        ConfigurationDmnEvaluationResponse dueDateOriginEarliest = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("dueDateOriginEarliest"))
            .value(stringValue("nextHearingDate,nextHearingDuration,priorityDate"))
            .build();

        var calculatedDates = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("calculatedDates"))
            .value(stringValue("nextHearingDate,nextHearingDuration,dueDate,priorityIntermediateDate,priorityDate"))
            .build();

        ConfigurationDmnEvaluationResponse priorityDateOriginEarliest = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("priorityDateOriginEarliest"))
            .value(stringValue("priorityIntermediateDate,nextHearingDate,dueDate"))
            .build();


        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("nextHearingDate"))
            .value(stringValue(NEXT_HEARING_DATE_VALUE))
            .build();

        String intermediateDateName = "nextHearingDuration";
        ConfigurationDmnEvaluationResponse nextHearingDurationOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue(intermediateDateName + ORIGIN_SUFFIX))
            .value(stringValue(INTERMEDIATE_DATE_VALUE))
            .build();

        ConfigurationDmnEvaluationResponse priorityIntermediateDate = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("priorityIntermediateDate"))
            .value(stringValue(GIVEN_DATE.plusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + "T16:00"))
            .build();
        List<ConfigurationDmnEvaluationResponse> evaluationResponses = readDueAndPriorityDateOriginFields(
            intermediateDateName, dueDateOriginEarliest, priorityDateOriginEarliest, calculatedDates,
            nextHearingDate, nextHearingDurationOrigin, priorityIntermediateDate
        );

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses
            = dateTypeConfigurator.configureDates(evaluationResponses, false, false);

        assertThat(configurationDmnEvaluationResponses)
            .hasSize(6)
            .isEqualTo(List.of(
                ConfigurationDmnEvaluationResponse.builder()
                    .name(stringValue("calculatedDates"))
                    .value(stringValue(
                        "nextHearingDate,nextHearingDuration,dueDate,priorityIntermediateDate,priorityDate"))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(stringValue("nextHearingDate"))
                    .value(stringValue("2022-10-13T16:00"))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(stringValue("nextHearingDuration"))
                    .value(stringValue("2022-10-19T21:00"))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(stringValue("dueDate"))
                    .value(stringValue("2022-10-18T17:00"))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(stringValue("priorityIntermediateDate"))
                    .value(stringValue("2022-10-15T16:00"))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(stringValue("priorityDate"))
                    .value(stringValue("2022-10-21T21:00"))
                    .build()
            ));
    }

    private List<ConfigurationDmnEvaluationResponse> readDueAndPriorityDateOriginFields(
        String intermediateDateName,
        ConfigurationDmnEvaluationResponse... fields) {
        List<ConfigurationDmnEvaluationResponse> allFields = new ArrayList<>();
        allFields.addAll(getDueDateConfigurations());
        allFields.addAll(getPriorityDateConfigurations());
        allFields.addAll(getIntermediateDateConfigurations(intermediateDateName));
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
                .name(stringValue("dueDateIntervalDays"))
                .value(stringValue("3"))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(stringValue("dueDateNonWorkingCalendar"))
                .value(stringValue(CALENDAR_URI))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(stringValue("dueDateNonWorkingDaysOfWeek"))
                .value(stringValue("SATURDAY,SUNDAY"))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(stringValue("dueDateSkipNonWorkingDays"))
                .value(stringValue("true"))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(stringValue("dueDateMustBeWorkingDay"))
                .value(stringValue(DATE_TYPE_MUST_BE_WORKING_DAY_NEXT))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(stringValue("dueDateTime"))
                .value(stringValue("17:00"))
                .build()
        );
    }

    private List<ConfigurationDmnEvaluationResponse> getPriorityDateConfigurations() {
        return List.of(
            ConfigurationDmnEvaluationResponse.builder()
                .name(stringValue("priorityDateIntervalDays"))
                .value(stringValue("6"))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(stringValue("priorityDateNonWorkingCalendar"))
                .value(stringValue(CALENDAR_URI))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(stringValue("priorityDateNonWorkingDaysOfWeek"))
                .value(stringValue("SATURDAY,SUNDAY"))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(stringValue("priorityDateSkipNonWorkingDays"))
                .value(stringValue("true"))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(stringValue("priorityDateMustBeWorkingDay"))
                .value(stringValue(DATE_TYPE_MUST_BE_WORKING_DAY_NEXT))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(stringValue("priorityDateTime"))
                .value(stringValue("21:00"))
                .build()
        );
    }

    private List<ConfigurationDmnEvaluationResponse> getIntermediateDateConfigurations(String intermediateDateName) {
        return List.of(
            ConfigurationDmnEvaluationResponse.builder()
                .name(stringValue(intermediateDateName + INTERVAL_DAYS_SUFFIX))
                .value(stringValue("6"))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(stringValue(intermediateDateName + NON_WORKING_CALENDAR_SUFFIX))
                .value(stringValue(CALENDAR_URI))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(stringValue(intermediateDateName + NON_WORKING_DAYS_OF_WEEK_SUFFIX))
                .value(stringValue("SATURDAY,SUNDAY"))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(stringValue(intermediateDateName + SKIP_NON_WORKING_DAYS_SUFFIX))
                .value(stringValue("true"))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(stringValue(intermediateDateName + MUST_BE_WORKING_DAY_SUFFIX))
                .value(stringValue(DATE_TYPE_MUST_BE_WORKING_DAY_NEXT))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(stringValue(intermediateDateName + TIME_SUFFIX))
                .value(stringValue("21:00"))
                .build()
        );
    }

    @Test
    public void shouldCalculateDateWhenOriginDateIsBSTDueDateNonBSTBackword() {
        String localDateTime = BST_DATE_BACKWARD.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        //Clocks go back an hour at 2:00am
        ConfigurationDmnEvaluationResponse dueDateOriginRef = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("dueDateOriginEarliest"))
            .value(stringValue("nextHearingDate,priorityDate"))
            .build();

        String priorityDateValue = localDateTime + "T01:30";
        ConfigurationDmnEvaluationResponse priorityDate = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("priorityDate"))
            .value(stringValue(priorityDateValue))
            .build();
        String nextHearingDateValue = localDateTime + "T16:00";
        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("nextHearingDate"))
            .value(stringValue(nextHearingDateValue))
            .build();
        ConfigurationDmnEvaluationResponse dueDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("dueDateIntervalDays"))
            .value(stringValue("4"))
            .build();
        ConfigurationDmnEvaluationResponse dueDateNonWorkingCalendar = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("dueDateNonWorkingCalendar"))
            .value(stringValue("https://www.gov.uk/bank-holidays/england-and-wales.json"))
            .build();

        ConfigurationDmnEvaluationResponse dueDateNonWorkingDaysOfWeek = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("dueDateNonWorkingDaysOfWeek"))
            .value(stringValue(""))
            .build();
        ConfigurationDmnEvaluationResponse dueDateSkipNonWorkingDays = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("dueDateSkipNonWorkingDays"))
            .value(stringValue("false"))
            .build();
        ConfigurationDmnEvaluationResponse dueDateMustBeWorkingDay = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("dueDateMustBeWorkingDay"))
            .value(stringValue("false"))
            .build();

        ConfigurationDmnEvaluationResponse dueDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("dueDateTime"))
            .value(stringValue("02:30"))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDates(
                List.of(dueDateIntervalDays, dueDateNonWorkingCalendar, dueDateMustBeWorkingDay, priorityDate,
                        dueDateNonWorkingDaysOfWeek, dueDateSkipNonWorkingDays, dueDateOriginRef, dueDateTime,
                        nextHearingDate
                ),
                false, false
            );

        assertThat(configurationDmnEvaluationResponses).hasSize(3)
            .isEqualTo(List.of(
                ConfigurationDmnEvaluationResponse.builder()
                    .name(stringValue("nextHearingDate"))
                    .value(stringValue(nextHearingDateValue))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(stringValue("dueDate"))
                    .value(stringValue("2022-10-30T02:30"))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(stringValue("priorityDate"))
                    .value(stringValue(priorityDateValue))
                    .build()
            ));
    }

    @Test
    public void shouldCalculateDateWhenOriginDateIsBSTDueDateNonBSTForward() {
        String localDateTime = BST_DATE_FORWARD.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        //Clocks go forward an hour at 1:00am
        ConfigurationDmnEvaluationResponse dueDateOriginRef = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("dueDateOriginEarliest"))
            .value(stringValue("nextHearingDate,priorityDate"))
            .build();

        String priorityDateValue = localDateTime + "T00:30";
        ConfigurationDmnEvaluationResponse priorityDate = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("priorityDate"))
            .value(stringValue(priorityDateValue))
            .build();
        String nextHearingDateValue = localDateTime + "T16:00";
        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("nextHearingDate"))
            .value(stringValue(nextHearingDateValue))
            .build();
        ConfigurationDmnEvaluationResponse dueDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("dueDateIntervalDays"))
            .value(stringValue("4"))
            .build();
        ConfigurationDmnEvaluationResponse dueDateNonWorkingCalendar = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("dueDateNonWorkingCalendar"))
            .value(stringValue("https://www.gov.uk/bank-holidays/england-and-wales.json"))
            .build();

        ConfigurationDmnEvaluationResponse dueDateNonWorkingDaysOfWeek = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("dueDateNonWorkingDaysOfWeek"))
            .value(stringValue(""))
            .build();
        ConfigurationDmnEvaluationResponse dueDateSkipNonWorkingDays = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("dueDateSkipNonWorkingDays"))
            .value(stringValue("false"))
            .build();
        ConfigurationDmnEvaluationResponse dueDateMustBeWorkingDay = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("dueDateMustBeWorkingDay"))
            .value(stringValue("false"))
            .build();

        ConfigurationDmnEvaluationResponse dueDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("dueDateTime"))
            .value(stringValue("01:30"))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDates(
                List.of(dueDateIntervalDays, dueDateNonWorkingCalendar, dueDateMustBeWorkingDay, priorityDate,
                        dueDateNonWorkingDaysOfWeek, dueDateSkipNonWorkingDays, dueDateOriginRef, dueDateTime,
                        nextHearingDate
                ),
                false, false
            );

        assertThat(configurationDmnEvaluationResponses).hasSize(3)
            .isEqualTo(List.of(
                ConfigurationDmnEvaluationResponse.builder()
                    .name(stringValue("nextHearingDate"))
                    .value(stringValue(nextHearingDateValue))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(stringValue("dueDate"))
                    .value(stringValue("2023-03-30T01:30"))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(stringValue("priorityDate"))
                    .value(stringValue(priorityDateValue))
                    .build()
            ));
    }

    @Test
    public void shouldNotDefaultForTheIntermediateDateWhenNull() {
        ConfigurationDmnEvaluationResponse dueDateOriginEarliest = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("dueDateOriginEarliest"))
            .value(stringValue("nextHearingDuration,nextHearingDate,priorityDate"))
            .build();

        ConfigurationDmnEvaluationResponse priorityDateOriginEarliest = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("priorityDateOriginEarliest"))
            .value(stringValue("nextHearingDuration,nextHearingDate,dueDate"))
            .build();

        ConfigurationDmnEvaluationResponse calculatedDates = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("calculatedDates"))
            .value(stringValue("nextHearingDate,nextHearingDuration,dueDate,priorityDate"))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("nextHearingDate"))
            .value(stringValue(NEXT_HEARING_DATE_VALUE))
            .build();

        String intermediateDateName = "nextHearingDuration";
        ConfigurationDmnEvaluationResponse nextHearingDuration = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue(intermediateDateName + ORIGIN_SUFFIX))
            .value(stringValue(""))
            .build();
        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDates(
                List.of(dueDateOriginEarliest, priorityDateOriginEarliest, calculatedDates, nextHearingDate,
                        nextHearingDuration, nextHearingDate
                ),
                false, false
            );

        assertThat(configurationDmnEvaluationResponses).hasSize(4)
            .isEqualTo(List.of(
                ConfigurationDmnEvaluationResponse.builder()
                    .name(stringValue("calculatedDates"))
                    .value(stringValue("nextHearingDate,nextHearingDuration,dueDate,priorityDate"))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(stringValue("nextHearingDate"))
                    .value(stringValue(NEXT_HEARING_DATE_VALUE))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(stringValue("dueDate"))
                    .value(stringValue("2022-10-13T16:00"))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(stringValue("priorityDate"))
                    .value(stringValue(NEXT_HEARING_DATE_VALUE))
                    .build()
            ));
    }

    @Test
    public void shouldNotDefaultForTheIntermediateDateTimeISNotNull() {
        ConfigurationDmnEvaluationResponse dueDateOriginEarliest = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("dueDateOriginEarliest"))
            .value(stringValue("nextHearingDuration,nextHearingDate,priorityDate"))
            .build();

        ConfigurationDmnEvaluationResponse priorityDateOriginEarliest = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("priorityDateOriginEarliest"))
            .value(stringValue("nextHearingDuration,nextHearingDate,dueDate"))
            .build();

        ConfigurationDmnEvaluationResponse calculatedDates = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("calculatedDates"))
            .value(stringValue("nextHearingDate,nextHearingDuration,dueDate,priorityDate"))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("nextHearingDate"))
            .value(stringValue(NEXT_HEARING_DATE_VALUE))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDurationTime = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("nextHearingDurationTime"))
            .value(stringValue("16:00"))
            .build();
        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDates(
                List.of(dueDateOriginEarliest, priorityDateOriginEarliest, calculatedDates, nextHearingDate,
                        nextHearingDurationTime, nextHearingDate
                ),
                false, false
            );

        assertThat(configurationDmnEvaluationResponses).hasSize(4)
            .isEqualTo(List.of(
                ConfigurationDmnEvaluationResponse.builder()
                    .name(stringValue("calculatedDates"))
                    .value(stringValue("nextHearingDate,nextHearingDuration,dueDate,priorityDate"))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(stringValue("nextHearingDate"))
                    .value(stringValue(NEXT_HEARING_DATE_VALUE))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(stringValue("dueDate"))
                    .value(stringValue("2022-10-13T16:00"))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(stringValue("priorityDate"))
                    .value(stringValue(NEXT_HEARING_DATE_VALUE))
                    .build()
            ));
    }

    @Test
    public void shouldNotDefaultWhenIntermediateOriginRefElementIsNull() {
        ConfigurationDmnEvaluationResponse calculatedDates = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("calculatedDates"))
            .value(stringValue("nextHearingDate,nextHearingDuration,dueDate,priorityDate"))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDurationOriginRef = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("nextHearingDurationOriginRef"))
            .value(stringValue("nextHearingDate"))
            .build();
        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDates(List.of(calculatedDates, nextHearingDurationOriginRef), false, false);

        String calculateDate = DEFAULT_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + "T16:00";
        assertThat(configurationDmnEvaluationResponses).hasSize(3)
            .isEqualTo(List.of(
                ConfigurationDmnEvaluationResponse.builder()
                    .name(stringValue("calculatedDates"))
                    .value(stringValue("nextHearingDate,nextHearingDuration,dueDate,priorityDate"))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(stringValue("dueDate"))
                    .value(stringValue(calculateDate))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(stringValue("priorityDate"))
                    .value(stringValue(calculateDate))
                    .build()
            ));
    }

}
