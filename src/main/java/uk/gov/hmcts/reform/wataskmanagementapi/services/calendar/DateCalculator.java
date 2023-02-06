package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import org.apache.logging.log4j.util.Strings;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.ConfigurationDmnEvaluationResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public interface DateCalculator {
    String DUE_DATE_ORIGIN = "dueDateOrigin";
    String DUE_DATE_ORIGIN_REF = "dueDateOriginRef";
    String DUE_DATE_ORIGIN_EARLIEST = "dueDateOriginEarliest";
    String DUE_DATE_INTERVAL_DAYS = "dueDateIntervalDays";
    String DUE_DATE_NON_WORKING_CALENDAR = "dueDateNonWorkingCalendar";
    String DUE_DATE_NON_WORKING_DAYS_OF_WEEK = "dueDateNonWorkingDaysOfWeek";
    String DUE_DATE_SKIP_NON_WORKING_DAYS = "dueDateSkipNonWorkingDays";
    String DUE_DATE_MUST_BE_WORKING_DAYS = "dueDateMustBeWorkingDay";
    String DUE_DATE_TIME = "dueDateTime";
    String PRIORITY_DATE_ORIGIN = "priorityDateOrigin";
    String PRIORITY_DATE_ORIGIN_REF = "priorityDateOriginRef";
    String PRIORITY_DATE_ORIGIN_EARLIEST = "priorityDateOriginEarliest";
    String PRIORITY_DATE_INTERVAL_DAYS = "priorityDateIntervalDays";
    String PRIORITY_DATE_NON_WORKING_CALENDAR = "priorityDateNonWorkingCalendar";
    String PRIORITY_DATE_NON_WORKING_DAYS_OF_WEEK = "priorityDateNonWorkingDaysOfWeek";
    String PRIORITY_DATE_SKIP_NON_WORKING_DAYS = "priorityDateSkipNonWorkingDays";
    String PRIORITY_DATE_MUST_BE_WORKING_DAYS = "priorityDateMustBeWorkingDay";
    String PRIORITY_DATE_TIME = "priorityDateTime";
    String NEXT_HEARING_DATE_TIME = "nextHearingDateTime";
    String NEXT_HEARING_DATE_ORIGIN = "nextHearingDateOrigin";
    String NEXT_HEARING_DATE_ORIGIN_REF = "nextHearingDateOriginRef";
    String NEXT_HEARING_DATE_ORIGIN_EARLIEST = "nextHearingDateOriginEarliest";
    String NEXT_HEARING_DATE_INTERVAL_DAYS = "nextHearingDateIntervalDays";
    String NEXT_HEARING_DATE_NON_WORKING_CALENDAR = "nextHearingDateNonWorkingCalendar";
    String NEXT_HEARING_DATE_NON_WORKING_DAYS_OF_WEEK = "nextHearingDateNonWorkingDaysOfWeek";
    String NEXT_HEARING_DATE_SKIP_NON_WORKING_DAYS = "nextHearingDateSkipNonWorkingDays";
    String NEXT_HEARING_DATE_MUST_BE_WORKING_DAYS = "nextHearingDateMustBeWorkingDay";

    String DEFAULT_NON_WORKING_CALENDAR = "https://www.gov.uk/bank-holidays/england-and-wales.json";
    String DEFAULT_DATE_TIME = "16:00";
    DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
    DateTimeFormatter DUE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    LocalDateTime DEFAULT_ZONED_DATE_TIME = LocalDateTime.now().plusDays(2)
        .withHour(16).withMinute(0).withSecond(0);
    LocalDateTime DEFAULT_DATE = LocalDateTime.now().plusDays(2);

    boolean supports(List<ConfigurationDmnEvaluationResponse> dueDateProperties,
                     DateType dateType,
                     boolean isReconfigureRequest);

    ConfigurationDmnEvaluationResponse calculateDate(
        DateType dateType,
        List<ConfigurationDmnEvaluationResponse> configResponses);

    default ConfigurationDmnEvaluationResponse getProperty(
        List<ConfigurationDmnEvaluationResponse> dueDateProperties, String dueDatePrefix) {
        return dueDateProperties.stream()
            .filter(r -> r.getName().getValue().equals(dueDatePrefix))
            .filter(r -> Strings.isNotBlank(r.getValue().getValue()))
            .reduce((a, b) -> b)
            .orElse(null);
    }

    default ConfigurationDmnEvaluationResponse getReConfigurableProperty(
        List<ConfigurationDmnEvaluationResponse> dueDateProperties, String dueDatePrefix) {
        return dueDateProperties.stream()
            .filter(r -> r.getName().getValue().equals(dueDatePrefix))
            .filter(r -> Strings.isNotBlank(r.getValue().getValue()))
            .filter(r -> r.getCanReconfigure().getValue())
            .reduce((a, b) -> b)
            .orElse(null);
    }

    default LocalDateTime addTimeToDate(ConfigurationDmnEvaluationResponse dueDateTimeResponse, LocalDateTime date) {
        String dueDateTime = dueDateTimeResponse.getValue().getValue();
        return useDateTime(date, dueDateTime);
    }

    default LocalDateTime parseDateTime(String inputDate) {
        try {
            ZoneId zoneId = ZoneId.systemDefault();
            ZonedDateTime zonedDateTime = ZonedDateTime.parse(inputDate).withZoneSameLocal(zoneId);
            return zonedDateTime.toLocalDateTime();
        } catch (DateTimeParseException p) {
            if (dateContainsTime(inputDate)) {
                return LocalDateTime.parse(inputDate, DATE_TIME_FORMATTER);
            } else {
                return LocalDate.parse(inputDate, DUE_DATE_FORMATTER).atStartOfDay();
            }
        }
    }

    default boolean dateContainsTime(String dueDate) {
        return dueDate.contains("T");
    }

    default LocalDateTime useDateTime(LocalDateTime date, String dueDateTime) {

        List<String> split = Arrays.asList(dueDateTime.replace("T", "").trim().split(":"));
        return date
            .with(ChronoField.HOUR_OF_DAY, Long.parseLong(split.get(0)))
            .with(ChronoField.MINUTE_OF_HOUR, Long.parseLong(split.get(1)))
            .with(ChronoField.SECOND_OF_MINUTE, 0)
            .with(ChronoField.NANO_OF_SECOND, 0);
    }

    default Optional<LocalDateTime> getOriginRefDate(
        List<ConfigurationDmnEvaluationResponse> configResponses,
        ConfigurationDmnEvaluationResponse originRefResponse) {
        List<DateType> originDateTypes = Arrays.stream(originRefResponse.getValue().getValue().split(","))
            .map(s -> DateType.from(s).orElseThrow()).toList();

        return originDateTypes.stream()
            .flatMap(r -> configResponses.stream()
                .filter(c -> DateType.from(c.getName().getValue()).isPresent()
                    && DateType.from(c.getName().getValue()).get().equals(r))
                .map(c -> LocalDateTime.parse(c.getValue().getValue(), DATE_TIME_FORMATTER)))
            .findFirst();
    }

    default Optional<LocalDateTime> getOriginEarliestDate(
        List<ConfigurationDmnEvaluationResponse> configResponses,
        ConfigurationDmnEvaluationResponse originEarliestResponse) {
        List<DateType> originDateTypes = Arrays.stream(originEarliestResponse.getValue().getValue().split(","))
            .map(s -> DateType.from(s).orElseThrow()).toList();

        return configResponses.stream()
            .filter(r -> DateType.from(r.getName().getValue()).isPresent()
                && originDateTypes.contains(DateType.from(r.getName().getValue()).get()))
            .map(r -> LocalDateTime.parse(r.getValue().getValue(), DATE_TIME_FORMATTER))
            .min(LocalDateTime::compareTo);
    }
}
