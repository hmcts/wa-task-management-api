package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.wataskmanagementapi.config.IntegrationIdamStubConfig;
import uk.gov.hmcts.reform.wataskmanagementapi.config.IntegrationSecurityTestConfig;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.DateCalculationException;

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
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateTypeConfigurator.AMBIGUOUS_ORIGIN_DATES_PROVIDED;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.PublicHolidaysCollectionTest.CALENDAR_URI;

@SpringBootTest
@Import({IntegrationSecurityTestConfig.class, IntegrationIdamStubConfig.class})
@ActiveProfiles({"integration"})
public class OriginRefDateTypeConfiguratorTest {

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

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void should_calculate_both_dates_when_multiple_origin_refs_exist_for_two_date_types(boolean configurable) {
        String nextHearingDateValue = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + "T16:00";

        ConfigurationDmnEvaluationResponse dueDateOriginRef = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOriginRef"))
            .value(CamundaValue.stringValue("nextHearingDate,priorityDate"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        ConfigurationDmnEvaluationResponse priorityDateOriginRef = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateOriginRef"))
            .value(CamundaValue.stringValue("dueDate,nextHearingDate"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(nextHearingDateValue))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = readDueAndPriorityDateOriginFields(
            configurable, dueDateOriginRef, priorityDateOriginRef, nextHearingDate);

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses
            = dateTypeConfigurator.configureDates(
            evaluationResponses,
            false,
            false,
            taskAttributes
        );

        assertThat(configurationDmnEvaluationResponses)
            .hasSize(3)
            .isEqualTo(List.of(
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("nextHearingDate"))
                    .value(CamundaValue.stringValue("2022-10-13T16:00"))
                    .canReconfigure(CamundaValue.booleanValue(false))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("dueDate"))
                    .value(CamundaValue.stringValue("2022-10-18T17:00"))
                    .canReconfigure(CamundaValue.booleanValue(false))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("priorityDate"))
                    .value(CamundaValue.stringValue("2022-10-26T21:00"))
                    .canReconfigure(CamundaValue.booleanValue(false))
                    .build()
            ));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void should_calculate_date_when_single_origin_refs_exist(boolean configurable) {
        ConfigurationDmnEvaluationResponse dueDateOriginRef = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOriginRef"))
            .value(CamundaValue.stringValue("nextHearingDate,priorityDate"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        ConfigurationDmnEvaluationResponse priorityDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDate"))
            .value(CamundaValue.stringValue(PRIORITY_DATE_VALUE))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(NEXT_HEARING_DATE_VALUE))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = readDueDateOriginFields(
            configurable, dueDateOriginRef, priorityDate, nextHearingDate);

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses
            = dateTypeConfigurator.configureDates(
            evaluationResponses,
            false,
            false,
            taskAttributes
        );

        assertThat(configurationDmnEvaluationResponses)
            .hasSize(3)
            .isEqualTo(List.of(
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("nextHearingDate"))
                    .value(CamundaValue.stringValue("2022-10-13T16:00"))
                    .canReconfigure(CamundaValue.booleanValue(false))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("dueDate"))
                    .value(CamundaValue.stringValue("2022-10-18T17:00"))
                    .canReconfigure(CamundaValue.booleanValue(false))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("priorityDate"))
                    .value(CamundaValue.stringValue("2022-10-15T16:00"))
                    .canReconfigure(CamundaValue.booleanValue(false))
                    .build()
            ));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true})
    public void should_calculate_date_when_single_origin_refs_containing_two_dates_with_first_empty_exist(
        boolean configurable) {
        ConfigurationDmnEvaluationResponse dueDateOriginRef = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOriginRef"))
            .value(CamundaValue.stringValue("nextHearingDate,nextHearingDuration"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDuration = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDuration"))
            .value(CamundaValue.stringValue(PRIORITY_DATE_VALUE))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        var calculatedDates = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("calculatedDates"))
            .value(CamundaValue.stringValue("nextHearingDate,nextHearingDuration,dueDate,priorityDate"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = readDueDateOriginFields(
            configurable, dueDateOriginRef, nextHearingDuration, calculatedDates);

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses
            = dateTypeConfigurator.configureDates(
            evaluationResponses,
            false,
            configurable,
            taskAttributes
        );

        assertThat(configurationDmnEvaluationResponses)
            .hasSize(2)
            .isEqualTo(List.of(
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("nextHearingDuration"))
                    .value(CamundaValue.stringValue("2022-10-15T16:00"))
                    .canReconfigure(CamundaValue.booleanValue(configurable))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("dueDate"))
                    .value(CamundaValue.stringValue("2022-10-19T17:00"))
                    .canReconfigure(CamundaValue.booleanValue(configurable))
                    .build()
            ));
    }

    private List<ConfigurationDmnEvaluationResponse> readDueAndPriorityDateOriginFields(
        boolean configurable,
        ConfigurationDmnEvaluationResponse... fields) {
        List<ConfigurationDmnEvaluationResponse> allFields = new ArrayList<>();
        allFields.addAll(getDueDateConfigurations(configurable));
        allFields.addAll(getPriorityDateConfigurations(configurable));
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
                .name(CamundaValue.stringValue("dueDateIntervalDays"))
                .value(CamundaValue.stringValue("3"))
                .canReconfigure(CamundaValue.booleanValue(configurable))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(CamundaValue.stringValue("dueDateNonWorkingCalendar"))
                .value(CamundaValue.stringValue(CALENDAR_URI))
                .canReconfigure(CamundaValue.booleanValue(configurable))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(CamundaValue.stringValue("dueDateNonWorkingDaysOfWeek"))
                .value(CamundaValue.stringValue("SATURDAY,SUNDAY"))
                .canReconfigure(CamundaValue.booleanValue(configurable))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(CamundaValue.stringValue("dueDateSkipNonWorkingDays"))
                .value(CamundaValue.stringValue("true"))
                .canReconfigure(CamundaValue.booleanValue(configurable))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(CamundaValue.stringValue("dueDateMustBeWorkingDay"))
                .value(CamundaValue.stringValue(DATE_TYPE_MUST_BE_WORKING_DAY_NEXT))
                .canReconfigure(CamundaValue.booleanValue(configurable))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(CamundaValue.stringValue("dueDateTime"))
                .value(CamundaValue.stringValue("17:00"))
                .canReconfigure(CamundaValue.booleanValue(configurable))
                .build()
        );
    }

    private List<ConfigurationDmnEvaluationResponse> getPriorityDateConfigurations(boolean configurable) {
        return List.of(
            ConfigurationDmnEvaluationResponse.builder()
                .name(CamundaValue.stringValue("priorityDateIntervalDays"))
                .value(CamundaValue.stringValue("6"))
                .canReconfigure(CamundaValue.booleanValue(configurable))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(CamundaValue.stringValue("priorityDateNonWorkingCalendar"))
                .value(CamundaValue.stringValue(CALENDAR_URI))
                .canReconfigure(CamundaValue.booleanValue(configurable))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(CamundaValue.stringValue("priorityDateNonWorkingDaysOfWeek"))
                .value(CamundaValue.stringValue("SATURDAY,SUNDAY"))
                .canReconfigure(CamundaValue.booleanValue(configurable))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(CamundaValue.stringValue("priorityDateSkipNonWorkingDays"))
                .value(CamundaValue.stringValue("true"))
                .canReconfigure(CamundaValue.booleanValue(configurable))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(CamundaValue.stringValue("priorityDateMustBeWorkingDay"))
                .value(CamundaValue.stringValue(DATE_TYPE_MUST_BE_WORKING_DAY_NEXT))
                .canReconfigure(CamundaValue.booleanValue(configurable))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(CamundaValue.stringValue("priorityDateTime"))
                .value(CamundaValue.stringValue("21:00"))
                .canReconfigure(CamundaValue.booleanValue(configurable))
                .build()
        );
    }

    @ParameterizedTest
    @ValueSource(booleans = {true})
    public void shouldCalculateDateWhenOriginDateIsBSTDueDateNonBSTBackword(boolean configurable) {
        String localDateTime = BST_DATE_BACKWARD.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        //Clocks go back an hour at 2:00am
        ConfigurationDmnEvaluationResponse dueDateOriginRef = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOriginRef"))
            .value(CamundaValue.stringValue("nextHearingDate"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        String priorityDateValue = localDateTime + "T01:30";
        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(priorityDateValue))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        ConfigurationDmnEvaluationResponse dueDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateIntervalDays"))
            .value(CamundaValue.stringValue("4"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();
        ConfigurationDmnEvaluationResponse dueDateNonWorkingCalendar = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateNonWorkingCalendar"))
            .value(CamundaValue.stringValue("https://www.gov.uk/bank-holidays/england-and-wales.json"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        ConfigurationDmnEvaluationResponse dueDateNonWorkingDaysOfWeek = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateNonWorkingDaysOfWeek"))
            .value(CamundaValue.stringValue(""))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();
        ConfigurationDmnEvaluationResponse dueDateSkipNonWorkingDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateSkipNonWorkingDays"))
            .value(CamundaValue.stringValue("false"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();
        ConfigurationDmnEvaluationResponse dueDateMustBeWorkingDay = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateMustBeWorkingDay"))
            .value(CamundaValue.stringValue("false"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        ConfigurationDmnEvaluationResponse dueDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateTime"))
            .value(CamundaValue.stringValue("02:30"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDates(
                List.of(dueDateIntervalDays, dueDateNonWorkingCalendar, dueDateMustBeWorkingDay, nextHearingDate,
                        dueDateNonWorkingDaysOfWeek, dueDateSkipNonWorkingDays, dueDateOriginRef, dueDateTime
                ),
                false,
                configurable,
                taskAttributes
            );

        assertThat(configurationDmnEvaluationResponses).hasSize(2)
            .isEqualTo(List.of(
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("nextHearingDate"))
                    .value(CamundaValue.stringValue(priorityDateValue))
                    .canReconfigure(CamundaValue.booleanValue(configurable))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("dueDate"))
                    .value(CamundaValue.stringValue("2022-10-30T02:30"))
                    .canReconfigure(CamundaValue.booleanValue(configurable))
                    .build()
            ));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true})
    public void shouldCalculateDateWhenOriginDateIsBSTDueDateNonBSTForward(boolean configurable) {
        String localDateTime = BST_DATE_FORWARD.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        //Clocks go forward an hour at 1:00am
        ConfigurationDmnEvaluationResponse dueDateOriginRef = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOriginRef"))
            .value(CamundaValue.stringValue("nextHearingDate"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        String priorityDateValue = localDateTime + "T00:30";
        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(priorityDateValue))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        ConfigurationDmnEvaluationResponse dueDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateIntervalDays"))
            .value(CamundaValue.stringValue("4"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();
        ConfigurationDmnEvaluationResponse dueDateNonWorkingCalendar = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateNonWorkingCalendar"))
            .value(CamundaValue.stringValue("https://www.gov.uk/bank-holidays/england-and-wales.json"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        ConfigurationDmnEvaluationResponse dueDateNonWorkingDaysOfWeek = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateNonWorkingDaysOfWeek"))
            .value(CamundaValue.stringValue(""))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();
        ConfigurationDmnEvaluationResponse dueDateSkipNonWorkingDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateSkipNonWorkingDays"))
            .value(CamundaValue.stringValue("false"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();
        ConfigurationDmnEvaluationResponse dueDateMustBeWorkingDay = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateMustBeWorkingDay"))
            .value(CamundaValue.stringValue("false"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        ConfigurationDmnEvaluationResponse dueDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateTime"))
            .value(CamundaValue.stringValue("01:30"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDates(
                List.of(dueDateIntervalDays, dueDateNonWorkingCalendar, dueDateMustBeWorkingDay, nextHearingDate,
                        dueDateNonWorkingDaysOfWeek, dueDateSkipNonWorkingDays, dueDateOriginRef, dueDateTime
                ),
                false,
                configurable,
                taskAttributes
            );

        assertThat(configurationDmnEvaluationResponses).hasSize(2)
            .isEqualTo(List.of(
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("nextHearingDate"))
                    .value(CamundaValue.stringValue(priorityDateValue))
                    .canReconfigure(CamundaValue.booleanValue(configurable))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("dueDate"))
                    .value(CamundaValue.stringValue("2023-03-30T01:30"))
                    .canReconfigure(CamundaValue.booleanValue(configurable))
                    .build()
            ));
    }

    @Test
    public void shouldReCalculateDateWhenBothDueDateAndDueDateOriginRefAreSetForReconfiguration() {
        String dueDateValue = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String nextHearingDateValue = GIVEN_DATE.plusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse dueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(dueDateValue + "T18:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse dueDateOriginRef = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOriginRef"))
            .value(CamundaValue.stringValue("nextHearingDate"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(nextHearingDateValue + "T21:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDates(
                List.of(dueDate, dueDateOriginRef, nextHearingDate),
                false,
                true,
                taskAttributes
            );

        String calculatedDueDate = dueDateValue + "T18:00";

        assertThat(configurationDmnEvaluationResponses).hasSize(2)
            .isEqualTo(List.of(
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("nextHearingDate"))
                    .value(CamundaValue.stringValue(nextHearingDateValue + "T21:00"))
                    .canReconfigure(CamundaValue.booleanValue(true))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("dueDate"))
                    .value(CamundaValue.stringValue(calculatedDueDate))
                    .canReconfigure(CamundaValue.booleanValue(true))
                    .build()
            ));
    }

    @Test
    public void shouldReCalculateDateWhenBothPriorityDateAndPriorityDateOriginRefAreSetForReconfiguration() {
        String dueDateValue = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String nextHearingDateValue = GIVEN_DATE.plusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse dueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDate"))
            .value(CamundaValue.stringValue(dueDateValue + "T18:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse dueDateOriginRef = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateOriginRef"))
            .value(CamundaValue.stringValue("nextHearingDate"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(nextHearingDateValue + "T21:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDates(
                List.of(dueDate, dueDateOriginRef, nextHearingDate),
                false,
                true,
                taskAttributes
            );

        String calculatedDueDate = dueDateValue + "T18:00";

        assertThat(configurationDmnEvaluationResponses).hasSize(2)
            .isEqualTo(List.of(
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("nextHearingDate"))
                    .value(CamundaValue.stringValue(nextHearingDateValue + "T21:00"))
                    .canReconfigure(CamundaValue.booleanValue(true))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("priorityDate"))
                    .value(CamundaValue.stringValue(calculatedDueDate))
                    .canReconfigure(CamundaValue.booleanValue(true))
                    .build()
            ));
    }

    @Test
    public void shouldReCalculateDateWhenBothNextHearingDateAndNextHearingDateOriginRefAreSetForReconfiguration() {
        String dueDateValue = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String nextHearingDateValue = GIVEN_DATE.plusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse dueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(dueDateValue + "T18:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDateOriginRef = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateOriginRef"))
            .value(CamundaValue.stringValue("dueDate"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(nextHearingDateValue + "T21:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDates(
                List.of(dueDate, nextHearingDateOriginRef, nextHearingDate),
                false,
                true,
                taskAttributes
            );

        String calculatedDueDate = dueDateValue + "T18:00";

        assertThat(configurationDmnEvaluationResponses).hasSize(2)
            .isEqualTo(List.of(
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("nextHearingDate"))
                    .value(CamundaValue.stringValue(calculatedDueDate))
                    .canReconfigure(CamundaValue.booleanValue(true))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("dueDate"))
                    .value(CamundaValue.stringValue(nextHearingDateValue + "T21:00"))
                    .canReconfigure(CamundaValue.booleanValue(true))
                    .build()
            ));
    }

    @Test
    public void shouldNotReCalculateDateWhenBothDueDateAndDueDateOriginRefAreNotSetForReconfiguration() {
        String dueDateValue = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String priorityDateValue = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String nextHearingDateValue = GIVEN_DATE.plusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse dueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(dueDateValue + "T18:00"))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        ConfigurationDmnEvaluationResponse dueDateOriginRef = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOriginRef"))
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
            .configureDates(
                List.of(dueDate, dueDateOriginRef, nextHearingDate, priorityDate),
                false,
                true,
                taskAttributes
            );

        assertThat(configurationDmnEvaluationResponses).isEmpty();
    }

    @Test
    public void shouldNotReCalculateDateWhenBothPriorityDateAndPriorityDateOriginRefAreNotSetForReconfiguration() {
        String priorityDateValue = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String dueDateValue = GIVEN_DATE.minusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String nextHearingDateValue = GIVEN_DATE.plusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse priorityDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDate"))
            .value(CamundaValue.stringValue(priorityDateValue + "T18:00"))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        ConfigurationDmnEvaluationResponse dueDateOriginRef = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateOriginRef"))
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
            .configureDates(
                List.of(priorityDate, dueDate, dueDateOriginRef, nextHearingDate),
                false,
                true,
                taskAttributes
            );

        assertThat(configurationDmnEvaluationResponses).isEmpty();
    }

    @Test
    public void shouldNotReCalculateDateWhenBothNextHearingDateAndOriginRefAreNotSetForReconfiguration() {
        String dueDateValue = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String nextHearingDateValue = GIVEN_DATE.plusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse dueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(dueDateValue + "T18:00"))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        ConfigurationDmnEvaluationResponse dueDateOriginRef = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateOriginRef"))
            .value(CamundaValue.stringValue("dueDate"))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(nextHearingDateValue + "T21:00"))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDates(List.of(dueDate, dueDateOriginRef, nextHearingDate), false, true, taskAttributes);

        assertThat(configurationDmnEvaluationResponses).isEmpty();
    }

    @Test
    public void shouldNotRecalculateDateWhenDueDateIsUnconfigurableButOriginRefIsConfigurable() {
        String dueDateValue = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse dueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(dueDateValue + "T18:00"))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        ConfigurationDmnEvaluationResponse dueDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOriginRef"))
            .value(CamundaValue.stringValue("priorityDate"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDates(
                List.of(dueDate, dueDateOrigin),
                false,
                true,
                taskAttributes
            );

        assertThat(configurationDmnEvaluationResponses).isEmpty();
    }

    @Test
    public void shouldNotRecalculateDateWhenPriorityDateIsUnconfigurableButOriginRefIsConfigurable() {
        String priorityDateValue = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse priorityDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDate"))
            .value(CamundaValue.stringValue(priorityDateValue + "T18:00"))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        ConfigurationDmnEvaluationResponse priorityDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateOriginRef"))
            .value(CamundaValue.stringValue("priorityDate"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDates(
                List.of(priorityDate, priorityDateOrigin),
                false,
                true,
                taskAttributes
            );

        assertThat(configurationDmnEvaluationResponses).isEmpty();
    }

    @Test
    public void shouldNotRecalculateDateWhenNextHearingDateIsUnconfigurableButOriginRefIsConfigurable() {
        String nextHearingDateValue = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(nextHearingDateValue + "T18:00"))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateOriginRef"))
            .value(CamundaValue.stringValue("priorityDate"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDates(
                List.of(nextHearingDate, nextHearingDateOrigin),
                false,
                true,
                taskAttributes
            );

        assertThat(configurationDmnEvaluationResponses).isEmpty();
    }

    @Test
    public void shouldRecalculateDateWhenDueDateIsConfigurableButOriginRefIsUnConfigurable() {
        String dueDateValue = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse dueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(dueDateValue + "T18:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse dueDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOriginRef"))
            .value(CamundaValue.stringValue("nextHearingDate,priorityDate"))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDates(
                List.of(dueDate, dueDateOrigin),
                false,
                true,
                taskAttributes
            );

        assertThat(configurationDmnEvaluationResponses).hasSize(1)
            .isEqualTo(List.of(
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("dueDate"))
                    .value(CamundaValue.stringValue(dueDateValue + "T18:00"))
                    .canReconfigure(CamundaValue.booleanValue(true))
                    .build()
            ));
    }

    @Test
    public void shouldRecalculateDateWhenPriorityDateIsConfigurableButOriginRefIsUnConfigurable() {
        String priorityDateValue = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse priorityDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDate"))
            .value(CamundaValue.stringValue(priorityDateValue + "T18:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse priorityDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateOriginRef"))
            .value(CamundaValue.stringValue("nextHearingDate,priorityDate"))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDates(
                List.of(priorityDate, priorityDateOrigin),
                false,
                true,
                taskAttributes
            );

        assertThat(configurationDmnEvaluationResponses).hasSize(1)
            .isEqualTo(List.of(
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("priorityDate"))
                    .value(CamundaValue.stringValue(priorityDate.getValue().getValue()))
                    .canReconfigure(CamundaValue.booleanValue(true))
                    .build()
            ));
    }

    @Test
    public void shouldRecalculateDateWhenNextHearingDateIsConfigurableButOriginRefIsUnConfigurable() {
        String nextHearingDateValue = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(nextHearingDateValue + "T18:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateOriginRef"))
            .value(CamundaValue.stringValue("nextHearingDate,priorityDate"))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDates(
                List.of(nextHearingDate, nextHearingDateOrigin),
                false,
                true,
                taskAttributes
            );

        assertThat(configurationDmnEvaluationResponses).hasSize(1)
            .isEqualTo(List.of(
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("nextHearingDate"))
                    .value(CamundaValue.stringValue(nextHearingDateValue + "T18:00"))
                    .canReconfigure(CamundaValue.booleanValue(true))
                    .build()
            ));
    }

    @Test
    public void shouldCalculateDateFromTaskResourceWhenReferenceDatesAreNotProvidedForReconfigure() {
        String nextHearingDateValue = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String dueDateValue = GIVEN_DATE.minusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        LocalDateTime taskResourceDueDate = GIVEN_DATE.minusDays(4);
        taskAttributes.put("nextHearingDate", taskResourceDueDate.atZone(ZoneId.systemDefault()).toOffsetDateTime());

        ConfigurationDmnEvaluationResponse calculatedDates = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("calculatedDates"))
            .value(CamundaValue.stringValue("nextHearingDate,nextHearingDuration,dueDate,priorityDate"))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(nextHearingDateValue + "T18:00"))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDuration = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDuration"))
            .value(CamundaValue.stringValue(dueDateValue + "T21:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse dueDateOriginRef = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOriginRef"))
            .value(CamundaValue.stringValue("nextHearingDate,nextHearingDuration"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        List<ConfigurationDmnEvaluationResponse> configResponses = dateTypeConfigurator
            .configureDates(
                List.of(
                    calculatedDates,
                    nextHearingDate,
                    nextHearingDuration,
                    dueDateOriginRef
                ),
                false,
                true,
                taskAttributes
            );

        assertThat(configResponses).hasSize(2)
            .isEqualTo(List.of(
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("nextHearingDuration"))
                    .value(CamundaValue.stringValue("2022-10-12T21:00"))
                    .canReconfigure(CamundaValue.booleanValue(true))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("dueDate"))
                    .value(CamundaValue.stringValue(taskResourceDueDate
                                                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                                                        + "T18:00"))
                    .canReconfigure(CamundaValue.booleanValue(true))
                    .build()
            ));
    }

    @Test
    public void shouldCalculateDueDateFromTaskResourceWhenReferenceDatesAreNotProvidedForReconfigure() {
        String nextHearingDateValue = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String dueDateValue = GIVEN_DATE.minusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        LocalDateTime taskResourceDueDate = GIVEN_DATE.minusDays(4);
        taskAttributes.put("dueDate", taskResourceDueDate.atZone(ZoneId.systemDefault()).toOffsetDateTime());

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

        ConfigurationDmnEvaluationResponse priorityDateOriginRef = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateOriginRef"))
            .value(CamundaValue.stringValue("dueDate,nextHearingDate"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        List<ConfigurationDmnEvaluationResponse> configResponses = dateTypeConfigurator
            .configureDates(
                List.of(
                    nextHearingDate,
                    dueDate,
                    priorityDateOriginRef
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
                    .canReconfigure(CamundaValue.booleanValue(true))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("priorityDate"))
                    .value(CamundaValue.stringValue(taskResourceDueDate
                                                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                                                        + "T18:00"))
                    .canReconfigure(CamundaValue.booleanValue(true))
                    .build()
            ));
    }

    @Test
    public void should_not_calculate_date_when_multiple_origin_date_types_for_due_date_exist() {
        ConfigurationDmnEvaluationResponse dueDateOriginRef = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOriginRef"))
            .value(CamundaValue.stringValue("nextHearingDate,nextHearingDuration"))
            .build();

        ConfigurationDmnEvaluationResponse dueDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOrigin"))
            .value(CamundaValue.stringValue(DUE_DATE_VALUE))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(dueDateOriginRef, dueDateOrigin);

        assertThatThrownBy(() -> dateTypeConfigurator
            .configureDates(
                evaluationResponses,
                false,
                false,
                taskAttributes
            ))
            .isInstanceOf(DateCalculationException.class)
            .hasMessage(AMBIGUOUS_ORIGIN_DATES_PROVIDED);
    }

    @Test
    public void should_not_calculate_date_when_multiple_origin_date_types_for_priority_date_exist() {
        ConfigurationDmnEvaluationResponse priorityDateOriginRef = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateOriginRef"))
            .value(CamundaValue.stringValue("nextHearingDate,nextHearingDuration"))
            .build();

        ConfigurationDmnEvaluationResponse priorityDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateOrigin"))
            .value(CamundaValue.stringValue(PRIORITY_DATE_VALUE))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses
            = List.of(priorityDateOriginRef, priorityDateOrigin);

        assertThatThrownBy(() -> dateTypeConfigurator
            .configureDates(
                evaluationResponses,
                false,
                false,
                taskAttributes
            ))
            .isInstanceOf(DateCalculationException.class)
            .hasMessage(AMBIGUOUS_ORIGIN_DATES_PROVIDED);
    }

    @Test
    public void should_not_calculate_date_when_multiple_origin_date_types_for_next_hearing_date_exist() {
        ConfigurationDmnEvaluationResponse nextHearingDateOriginRef = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateOriginRef"))
            .value(CamundaValue.stringValue("nextHearingDate,nextHearingDuration"))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateOrigin"))
            .value(CamundaValue.stringValue(NEXT_HEARING_DATE_VALUE))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(
            nextHearingDateOriginRef, nextHearingDateOrigin);

        assertThatThrownBy(() -> dateTypeConfigurator
            .configureDates(
                evaluationResponses,
                false,
                false,
                taskAttributes
            ))
            .isInstanceOf(DateCalculationException.class)
            .hasMessage(AMBIGUOUS_ORIGIN_DATES_PROVIDED);
    }

    @Test
    public void should_not_calculate_date_when_multiple_origin_date_types_for_intermediate_date_exist() {
        ConfigurationDmnEvaluationResponse nextHearingDurationOriginRef = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDurationOriginRef"))
            .value(CamundaValue.stringValue("nextHearingDate,nextHearingDuration"))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDurationOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDurationOrigin"))
            .value(CamundaValue.stringValue(DUE_DATE_VALUE))
            .build();

        ConfigurationDmnEvaluationResponse calculatedDates = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("calculatedDates"))
            .value(CamundaValue.stringValue("nextHearingDate,nextHearingDuration,dueDate,priorityDate"))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(
            nextHearingDurationOriginRef, nextHearingDurationOrigin, calculatedDates);

        assertThatThrownBy(() -> dateTypeConfigurator
            .configureDates(
                evaluationResponses,
                false,
                false,
                taskAttributes
            ))
            .isInstanceOf(DateCalculationException.class)
            .hasMessage(AMBIGUOUS_ORIGIN_DATES_PROVIDED);
    }

    @Test
    @DisplayName("Only one OriginRef present in DMN and has canConfigure set to False")
    public void shouldNotReCalculateDateWhenOnlyDueDateOriginRefIsSpecifiedAndNotSetForReconfiguration() {
        LocalDateTime priorityDate = GIVEN_DATE.minusDays(2);
        LocalDateTime nextHearingDate = GIVEN_DATE.plusDays(2);
        taskAttributes.put("priorityDate", priorityDate.atZone(ZoneId.systemDefault()).toOffsetDateTime());
        taskAttributes.put("dueDate", GIVEN_DATE.atZone(ZoneId.systemDefault()).toOffsetDateTime());
        taskAttributes.put("nextHearingDate", nextHearingDate.atZone(ZoneId.systemDefault()).toOffsetDateTime());

        ConfigurationDmnEvaluationResponse dueDateOriginRef = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOriginRef"))
            .value(CamundaValue.stringValue("nextHearingDate,priorityDate"))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDates(List.of(dueDateOriginRef),
                            false, true,
                            taskAttributes
            );

        assertThat(configurationDmnEvaluationResponses).hasSize(0);
    }

    @Test
    @DisplayName("Only OriginRef set and canConfigure is True and all parameters canReconfigure false")
    public void shouldCalculateDateWhenOriginDateRefIsSetForReconfigurationAndOtherParametersNotSpecified() {
        LocalDateTime priorityDate = GIVEN_DATE.minusDays(2);
        LocalDateTime nextHearingDate = GIVEN_DATE.plusDays(2);
        taskAttributes.put("priorityDate", priorityDate.atZone(ZoneId.systemDefault()).toOffsetDateTime());
        taskAttributes.put("dueDate", GIVEN_DATE.atZone(ZoneId.systemDefault()).toOffsetDateTime());
        taskAttributes.put("nextHearingDate", nextHearingDate.atZone(ZoneId.systemDefault()).toOffsetDateTime());

        ConfigurationDmnEvaluationResponse dueDateOriginRef = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOriginRef"))
            .value(CamundaValue.stringValue("nextHearingDate,priorityDate"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse dueDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateIntervalDays"))
            .value(CamundaValue.stringValue("0"))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();
        ConfigurationDmnEvaluationResponse dueDateNonWorkingCalendar = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateNonWorkingCalendar"))
            .value(CamundaValue.stringValue("https://www.gov.uk/bank-holidays/england-and-wales.json"))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        ConfigurationDmnEvaluationResponse dueDateNonWorkingDaysOfWeek = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateNonWorkingDaysOfWeek"))
            .value(CamundaValue.stringValue(""))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();
        ConfigurationDmnEvaluationResponse dueDateSkipNonWorkingDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateSkipNonWorkingDays"))
            .value(CamundaValue.stringValue("true"))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();
        ConfigurationDmnEvaluationResponse dueDateMustBeWorkingDay = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateMustBeWorkingDay"))
            .value(CamundaValue.stringValue("Next"))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDates(
                List.of(dueDateIntervalDays, dueDateNonWorkingCalendar,
                        dueDateMustBeWorkingDay, dueDateNonWorkingDaysOfWeek, dueDateSkipNonWorkingDays,
                        dueDateOriginRef
                ),
                false, true,
                taskAttributes
            );

        assertThat(configurationDmnEvaluationResponses).hasSize(1)
            .isEqualTo(List.of(
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("dueDate"))
                    .value(CamundaValue.stringValue(
                        nextHearingDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + "T18:00")
                    )
                    .canReconfigure(CamundaValue.booleanValue(true))
                    .build()
            ));
    }

    @Test
    @DisplayName("Only OriginRef set and canConfigure is True and all parameters canReconfigure set true")
    public void shouldCalculateDateWhenOriginDateRefIsSetForReconfigurationAndOtherParametersReconfigurable() {
        LocalDateTime priorityDate = GIVEN_DATE.minusDays(6);
        LocalDateTime nextHearingDate = GIVEN_DATE.plusDays(6);
        taskAttributes.put("priorityDate", priorityDate.atZone(ZoneId.systemDefault()).toOffsetDateTime());
        taskAttributes.put("dueDate", GIVEN_DATE.atZone(ZoneId.systemDefault()).toOffsetDateTime());
        taskAttributes.put("nextHearingDate", nextHearingDate.atZone(ZoneId.systemDefault()).toOffsetDateTime());

        ConfigurationDmnEvaluationResponse dueDateOriginRef = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOriginRef"))
            .value(CamundaValue.stringValue("nextHearingDate,priorityDate"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse dueDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateIntervalDays"))
            .value(CamundaValue.stringValue("2"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();
        ConfigurationDmnEvaluationResponse dueDateNonWorkingCalendar = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateNonWorkingCalendar"))
            .value(CamundaValue.stringValue("https://www.gov.uk/bank-holidays/england-and-wales.json"))
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
            .value(CamundaValue.stringValue("Next"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDates(
                List.of(dueDateIntervalDays, dueDateNonWorkingCalendar,
                        dueDateMustBeWorkingDay, dueDateNonWorkingDaysOfWeek, dueDateSkipNonWorkingDays,
                        dueDateOriginRef
                ),
                false, true,
                taskAttributes
            );

        assertThat(configurationDmnEvaluationResponses).hasSize(1)
            .isEqualTo(List.of(
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("dueDate"))
                    .value(CamundaValue.stringValue(
                        nextHearingDate.plusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + "T18:00")
                    )
                    .canReconfigure(CamundaValue.booleanValue(true))
                    .build()
            ));
    }

    @Test
    @DisplayName("Only OriginRef is set and reconfigurable including all parameters and with negative interval")
    public void shouldCalculateDateWhenOriginDateRefIsSetForReconfigurationWithNegativeInterval() {
        LocalDateTime priorityDate = GIVEN_DATE.minusDays(6);
        LocalDateTime nextHearingDate = GIVEN_DATE.plusDays(6);
        taskAttributes.put("priorityDate", priorityDate.atZone(ZoneId.systemDefault()).toOffsetDateTime());
        taskAttributes.put("dueDate", GIVEN_DATE.atZone(ZoneId.systemDefault()).toOffsetDateTime());
        taskAttributes.put("nextHearingDate", nextHearingDate.atZone(ZoneId.systemDefault()).toOffsetDateTime());

        ConfigurationDmnEvaluationResponse dueDateOriginRef = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOriginRef"))
            .value(CamundaValue.stringValue("nextHearingDate,priorityDate"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse dueDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateIntervalDays"))
            .value(CamundaValue.stringValue("-2"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();
        ConfigurationDmnEvaluationResponse dueDateNonWorkingCalendar = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateNonWorkingCalendar"))
            .value(CamundaValue.stringValue("https://www.gov.uk/bank-holidays/england-and-wales.json"))
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
            .value(CamundaValue.stringValue("Previous"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDates(
                List.of(dueDateIntervalDays, dueDateNonWorkingCalendar,
                        dueDateMustBeWorkingDay, dueDateNonWorkingDaysOfWeek, dueDateSkipNonWorkingDays,
                        dueDateOriginRef
                ),
                false, true,
                taskAttributes
            );

        assertThat(configurationDmnEvaluationResponses).hasSize(1)
            .isEqualTo(List.of(
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("dueDate"))
                    .value(CamundaValue.stringValue(
                        nextHearingDate.minusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + "T18:00")
                    )
                    .canReconfigure(CamundaValue.booleanValue(true))
                    .build()
            ));
    }

    @Test
    @DisplayName("Only OriginRef set and canConfigure is True and only some parameters canReconfigure set true")
    public void shouldCalculateDateWhenOriginDateRefIsReconfigurableButOnlySomeParametersReconfigurable() {
        LocalDateTime priorityDate = GIVEN_DATE.minusDays(6);
        LocalDateTime nextHearingDate = GIVEN_DATE.plusDays(6);
        taskAttributes.put("priorityDate", priorityDate.atZone(ZoneId.systemDefault()).toOffsetDateTime());
        taskAttributes.put("dueDate", GIVEN_DATE.atZone(ZoneId.systemDefault()).toOffsetDateTime());
        taskAttributes.put("nextHearingDate", nextHearingDate.atZone(ZoneId.systemDefault()).toOffsetDateTime());

        ConfigurationDmnEvaluationResponse dueDateOriginRef = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOriginRef"))
            .value(CamundaValue.stringValue("nextHearingDate,priorityDate"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse dueDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateIntervalDays"))
            .value(CamundaValue.stringValue("2"))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();
        ConfigurationDmnEvaluationResponse dueDateNonWorkingCalendar = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateNonWorkingCalendar"))
            .value(CamundaValue.stringValue("https://www.gov.uk/bank-holidays/england-and-wales.json"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse dueDateNonWorkingDaysOfWeek = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateNonWorkingDaysOfWeek"))
            .value(CamundaValue.stringValue("SATURDAY,SUNDAY"))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();
        ConfigurationDmnEvaluationResponse dueDateSkipNonWorkingDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateSkipNonWorkingDays"))
            .value(CamundaValue.stringValue("true"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();
        ConfigurationDmnEvaluationResponse dueDateMustBeWorkingDay = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateMustBeWorkingDay"))
            .value(CamundaValue.stringValue("Next"))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDates(
                List.of(dueDateIntervalDays, dueDateNonWorkingCalendar,
                        dueDateMustBeWorkingDay, dueDateNonWorkingDaysOfWeek, dueDateSkipNonWorkingDays,
                        dueDateOriginRef
                ),
                false, true,
                taskAttributes
            );

        assertThat(configurationDmnEvaluationResponses).hasSize(1)
            .isEqualTo(List.of(
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("dueDate"))
                    .value(CamundaValue.stringValue(
                        nextHearingDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + "T18:00")
                    )
                    .canReconfigure(CamundaValue.booleanValue(true))
                    .build()
            ));
    }

    @Test
    @DisplayName("Only OriginRef set but canConfigure is false and all parameters canReconfigure set true")
    public void shouldNotCalculateDateWhenOriginDateRefIsNotReconfigurableButOtherParametersAreReconfigurable() {
        LocalDateTime priorityDate = GIVEN_DATE.minusDays(6);
        LocalDateTime nextHearingDate = GIVEN_DATE.plusDays(6);
        taskAttributes.put("priorityDate", priorityDate.atZone(ZoneId.systemDefault()).toOffsetDateTime());
        taskAttributes.put("dueDate", GIVEN_DATE.atZone(ZoneId.systemDefault()).toOffsetDateTime());
        taskAttributes.put("nextHearingDate", nextHearingDate.atZone(ZoneId.systemDefault()).toOffsetDateTime());

        ConfigurationDmnEvaluationResponse dueDateOriginRef = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOriginRef"))
            .value(CamundaValue.stringValue("nextHearingDate,priorityDate"))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        ConfigurationDmnEvaluationResponse dueDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateIntervalDays"))
            .value(CamundaValue.stringValue("2"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();
        ConfigurationDmnEvaluationResponse dueDateNonWorkingCalendar = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateNonWorkingCalendar"))
            .value(CamundaValue.stringValue("https://www.gov.uk/bank-holidays/england-and-wales.json"))
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
            .value(CamundaValue.stringValue("Next"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDates(
                List.of(dueDateIntervalDays, dueDateNonWorkingCalendar,
                        dueDateMustBeWorkingDay, dueDateNonWorkingDaysOfWeek, dueDateSkipNonWorkingDays,
                        dueDateOriginRef
                ),
                false, true,
                taskAttributes
            );

        assertThat(configurationDmnEvaluationResponses).hasSize(0);
    }
}
