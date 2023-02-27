package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.ConfigurationDmnEvaluationResponse;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static uk.gov.hmcts.reform.wataskmanagementapi.domain.calendar.DateTypeIntervalData.DATE_TYPE_MUST_BE_WORKING_DAY_NEXT;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.PublicHolidaysCollectionTest.CALENDAR_URI;

@SpringBootTest
@ActiveProfiles({"integration"})
public class OriginRefDateTypeConfiguratorTest {

    public static final LocalDateTime GIVEN_DATE = LocalDateTime.of(2022, 10, 13, 18, 0, 0);

    public static final LocalDateTime BST_DATE_BACKWARD = LocalDateTime.of(2022, 10, 26, 18, 0, 0);
    public static final LocalDateTime BST_DATE_FORWARD = LocalDateTime.of(2023, 3, 26, 18, 0, 0);

    public static final String PRIORITY_DATE_VALUE = GIVEN_DATE.plusDays(2).format(DateTimeFormatter.ofPattern(
        "yyyy-MM-dd")) + "T16:00";
    public static final String NEXT_HEARING_DATE_VALUE = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        + "T16:00";

    @Autowired
    private DateTypeConfigurator dateTypeConfigurator;
    private String isReConfigurationRequest = "false";

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

        ConfigurationDmnEvaluationResponse calculatedDates = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("calculatedDates"))
            .value(CamundaValue.stringValue("nextHearingDate,dueDate,priorityDate"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(nextHearingDateValue))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = readDueAndPriorityDateOriginFields(
            configurable, dueDateOriginRef, priorityDateOriginRef, calculatedDates, nextHearingDate);

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses
            = dateTypeConfigurator.configureDates(evaluationResponses, false, false);

        Assertions.assertThat(configurationDmnEvaluationResponses)
            .hasSize(4)
            .isEqualTo(List.of(
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("calculatedDates"))
                    .value(CamundaValue.stringValue("nextHearingDate,dueDate,priorityDate"))
                    .canReconfigure(CamundaValue.booleanValue(configurable))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("nextHearingDate"))
                    .value(CamundaValue.stringValue("2022-10-13T16:00"))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("dueDate"))
                    .value(CamundaValue.stringValue("2022-10-18T17:00"))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("priorityDate"))
                    .value(CamundaValue.stringValue("2022-10-26T21:00"))
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

        var calculatedDates = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("calculatedDates"))
            .value(CamundaValue.stringValue("nextHearingDate,dueDate,priorityDate"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(NEXT_HEARING_DATE_VALUE))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = readDueDateOriginFields(
            configurable, dueDateOriginRef, priorityDate, calculatedDates, nextHearingDate);

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses
            = dateTypeConfigurator.configureDates(evaluationResponses, false, false);

        Assertions.assertThat(configurationDmnEvaluationResponses)
            .hasSize(4)
            .isEqualTo(List.of(
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("calculatedDates"))
                    .value(CamundaValue.stringValue("nextHearingDate,dueDate,priorityDate"))
                    .canReconfigure(CamundaValue.booleanValue(configurable))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("nextHearingDate"))
                    .value(CamundaValue.stringValue("2022-10-13T16:00"))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("dueDate"))
                    .value(CamundaValue.stringValue("2022-10-18T17:00"))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("priorityDate"))
                    .value(CamundaValue.stringValue("2022-10-15T16:00"))
                    .build()
            ));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void should_calculate_date_when_single_origin_refs_containing_two_dates_with_first_empty_exist(
        boolean configurable) {
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

        var calculatedDates = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("calculatedDates"))
            .value(CamundaValue.stringValue("nextHearingDate,dueDate,priorityDate"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();


        List<ConfigurationDmnEvaluationResponse> evaluationResponses = readDueDateOriginFields(
            configurable, dueDateOriginRef, priorityDate, calculatedDates);

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses
            = dateTypeConfigurator.configureDates(evaluationResponses, false, false);

        Assertions.assertThat(configurationDmnEvaluationResponses)
            .hasSize(3)
            .isEqualTo(List.of(
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("calculatedDates"))
                    .value(CamundaValue.stringValue("nextHearingDate,dueDate,priorityDate"))
                    .canReconfigure(CamundaValue.booleanValue(configurable))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("dueDate"))
                    .value(CamundaValue.stringValue("2022-10-19T17:00"))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("priorityDate"))
                    .value(CamundaValue.stringValue("2022-10-15T16:00"))
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
    @ValueSource(booleans = {true, false})
    public void shouldCalculateDateWhenOriginDateIsBSTDueDateNonBSTBackword(boolean configurable) {
        String localDateTime = BST_DATE_BACKWARD.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        //Clocks go back an hour at 2:00am
        ConfigurationDmnEvaluationResponse dueDateOriginRef = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOriginRef"))
            .value(CamundaValue.stringValue("nextHearingDate,priorityDate"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        String priorityDateValue = localDateTime + "T01:30";
        ConfigurationDmnEvaluationResponse priorityDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDate"))
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
                List.of(dueDateIntervalDays, dueDateNonWorkingCalendar, dueDateMustBeWorkingDay, priorityDate,
                        dueDateNonWorkingDaysOfWeek, dueDateSkipNonWorkingDays, dueDateOriginRef, dueDateTime
                ),
                false, configurable
            );

        Assertions.assertThat(configurationDmnEvaluationResponses).hasSize(2)
            .isEqualTo(List.of(
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

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void shouldCalculateDateWhenOriginDateIsBSTDueDateNonBSTForward(boolean configurable) {
        String localDateTime = BST_DATE_FORWARD.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        //Clocks go forward an hour at 1:00am
        ConfigurationDmnEvaluationResponse dueDateOriginRef = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOriginRef"))
            .value(CamundaValue.stringValue("nextHearingDate,priorityDate"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        String priorityDateValue = localDateTime + "T00:30";
        ConfigurationDmnEvaluationResponse priorityDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDate"))
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
                List.of(dueDateIntervalDays, dueDateNonWorkingCalendar, dueDateMustBeWorkingDay, priorityDate,
                        dueDateNonWorkingDaysOfWeek, dueDateSkipNonWorkingDays, dueDateOriginRef, dueDateTime
                ),
                false, configurable
            );

        Assertions.assertThat(configurationDmnEvaluationResponses).hasSize(2)
            .isEqualTo(List.of(
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("dueDate"))
                    .value(CamundaValue.stringValue("2023-03-30T01:30"))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("priorityDate"))
                    .value(CamundaValue.stringValue(priorityDateValue))
                    .build()
            ));
    }


}
