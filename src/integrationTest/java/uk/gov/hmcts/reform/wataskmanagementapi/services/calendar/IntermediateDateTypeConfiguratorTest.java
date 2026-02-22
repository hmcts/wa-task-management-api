package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.wataskmanagementapi.config.IntegrationTest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.ConfigurationDmnEvaluationResponse;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.calendar.DateTypeIntervalData.DATE_TYPE_MUST_BE_WORKING_DAY_NEXT;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaValue.stringValue;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.CalendarTestSupport.CALENDAR_URI;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateCalculator.INTERVAL_DAYS_SUFFIX;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateCalculator.INVALID_DATE_REFERENCE_FIELD;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateCalculator.MUST_BE_WORKING_DAY_SUFFIX;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateCalculator.NON_WORKING_CALENDAR_SUFFIX;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateCalculator.NON_WORKING_DAYS_OF_WEEK_SUFFIX;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateCalculator.ORIGIN_SUFFIX;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateCalculator.SKIP_NON_WORKING_DAYS_SUFFIX;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateCalculator.TIME_SUFFIX;

@IntegrationTest
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
    private final Map<String, Object> taskAttributes = new HashMap<>();

    @Autowired
    private DateTypeConfigurator dateTypeConfigurator;

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void should_calculate_based_on_intermediate_date_origin_in_origin_earliest_dates(boolean configurable) {
        ConfigurationDmnEvaluationResponse dueDateOriginEarliest = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("dueDateOriginEarliest"))
            .value(stringValue("nextHearingDuration,nextHearingDate"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        ConfigurationDmnEvaluationResponse priorityDateOriginEarliest = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("priorityDateOriginEarliest"))
            .value(stringValue("nextHearingDuration,nextHearingDate,dueDate"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        ConfigurationDmnEvaluationResponse calculatedDates = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("calculatedDates"))
            .value(stringValue("nextHearingDate,nextHearingDuration,dueDate,priorityDate"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("nextHearingDate"))
            .value(stringValue(NEXT_HEARING_DATE_VALUE))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        String intermediateDateName = "nextHearingDuration";
        ConfigurationDmnEvaluationResponse nextHearingDurationOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue(intermediateDateName + ORIGIN_SUFFIX))
            .value(stringValue(INTERMEDIATE_DATE_VALUE))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = readDueAndPriorityDateOriginFields(
            configurable, intermediateDateName, dueDateOriginEarliest, priorityDateOriginEarliest, calculatedDates,
            nextHearingDate, nextHearingDurationOrigin
        );

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses
            = dateTypeConfigurator.configureDates(evaluationResponses, false, configurable, taskAttributes);

        assertThat(configurationDmnEvaluationResponses)
            .hasSize(4)
            .isEqualTo(List.of(
                ConfigurationDmnEvaluationResponse.builder()
                    .name(stringValue("nextHearingDate"))
                    .value(stringValue("2022-10-13T16:00"))
                    .canReconfigure(CamundaValue.booleanValue(configurable))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(stringValue("nextHearingDuration"))
                    .value(stringValue("2022-10-19T21:00"))
                    .canReconfigure(CamundaValue.booleanValue(configurable))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(stringValue("dueDate"))
                    .value(stringValue("2022-10-18T17:00"))
                    .canReconfigure(CamundaValue.booleanValue(configurable))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(stringValue("priorityDate"))
                    .value(stringValue("2022-10-21T21:00"))
                    .canReconfigure(CamundaValue.booleanValue(configurable))
                    .build()
            ));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void should_calculate_based_on_intermediate_date_origin_in_single_origin_earliest_date(
        boolean configurable) {
        ConfigurationDmnEvaluationResponse dueDateOriginEarliest = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("dueDateOriginEarliest"))
            .value(stringValue("nextHearingDate,nextHearingDuration"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        ConfigurationDmnEvaluationResponse priorityDate = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("priorityDate"))
            .value(stringValue(PRIORITY_DATE_VALUE))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        var calculatedDates = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("calculatedDates"))
            .value(stringValue("nextHearingDate,nextHearingDuration,dueDate,priorityDate"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        var nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("nextHearingDate"))
            .value(stringValue(NEXT_HEARING_DATE_VALUE))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        var intermediateDate = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("nextHearingDuration"))
            .value(stringValue(INTERMEDIATE_DATE_VALUE))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = readDueDateOriginFields(
            configurable,
            dueDateOriginEarliest,
            priorityDate,
            calculatedDates,
            nextHearingDate,
            intermediateDate
        );

        var configurationDmnEvaluationResponses
            = dateTypeConfigurator.configureDates(evaluationResponses, false, configurable, taskAttributes);

        assertThat(configurationDmnEvaluationResponses)
            .hasSize(4)
            .isEqualTo(List.of(
                ConfigurationDmnEvaluationResponse.builder()
                    .name(stringValue("nextHearingDate"))
                    .value(stringValue("2022-10-13T16:00"))
                    .canReconfigure(CamundaValue.booleanValue(configurable))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(stringValue("nextHearingDuration"))
                    .value(stringValue("2022-10-11T16:00"))
                    .canReconfigure(CamundaValue.booleanValue(configurable))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(stringValue("dueDate"))
                    .value(stringValue("2022-10-14T17:00"))
                    .canReconfigure(CamundaValue.booleanValue(configurable))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(stringValue("priorityDate"))
                    .value(stringValue("2022-10-15T16:00"))
                    .canReconfigure(CamundaValue.booleanValue(configurable))
                    .build()
            ));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void should_calculate_based_on_multiple_intermediate_date_origins(boolean configurable) {
        ConfigurationDmnEvaluationResponse dueDateOriginEarliest = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("dueDateOriginEarliest"))
            .value(stringValue("nextHearingDate,nextHearingDuration"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        String calculatedDatesValue = "nextHearingDate,nextHearingDuration,dueDate,"
            + "priorityIntermediateDate,priorityDate";
        var calculatedDates = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("calculatedDates"))
            .value(stringValue(calculatedDatesValue))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        ConfigurationDmnEvaluationResponse priorityDateOriginEarliest = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("priorityDateOriginEarliest"))
            .value(stringValue("priorityIntermediateDate,nextHearingDate,dueDate"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();


        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("nextHearingDate"))
            .value(stringValue(NEXT_HEARING_DATE_VALUE))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        String intermediateDateName = "nextHearingDuration";
        ConfigurationDmnEvaluationResponse nextHearingDurationOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue(intermediateDateName + ORIGIN_SUFFIX))
            .value(stringValue(INTERMEDIATE_DATE_VALUE))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        ConfigurationDmnEvaluationResponse priorityIntermediateDate = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("priorityIntermediateDate"))
            .value(stringValue(GIVEN_DATE.plusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + "T16:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();
        List<ConfigurationDmnEvaluationResponse> evaluationResponses = readDueAndPriorityDateOriginFields(
            configurable, intermediateDateName, dueDateOriginEarliest, priorityDateOriginEarliest, calculatedDates,
            nextHearingDate, nextHearingDurationOrigin, priorityIntermediateDate
        );

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses
            = dateTypeConfigurator.configureDates(evaluationResponses, false, configurable, taskAttributes);

        assertThat(configurationDmnEvaluationResponses)
            .hasSize(5)
            .isEqualTo(List.of(
                ConfigurationDmnEvaluationResponse.builder()
                    .name(stringValue("nextHearingDate"))
                    .value(stringValue("2022-10-13T16:00"))
                    .canReconfigure(CamundaValue.booleanValue(configurable))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(stringValue("nextHearingDuration"))
                    .value(stringValue("2022-10-19T21:00"))
                    .canReconfigure(CamundaValue.booleanValue(configurable))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(stringValue("dueDate"))
                    .value(stringValue("2022-10-18T17:00"))
                    .canReconfigure(CamundaValue.booleanValue(configurable))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(stringValue("priorityIntermediateDate"))
                    .value(stringValue("2022-10-15T16:00"))
                    .canReconfigure(CamundaValue.booleanValue(configurable))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(stringValue("priorityDate"))
                    .value(stringValue("2022-10-21T21:00"))
                    .canReconfigure(CamundaValue.booleanValue(configurable))
                    .build()
            ));
    }

    private List<ConfigurationDmnEvaluationResponse> readDueAndPriorityDateOriginFields(
        boolean configurable,
        String intermediateDateName,
        ConfigurationDmnEvaluationResponse... fields) {
        List<ConfigurationDmnEvaluationResponse> allFields = new ArrayList<>();
        allFields.addAll(getDueDateConfigurations(configurable));
        allFields.addAll(getPriorityDateConfigurations(configurable));
        allFields.addAll(getIntermediateDateConfigurations(intermediateDateName, configurable));
        allFields.addAll(List.of(fields));
        return allFields;
    }

    private List<ConfigurationDmnEvaluationResponse> readDueDateOriginFields(
        boolean configurable,
        ConfigurationDmnEvaluationResponse... fields) {
        List<ConfigurationDmnEvaluationResponse> allFields = new ArrayList<>();
        allFields.addAll(getDueDateConfigurations(configurable));
        allFields.addAll(List.of(fields));
        return allFields;
    }

    private List<ConfigurationDmnEvaluationResponse> getDueDateConfigurations(boolean configurable) {
        return List.of(
            ConfigurationDmnEvaluationResponse.builder()
                .name(stringValue("dueDateIntervalDays"))
                .value(stringValue("3"))
                .canReconfigure(CamundaValue.booleanValue(configurable))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(stringValue("dueDateNonWorkingCalendar"))
                .value(stringValue(CALENDAR_URI))
                .canReconfigure(CamundaValue.booleanValue(configurable))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(stringValue("dueDateNonWorkingDaysOfWeek"))
                .value(stringValue("SATURDAY,SUNDAY"))
                .canReconfigure(CamundaValue.booleanValue(configurable))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(stringValue("dueDateSkipNonWorkingDays"))
                .value(stringValue("true"))
                .canReconfigure(CamundaValue.booleanValue(configurable))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(stringValue("dueDateMustBeWorkingDay"))
                .value(stringValue(DATE_TYPE_MUST_BE_WORKING_DAY_NEXT))
                .canReconfigure(CamundaValue.booleanValue(configurable))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(stringValue("dueDateTime"))
                .value(stringValue("17:00"))
                .canReconfigure(CamundaValue.booleanValue(configurable))
                .build()
        );
    }

    private List<ConfigurationDmnEvaluationResponse> getPriorityDateConfigurations(boolean configurable) {
        return List.of(
            ConfigurationDmnEvaluationResponse.builder()
                .name(stringValue("priorityDateIntervalDays"))
                .value(stringValue("6"))
                .canReconfigure(CamundaValue.booleanValue(configurable))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(stringValue("priorityDateNonWorkingCalendar"))
                .value(stringValue(CALENDAR_URI))
                .canReconfigure(CamundaValue.booleanValue(configurable))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(stringValue("priorityDateNonWorkingDaysOfWeek"))
                .value(stringValue("SATURDAY,SUNDAY"))
                .canReconfigure(CamundaValue.booleanValue(configurable))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(stringValue("priorityDateSkipNonWorkingDays"))
                .value(stringValue("true"))
                .canReconfigure(CamundaValue.booleanValue(configurable))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(stringValue("priorityDateMustBeWorkingDay"))
                .value(stringValue(DATE_TYPE_MUST_BE_WORKING_DAY_NEXT))
                .canReconfigure(CamundaValue.booleanValue(configurable))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(stringValue("priorityDateTime"))
                .value(stringValue("21:00"))
                .canReconfigure(CamundaValue.booleanValue(configurable))
                .build()
        );
    }

    private List<ConfigurationDmnEvaluationResponse> getIntermediateDateConfigurations(
        String intermediateDateName, boolean configurable) {
        return List.of(
            ConfigurationDmnEvaluationResponse.builder()
                .name(stringValue(intermediateDateName + INTERVAL_DAYS_SUFFIX))
                .value(stringValue("6"))
                .canReconfigure(CamundaValue.booleanValue(configurable))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(stringValue(intermediateDateName + NON_WORKING_CALENDAR_SUFFIX))
                .value(stringValue(CALENDAR_URI))
                .canReconfigure(CamundaValue.booleanValue(configurable))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(stringValue(intermediateDateName + NON_WORKING_DAYS_OF_WEEK_SUFFIX))
                .value(stringValue("SATURDAY,SUNDAY"))
                .canReconfigure(CamundaValue.booleanValue(configurable))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(stringValue(intermediateDateName + SKIP_NON_WORKING_DAYS_SUFFIX))
                .value(stringValue("true"))
                .canReconfigure(CamundaValue.booleanValue(configurable))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(stringValue(intermediateDateName + MUST_BE_WORKING_DAY_SUFFIX))
                .value(stringValue(DATE_TYPE_MUST_BE_WORKING_DAY_NEXT))
                .canReconfigure(CamundaValue.booleanValue(configurable))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(stringValue(intermediateDateName + TIME_SUFFIX))
                .value(stringValue("21:00"))
                .canReconfigure(CamundaValue.booleanValue(configurable))
                .build()
        );
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void shouldCalculateDateWhenOriginDateIsBSTDueDateNonBSTBackword(boolean configurable) {
        String localDateTime = BST_DATE_BACKWARD.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        //Clocks go back an hour at 2:00am
        ConfigurationDmnEvaluationResponse dueDateOriginRef = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("dueDateOriginEarliest"))
            .value(stringValue("nextHearingDate"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        String priorityDateValue = localDateTime + "T01:30";
        ConfigurationDmnEvaluationResponse priorityDate = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("priorityDate"))
            .value(stringValue(priorityDateValue))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();
        String nextHearingDateValue = localDateTime + "T16:00";
        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("nextHearingDate"))
            .value(stringValue(nextHearingDateValue))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();
        ConfigurationDmnEvaluationResponse dueDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("dueDateIntervalDays"))
            .value(stringValue("4"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();
        ConfigurationDmnEvaluationResponse dueDateNonWorkingCalendar = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("dueDateNonWorkingCalendar"))
            .value(stringValue(CALENDAR_URI))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        ConfigurationDmnEvaluationResponse dueDateNonWorkingDaysOfWeek = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("dueDateNonWorkingDaysOfWeek"))
            .value(stringValue(""))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();
        ConfigurationDmnEvaluationResponse dueDateSkipNonWorkingDays = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("dueDateSkipNonWorkingDays"))
            .value(stringValue("false"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();
        ConfigurationDmnEvaluationResponse dueDateMustBeWorkingDay = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("dueDateMustBeWorkingDay"))
            .value(stringValue("false"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        ConfigurationDmnEvaluationResponse dueDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("dueDateTime"))
            .value(stringValue("02:30"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDates(
                List.of(dueDateIntervalDays, dueDateNonWorkingCalendar, dueDateMustBeWorkingDay, priorityDate,
                        dueDateNonWorkingDaysOfWeek, dueDateSkipNonWorkingDays, dueDateOriginRef, dueDateTime,
                        nextHearingDate
                ),
                false, configurable,
                taskAttributes
            );

        assertThat(configurationDmnEvaluationResponses).hasSize(3)
            .isEqualTo(List.of(
                ConfigurationDmnEvaluationResponse.builder()
                    .name(stringValue("nextHearingDate"))
                    .value(stringValue(nextHearingDateValue))
                    .canReconfigure(CamundaValue.booleanValue(configurable))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(stringValue("dueDate"))
                    .value(stringValue("2022-10-30T02:30"))
                    .canReconfigure(CamundaValue.booleanValue(configurable))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(stringValue("priorityDate"))
                    .value(stringValue(priorityDateValue))
                    .canReconfigure(CamundaValue.booleanValue(configurable))
                    .build()
            ));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void shouldCalculateDateWhenOriginDateIsBSTDueDateNonBSTForward(boolean configurable) {
        String localDateTime = BST_DATE_FORWARD.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        //Clocks go forward an hour at 1:00am
        ConfigurationDmnEvaluationResponse dueDateOriginRef = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("dueDateOriginEarliest"))
            .value(stringValue("nextHearingDate"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        String priorityDateValue = localDateTime + "T00:30";
        ConfigurationDmnEvaluationResponse priorityDate = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("priorityDate"))
            .value(stringValue(priorityDateValue))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();
        String nextHearingDateValue = localDateTime + "T16:00";
        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("nextHearingDate"))
            .value(stringValue(nextHearingDateValue))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();
        ConfigurationDmnEvaluationResponse dueDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("dueDateIntervalDays"))
            .value(stringValue("4"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();
        ConfigurationDmnEvaluationResponse dueDateNonWorkingCalendar = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("dueDateNonWorkingCalendar"))
            .value(stringValue(CALENDAR_URI))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        ConfigurationDmnEvaluationResponse dueDateNonWorkingDaysOfWeek = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("dueDateNonWorkingDaysOfWeek"))
            .value(stringValue(""))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();
        ConfigurationDmnEvaluationResponse dueDateSkipNonWorkingDays = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("dueDateSkipNonWorkingDays"))
            .value(stringValue("false"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();
        ConfigurationDmnEvaluationResponse dueDateMustBeWorkingDay = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("dueDateMustBeWorkingDay"))
            .value(stringValue("false"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        ConfigurationDmnEvaluationResponse dueDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("dueDateTime"))
            .value(stringValue("01:30"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDates(
                List.of(dueDateIntervalDays, dueDateNonWorkingCalendar, dueDateMustBeWorkingDay, priorityDate,
                        dueDateNonWorkingDaysOfWeek, dueDateSkipNonWorkingDays, dueDateOriginRef, dueDateTime,
                        nextHearingDate
                ),
                false, false,
                taskAttributes
            );

        assertThat(configurationDmnEvaluationResponses).hasSize(3)
            .isEqualTo(List.of(
                ConfigurationDmnEvaluationResponse.builder()
                    .name(stringValue("nextHearingDate"))
                    .value(stringValue(nextHearingDateValue))
                    .canReconfigure(CamundaValue.booleanValue(false))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(stringValue("dueDate"))
                    .value(stringValue("2023-03-30T01:30"))
                    .canReconfigure(CamundaValue.booleanValue(false))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(stringValue("priorityDate"))
                    .value(stringValue(priorityDateValue))
                    .canReconfigure(CamundaValue.booleanValue(false))
                    .build()
            ));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void shouldNotDefaultForTheIntermediateDateWhenIntermediateAttributesAreBlank(boolean configurable) {
        ConfigurationDmnEvaluationResponse dueDateOriginEarliest = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("dueDateOriginEarliest"))
            .value(stringValue("nextHearingDuration,nextHearingDate"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        ConfigurationDmnEvaluationResponse priorityDateOriginEarliest = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("priorityDateOriginEarliest"))
            .value(stringValue("nextHearingDuration,nextHearingDate,dueDate"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        ConfigurationDmnEvaluationResponse calculatedDates = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("calculatedDates"))
            .value(stringValue("nextHearingDate,nextHearingDuration,dueDate,priorityDate"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("nextHearingDate"))
            .value(stringValue(NEXT_HEARING_DATE_VALUE))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses
            = dateTypeConfigurator.configureDates(
            List.of(dueDateOriginEarliest, priorityDateOriginEarliest,
                    calculatedDates, nextHearingDate, nextHearingDate
            ),
            false,
            configurable,
            taskAttributes
        );
        assertThat(configurationDmnEvaluationResponses).hasSize(3)
            .isEqualTo(List.of(
                ConfigurationDmnEvaluationResponse.builder()
                    .name(stringValue("nextHearingDate"))
                    .value(stringValue("2022-10-13T16:00"))
                    .canReconfigure(CamundaValue.booleanValue(configurable))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(stringValue("dueDate"))
                    .value(stringValue("2022-10-13T16:00"))
                    .canReconfigure(CamundaValue.booleanValue(configurable))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(stringValue("priorityDate"))
                    .value(stringValue("2022-10-13T16:00"))
                    .canReconfigure(CamundaValue.booleanValue(configurable))
                    .build()
            ));


    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void shouldNotDefaultForTheIntermediateDateTimeIsNotNull(boolean configurable) {
        ConfigurationDmnEvaluationResponse dueDateOriginEarliest = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("dueDateOriginEarliest"))
            .value(stringValue("nextHearingDuration,nextHearingDate"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        ConfigurationDmnEvaluationResponse priorityDateOriginEarliest = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("priorityDateOriginEarliest"))
            .value(stringValue("nextHearingDuration,nextHearingDate,dueDate"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        ConfigurationDmnEvaluationResponse calculatedDates = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("calculatedDates"))
            .value(stringValue("nextHearingDate,nextHearingDuration,dueDate,priorityDate"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("nextHearingDate"))
            .value(stringValue(NEXT_HEARING_DATE_VALUE))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDurationTime = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("nextHearingDurationTime"))
            .value(stringValue("16:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        assertThat(dateTypeConfigurator.configureDates(
            List.of(dueDateOriginEarliest, priorityDateOriginEarliest, nextHearingDurationTime,
                    calculatedDates, nextHearingDate, nextHearingDate
            ),
            false,
            configurable,
            taskAttributes
        )).hasSize(3)
            .isEqualTo(List.of(
                ConfigurationDmnEvaluationResponse.builder()
                    .name(stringValue("nextHearingDate"))
                    .value(stringValue("2022-10-13T16:00"))
                    .canReconfigure(CamundaValue.booleanValue(configurable))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(stringValue("dueDate"))
                    .value(stringValue("2022-10-13T16:00"))
                    .canReconfigure(CamundaValue.booleanValue(configurable))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(stringValue("priorityDate"))
                    .value(stringValue("2022-10-13T16:00"))
                    .canReconfigure(CamundaValue.booleanValue(configurable))
                    .build()
            ));
    }

    @Test
    public void shouldThrowWhenIntermediateOriginRefElementIsNotYetCalculatedBasedOnOrder() {
        ConfigurationDmnEvaluationResponse calculatedDates = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("calculatedDates"))
            .value(stringValue("nextHearingDate,nextHearingDuration,dueDate,priorityDate"))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDurationOriginRef = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("nextHearingDurationOriginRef"))
            .value(stringValue("dueDate"))
            .build();

        assertThatThrownBy(() -> dateTypeConfigurator
            .configureDates(
                List.of(calculatedDates, nextHearingDurationOriginRef),
                false,
                false,
                taskAttributes
            ))
            .isInstanceOf(RuntimeException.class)
            .hasMessage(String.format(INVALID_DATE_REFERENCE_FIELD, "dueDate"));
    }

    @Test
    public void shouldNotRecalculateDateWhenDueDateIsUnconfigurableButIntermediateIsConfigurable() {
        String dueDateValue = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String dueDateDurationValue = GIVEN_DATE.plusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse dueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(dueDateValue + "T18:00"))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        ConfigurationDmnEvaluationResponse dueDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateDuration"))
            .value(CamundaValue.stringValue(dueDateDurationValue + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDates(List.of(dueDate, dueDateOrigin), false, true, taskAttributes);

        assertThat(configurationDmnEvaluationResponses).isEmpty();
    }

    @Test
    public void shouldNotRecalculateDateWhenPriorityDateIsUnconfigurableButIntermediateIsConfigurable() {
        String dueDateValue = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String dueDateDurationValue = GIVEN_DATE.plusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse priorityDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDate"))
            .value(CamundaValue.stringValue(dueDateValue + "T18:00"))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        ConfigurationDmnEvaluationResponse priorityDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateDuration"))
            .value(CamundaValue.stringValue(dueDateDurationValue + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDates(List.of(priorityDate, priorityDateOrigin), false, true, taskAttributes);

        assertThat(configurationDmnEvaluationResponses).isEmpty();
    }

    @Test
    public void shouldNotRecalculateDateWhenNextHearingDateIsUnconfigurableButIntermediateIsConfigurable() {
        String dueDateValue = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String dueDateDurationValue = GIVEN_DATE.plusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(dueDateValue + "T18:00"))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateDuration"))
            .value(CamundaValue.stringValue(dueDateDurationValue + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDates(List.of(nextHearingDate, nextHearingDateOrigin), false, true, taskAttributes);

        assertThat(configurationDmnEvaluationResponses).isEmpty();
    }

    @Test
    @DisplayName("Intermediate date and its all parameters are not set to be reconfigurable")
    public void shouldNotCalculateIntermediateDateWhenNotSetToReconfigurableIncludingAllParameters() {
        String nextHearingDateValue = GIVEN_DATE.plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse calculatedDates = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("calculatedDates"))
            .value(stringValue("nextHearingDate,hearingDatePreDate,dueDate,priorityDate"))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(nextHearingDateValue))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse hearingDatePreDateOriginRef = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("hearingDatePreDateOriginRef"))
            .value(CamundaValue.stringValue("nextHearingDate"))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        ConfigurationDmnEvaluationResponse hearingDatePreDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("hearingDatePreDateIntervalDays"))
            .value(CamundaValue.stringValue("-5"))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        ConfigurationDmnEvaluationResponse hearingDatePreDateNonWorkingCalendar = ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue("hearingDatePreDateNonWorkingCalendar"))
            .value(CamundaValue.stringValue(CALENDAR_URI))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        ConfigurationDmnEvaluationResponse hearingDatePreDateNonWorkingDaysOfWeek = ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue("hearingDatePreDateNonWorkingDaysOfWeek"))
            .value(CamundaValue.stringValue("SATURDAY,SUNDAY"))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        ConfigurationDmnEvaluationResponse hearingDatePreDateSkipNonWorkingDays = ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue("hearingDatePreDateSkipNonWorkingDays"))
            .value(CamundaValue.stringValue("true"))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        ConfigurationDmnEvaluationResponse hearingDatePreDateMustBeWorkingDay = ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue("hearingDatePreDateMustBeWorkingDay"))
            .value(CamundaValue.stringValue(DATE_TYPE_MUST_BE_WORKING_DAY_NEXT))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        ConfigurationDmnEvaluationResponse dueDateOriginRef = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOriginRef"))
            .value(CamundaValue.stringValue("nextHearingDate"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse dueDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateIntervalDays"))
            .value(CamundaValue.stringValue("21"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse dueDateNonWorkingCalendar = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateNonWorkingCalendar"))
            .value(CamundaValue.stringValue(CALENDAR_URI))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse dueDateNonWorkingDaysOfWeek = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateNonWorkingDaysOfWeek"))
            .value(CamundaValue.stringValue("SATURDAY,SUNDAY"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse dueDateSkipNonWorkingDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateSkipNonWorkingDays"))
            .value(CamundaValue.stringValue("true"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse dueDateMustBeWorkingDay = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateMustBeWorkingDay"))
            .value(CamundaValue.stringValue(DATE_TYPE_MUST_BE_WORKING_DAY_NEXT))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse priorityDateOriginEarliest = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateOriginEarliest"))
            .value(CamundaValue.stringValue("hearingDatePreDate,dueDate"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDates(
                List.of(calculatedDates, nextHearingDate, hearingDatePreDateOriginRef,
                        hearingDatePreDateIntervalDays, hearingDatePreDateNonWorkingCalendar,
                        hearingDatePreDateNonWorkingDaysOfWeek, hearingDatePreDateSkipNonWorkingDays,
                        hearingDatePreDateMustBeWorkingDay, dueDateOriginRef, dueDateIntervalDays,
                        dueDateNonWorkingCalendar, dueDateMustBeWorkingDay, dueDateNonWorkingDaysOfWeek,
                        dueDateSkipNonWorkingDays, priorityDateOriginEarliest
                ), false, true, taskAttributes
            );

        assertThat(configurationDmnEvaluationResponses).hasSize(4)
            .isEqualTo(List.of(
                ConfigurationDmnEvaluationResponse.builder()
                    .name(stringValue("nextHearingDate"))
                    .value(stringValue(nextHearingDateValue + "T16:00"))
                    .canReconfigure(CamundaValue.booleanValue(true))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(stringValue("hearingDatePreDate"))
                    .value(stringValue(nextHearingDateValue + "T16:00"))
                    .canReconfigure(CamundaValue.booleanValue(true))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(stringValue("dueDate"))
                    .value(stringValue(GIVEN_DATE.plusDays(32)
                                           .format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + "T16:00"))
                    .canReconfigure(CamundaValue.booleanValue(true))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(stringValue("priorityDate"))
                    .value(stringValue(nextHearingDateValue + "T16:00"))
                    .canReconfigure(CamundaValue.booleanValue(true))
                    .build()
            ));
    }

    @Test
    @DisplayName("Intermediate date is not set to be reconfigurable but its parameters are reconfigurable")
    public void shouldCalculateIntermediateDateWhenIsNotSetToBeReconfigurableWhenParametersAreReconfigurable() {
        String nextHearingDateValue = GIVEN_DATE.plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse calculatedDates = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("calculatedDates"))
            .value(stringValue("nextHearingDate,hearingDatePreDate,dueDate,priorityDate"))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(nextHearingDateValue))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse hearingDatePreDateOriginRef = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("hearingDatePreDateOriginRef"))
            .value(CamundaValue.stringValue("nextHearingDate"))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        ConfigurationDmnEvaluationResponse hearingDatePreDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("hearingDatePreDateIntervalDays"))
            .value(CamundaValue.stringValue("-5"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse hearingDatePreDateNonWorkingCalendar = ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue("hearingDatePreDateNonWorkingCalendar"))
            .value(CamundaValue.stringValue(CALENDAR_URI))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse hearingDatePreDateNonWorkingDaysOfWeek = ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue("hearingDatePreDateNonWorkingDaysOfWeek"))
            .value(CamundaValue.stringValue("SATURDAY,SUNDAY"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse hearingDatePreDateSkipNonWorkingDays = ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue("hearingDatePreDateSkipNonWorkingDays"))
            .value(CamundaValue.stringValue("true"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse hearingDatePreDateMustBeWorkingDay = ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue("hearingDatePreDateMustBeWorkingDay"))
            .value(CamundaValue.stringValue(DATE_TYPE_MUST_BE_WORKING_DAY_NEXT))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse dueDateOriginRef = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOriginRef"))
            .value(CamundaValue.stringValue("nextHearingDate"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse dueDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateIntervalDays"))
            .value(CamundaValue.stringValue("21"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse dueDateNonWorkingCalendar = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateNonWorkingCalendar"))
            .value(CamundaValue.stringValue(CALENDAR_URI))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse dueDateNonWorkingDaysOfWeek = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateNonWorkingDaysOfWeek"))
            .value(CamundaValue.stringValue("SATURDAY,SUNDAY"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse dueDateSkipNonWorkingDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateSkipNonWorkingDays"))
            .value(CamundaValue.stringValue("true"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse dueDateMustBeWorkingDay = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateMustBeWorkingDay"))
            .value(CamundaValue.stringValue(DATE_TYPE_MUST_BE_WORKING_DAY_NEXT))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse priorityDateOriginEarliest = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateOriginEarliest"))
            .value(CamundaValue.stringValue("hearingDatePreDate,dueDate"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDates(
                List.of(calculatedDates, nextHearingDate, hearingDatePreDateOriginRef,
                        hearingDatePreDateIntervalDays, hearingDatePreDateNonWorkingCalendar,
                        hearingDatePreDateNonWorkingDaysOfWeek, hearingDatePreDateSkipNonWorkingDays,
                        hearingDatePreDateMustBeWorkingDay, dueDateOriginRef, dueDateIntervalDays,
                        dueDateNonWorkingCalendar, dueDateMustBeWorkingDay, dueDateNonWorkingDaysOfWeek,
                        dueDateSkipNonWorkingDays, priorityDateOriginEarliest
                ), false, true, taskAttributes
            );

        assertThat(configurationDmnEvaluationResponses).hasSize(4)
            .isEqualTo(List.of(
                ConfigurationDmnEvaluationResponse.builder()
                    .name(stringValue("nextHearingDate"))
                    .value(stringValue(nextHearingDateValue + "T16:00"))
                    .canReconfigure(CamundaValue.booleanValue(true))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(stringValue("hearingDatePreDate"))
                    .value(stringValue(GIVEN_DATE.minusDays(6)
                                           .format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + "T16:00"))
                    .canReconfigure(CamundaValue.booleanValue(true))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(stringValue("dueDate"))
                    .value(stringValue(GIVEN_DATE.plusDays(32)
                                           .format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + "T16:00"))
                    .canReconfigure(CamundaValue.booleanValue(true))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(stringValue("priorityDate"))
                    .value(stringValue(GIVEN_DATE.minusDays(6)
                                           .format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + "T16:00"))
                    .canReconfigure(CamundaValue.booleanValue(true))
                    .build()
            ));
    }

    @Test
    @DisplayName("Intermediate date and its all parameters are set to be reconfigurable")
    public void shouldCalculateIntermediateDateWhenIsSetToBeReconfigurableIncludingAllParameters() {
        String nextHearingDateValue = GIVEN_DATE.plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse calculatedDates = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("calculatedDates"))
            .value(stringValue("nextHearingDate,hearingDatePreDate,dueDate,priorityDate"))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(nextHearingDateValue))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse hearingDatePreDateOriginRef = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("hearingDatePreDateOriginRef"))
            .value(CamundaValue.stringValue("nextHearingDate"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse hearingDatePreDateIntervalDays = ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue("hearingDatePreDateIntervalDays"))
            .value(CamundaValue.stringValue("10"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse hearingDatePreDateNonWorkingCalendar = ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue("hearingDatePreDateNonWorkingCalendar"))
            .value(CamundaValue.stringValue(CALENDAR_URI))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse hearingDatePreDateNonWorkingDaysOfWeek = ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue("hearingDatePreDateNonWorkingDaysOfWeek"))
            .value(CamundaValue.stringValue("SATURDAY,SUNDAY"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse hearingDatePreDateSkipNonWorkingDays = ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue("hearingDatePreDateSkipNonWorkingDays"))
            .value(CamundaValue.stringValue("true"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse hearingDatePreDateMustBeWorkingDay = ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue("hearingDatePreDateMustBeWorkingDay"))
            .value(CamundaValue.stringValue(DATE_TYPE_MUST_BE_WORKING_DAY_NEXT))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse dueDateOriginRef = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOriginRef"))
            .value(CamundaValue.stringValue("nextHearingDate"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse dueDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateIntervalDays"))
            .value(CamundaValue.stringValue("21"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse dueDateNonWorkingCalendar = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateNonWorkingCalendar"))
            .value(CamundaValue.stringValue(CALENDAR_URI))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse dueDateNonWorkingDaysOfWeek = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateNonWorkingDaysOfWeek"))
            .value(CamundaValue.stringValue("SATURDAY,SUNDAY"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse dueDateSkipNonWorkingDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateSkipNonWorkingDays"))
            .value(CamundaValue.stringValue("true"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse dueDateMustBeWorkingDay = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateMustBeWorkingDay"))
            .value(CamundaValue.stringValue(DATE_TYPE_MUST_BE_WORKING_DAY_NEXT))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse priorityDateOriginEarliest = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateOriginEarliest"))
            .value(CamundaValue.stringValue("hearingDatePreDate,dueDate"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDates(
                List.of(calculatedDates, nextHearingDate, hearingDatePreDateOriginRef,
                        hearingDatePreDateIntervalDays, hearingDatePreDateNonWorkingCalendar,
                        hearingDatePreDateNonWorkingDaysOfWeek, hearingDatePreDateSkipNonWorkingDays,
                        hearingDatePreDateMustBeWorkingDay, dueDateOriginRef, dueDateIntervalDays,
                        dueDateNonWorkingCalendar, dueDateMustBeWorkingDay, dueDateNonWorkingDaysOfWeek,
                        dueDateSkipNonWorkingDays, priorityDateOriginEarliest
                ), false, true, taskAttributes
            );

        assertThat(configurationDmnEvaluationResponses).hasSize(4)
            .isEqualTo(List.of(
                ConfigurationDmnEvaluationResponse.builder()
                    .name(stringValue("nextHearingDate"))
                    .value(stringValue(nextHearingDateValue + "T16:00"))
                    .canReconfigure(CamundaValue.booleanValue(true))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(stringValue("hearingDatePreDate"))
                    .value(stringValue(GIVEN_DATE.plusDays(15)
                                           .format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + "T16:00"))
                    .canReconfigure(CamundaValue.booleanValue(true))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(stringValue("dueDate"))
                    .value(stringValue(GIVEN_DATE.plusDays(32)
                                           .format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + "T16:00"))
                    .canReconfigure(CamundaValue.booleanValue(true))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(stringValue("priorityDate"))
                    .value(stringValue(GIVEN_DATE.plusDays(15)
                                           .format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + "T16:00"))
                    .canReconfigure(CamundaValue.booleanValue(true))
                    .build()
            ));
    }

    @Test
    @DisplayName("Intermediate date calculation fails due to unspecified intermediate date in DMN")
    public void shouldFailAndReturnEmptyIntermediateDateDueToUnspecifiedIntermediateDateInDmn() {
        String nextHearingDateValue = GIVEN_DATE.plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse calculatedDates = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("calculatedDates"))
            .value(stringValue("nextHearingDate,nonSpecifiedIntDate,hearingDatePreDate,dueDate,priorityDate"))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(nextHearingDateValue))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse hearingDatePreDateOriginRef = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("hearingDatePreDateOriginRef"))
            .value(CamundaValue.stringValue("nonSpecifiedIntDate"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse hearingDatePreDateIntervalDays = ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue("hearingDatePreDateIntervalDays"))
            .value(CamundaValue.stringValue("5"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse hearingDatePreDateNonWorkingCalendar = ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue("hearingDatePreDateNonWorkingCalendar"))
            .value(CamundaValue.stringValue(CALENDAR_URI))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse hearingDatePreDateNonWorkingDaysOfWeek = ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue("hearingDatePreDateNonWorkingDaysOfWeek"))
            .value(CamundaValue.stringValue("SATURDAY,SUNDAY"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse hearingDatePreDateSkipNonWorkingDays = ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue("hearingDatePreDateSkipNonWorkingDays"))
            .value(CamundaValue.stringValue("true"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse hearingDatePreDateMustBeWorkingDay = ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue("hearingDatePreDateMustBeWorkingDay"))
            .value(CamundaValue.stringValue(DATE_TYPE_MUST_BE_WORKING_DAY_NEXT))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse dueDateOriginRef = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOriginRef"))
            .value(CamundaValue.stringValue("nextHearingDate"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse dueDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateIntervalDays"))
            .value(CamundaValue.stringValue("21"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse dueDateNonWorkingCalendar = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateNonWorkingCalendar"))
            .value(CamundaValue.stringValue(CALENDAR_URI))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse dueDateNonWorkingDaysOfWeek = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateNonWorkingDaysOfWeek"))
            .value(CamundaValue.stringValue("SATURDAY,SUNDAY"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse dueDateSkipNonWorkingDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateSkipNonWorkingDays"))
            .value(CamundaValue.stringValue("true"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse dueDateMustBeWorkingDay = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateMustBeWorkingDay"))
            .value(CamundaValue.stringValue(DATE_TYPE_MUST_BE_WORKING_DAY_NEXT))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse priorityDateOriginEarliest = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateOriginLatest"))
            .value(CamundaValue.stringValue("hearingDatePreDate,dueDate"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = dateTypeConfigurator.configureDates(
            List.of(calculatedDates, nextHearingDate, hearingDatePreDateOriginRef,
                    hearingDatePreDateIntervalDays, hearingDatePreDateNonWorkingCalendar,
                    hearingDatePreDateNonWorkingDaysOfWeek, hearingDatePreDateSkipNonWorkingDays,
                    hearingDatePreDateMustBeWorkingDay, dueDateOriginRef, dueDateIntervalDays,
                    dueDateNonWorkingCalendar, dueDateMustBeWorkingDay, dueDateNonWorkingDaysOfWeek,
                    dueDateSkipNonWorkingDays, priorityDateOriginEarliest
            ), false, true, taskAttributes
        );

        assertThat(evaluationResponses).hasSize(3)
            .isEqualTo(List.of(
                ConfigurationDmnEvaluationResponse.builder()
                    .name(stringValue("nextHearingDate"))
                    .value(stringValue("2022-10-14T16:00"))
                    .canReconfigure(CamundaValue.booleanValue(true))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(stringValue("dueDate"))
                    .value(stringValue("2022-11-14T16:00"))
                    .canReconfigure(CamundaValue.booleanValue(true))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(stringValue("priorityDate"))
                    .value(stringValue("2022-11-14T16:00"))
                    .canReconfigure(CamundaValue.booleanValue(true))
                    .build()
            ));
    }
}
