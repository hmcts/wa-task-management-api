package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import org.apache.logging.log4j.util.Strings;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.DateCalculationException;
import uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateTypeConfigurator.DateTypeObject;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public interface DateCalculator {
    String ORIGIN_SUFFIX = "Origin";
    String INTERVAL_DAYS_SUFFIX = "IntervalDays";
    String NON_WORKING_CALENDAR_SUFFIX = "NonWorkingCalendar";
    String NON_WORKING_DAYS_OF_WEEK_SUFFIX = "NonWorkingDaysOfWeek";
    String SKIP_NON_WORKING_DAYS_SUFFIX = "SkipNonWorkingDays";
    String MUST_BE_WORKING_DAY_SUFFIX = "MustBeWorkingDay";
    String ORIGIN_REF_SUFFIX = "OriginRef";
    String TIME_SUFFIX = "Time";
    String ORIGIN_EARLIEST_SUFFIX = "OriginEarliest";
    String ORIGIN_LATEST_SUFFIX = "OriginLatest";
    String DUE_DATE_ORIGIN = "dueDateOrigin";
    String DUE_DATE_ORIGIN_REF = "dueDateOriginRef";
    String DUE_DATE_ORIGIN_EARLIEST = "dueDateOriginEarliest";
    String DUE_DATE_INTERVAL_DAYS = "dueDateIntervalDays";
    String DUE_DATE_NON_WORKING_CALENDAR = "dueDateNonWorkingCalendar";
    String DUE_DATE_NON_WORKING_DAYS_OF_WEEK = "dueDateNonWorkingDaysOfWeek";
    String DUE_DATE_SKIP_NON_WORKING_DAYS = "dueDateSkipNonWorkingDays";
    String DUE_DATE_MUST_BE_WORKING_DAYS = "dueDateMustBeWorkingDay";
    String DUE_DATE_TIME = "dueDateTime";
    String DUE_DATE_ORIGIN_LATEST = "dueDateOriginLatest";
    String PRIORITY_DATE_ORIGIN = "priorityDateOrigin";
    String PRIORITY_DATE_ORIGIN_REF = "priorityDateOriginRef";
    String PRIORITY_DATE_ORIGIN_EARLIEST = "priorityDateOriginEarliest";
    String PRIORITY_DATE_ORIGIN_LATEST = "priorityDateOriginLatest";
    String PRIORITY_DATE_INTERVAL_DAYS = "priorityDateIntervalDays";
    String PRIORITY_DATE_NON_WORKING_CALENDAR = "priorityDateNonWorkingCalendar";
    String PRIORITY_DATE_NON_WORKING_DAYS_OF_WEEK = "priorityDateNonWorkingDaysOfWeek";
    String PRIORITY_DATE_SKIP_NON_WORKING_DAYS = "priorityDateSkipNonWorkingDays";
    String PRIORITY_DATE_MUST_BE_WORKING_DAYS = "priorityDateMustBeWorkingDay";
    String PRIORITY_DATE_TIME = "priorityDateTime";
    String NEXT_HEARING_DATE_TIME = "nextHearingDateTime";
    String NEXT_HEARING_DATE_ORIGIN = "nextHearingDateOrigin";
    String NEXT_HEARING_DATE_ORIGIN_LATEST = "nextHearingDateOriginLatest";
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
    DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    LocalDateTime DEFAULT_ZONED_DATE_TIME = LocalDateTime.now().plusDays(2).withHour(16).withMinute(0).withSecond(0);
    LocalDateTime DEFAULT_DATE = LocalDateTime.now().plusDays(2);
    String INVALID_DATE_REFERENCE_FIELD = "Invalid Date reference field %s. Referred field is not yet available.";

    boolean supports(List<ConfigurationDmnEvaluationResponse> dueDateProperties,
                     DateTypeObject dateTypeObject,
                     boolean isReconfigureRequest);

    ConfigurationDmnEvaluationResponse calculateDate(
        List<ConfigurationDmnEvaluationResponse> configResponses,
        DateTypeObject dateType,
        boolean isReconfigureRequest,
        Map<String, Object> taskAttributes,
        List<ConfigurationDmnEvaluationResponse> calculatedConfigurations);

    default ConfigurationDmnEvaluationResponse getProperty(
        List<ConfigurationDmnEvaluationResponse> dueDateProperties,
        String dueDatePrefix,
        boolean isReconfigureRequest) {
        return dueDateProperties.stream()
            .filter(r -> r.getName().getValue().equals(dueDatePrefix))
            .filter(r -> Strings.isNotBlank(r.getValue().getValue()))
            .filter(r -> !isReconfigureRequest || r.getCanReconfigure() != null && r.getCanReconfigure().getValue())
            .reduce((a, b) -> b)
            .orElse(null);
    }

    default boolean isPropertyEmptyIrrespectiveOfReconfiguration(
        List<ConfigurationDmnEvaluationResponse> dateTypeProperties,
        String dateTypePrefix) {
        return dateTypeProperties.stream()
            .filter(r -> r.getName().getValue().equals(dateTypePrefix))
            .filter(r -> Strings.isNotBlank(r.getValue().getValue()))
            .reduce((a, b) -> b)
            .isEmpty();
    }

    default LocalDateTime addTimeToDate(
        ConfigurationDmnEvaluationResponse dueDateTimeResponse, LocalDateTime date) {
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
                Optional<LocalDateTime> calculated = parseDateTime(inputDate, DateTimeFormatter.ISO_DATE_TIME);
                return calculated
                    .orElseThrow(() -> new RuntimeException("Provided date has invalid format: " + inputDate));
            } else {
                return LocalDate.parse(inputDate, DATE_FORMATTER).atStartOfDay();
            }
        }
    }

    default Optional<LocalDateTime> parseDateTime(String inputDate, DateTimeFormatter formatter) {
        try {
            return Optional.of(LocalDateTime.parse(inputDate, formatter));
        } catch (DateTimeParseException e) {
            return Optional.empty();
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
        ConfigurationDmnEvaluationResponse originRefResponse,
        Map<String, Object> taskAttributes,
        boolean isReconfigureRequest) {
        return getReferenceDateValues(
            configResponses,
            taskAttributes,
            isReconfigureRequest,
            getReferenceDates(originRefResponse)
        ).findFirst();
    }

    private static List<DateTypeObject> getReferenceDates(ConfigurationDmnEvaluationResponse originRefResponse) {
        return Arrays.stream(originRefResponse.getValue().getValue().split(","))
            .map(s -> new DateTypeObject(DateType.from(s), s)).toList();
    }

    default Optional<LocalDateTime> getOriginEarliestDate(
        List<ConfigurationDmnEvaluationResponse> configResponses,
        ConfigurationDmnEvaluationResponse originEarliestResponse,
        Map<String, Object> taskAttributes,
        boolean isReconfigureRequest) {
        return getReferenceDateValues(
            configResponses,
            taskAttributes,
            isReconfigureRequest,
            getReferenceDates(originEarliestResponse)
        ).min(LocalDateTime::compareTo);
    }

    default Optional<LocalDateTime> getOriginLatestDate(
        List<ConfigurationDmnEvaluationResponse> configResponses,
        ConfigurationDmnEvaluationResponse originLatestResponse,
        Map<String, Object> taskAttributes,
        boolean isReconfigureRequest) {

        return getReferenceDateValues(
            configResponses,
            taskAttributes,
            isReconfigureRequest,
            getReferenceDates(originLatestResponse)
        ).max(LocalDateTime::compareTo);
    }

    private Stream<LocalDateTime> getReferenceDateValues(
        List<ConfigurationDmnEvaluationResponse> calculatedConfigurations,
        Map<String, Object> taskAttributes,
        boolean isReconfigureRequest,
        List<DateTypeObject> referenceDates) {
        return referenceDates.stream()
            .map(DateTypeObject::dateTypeName)
            .map(t -> getMatchingConfigResponseDate(calculatedConfigurations, taskAttributes, isReconfigureRequest, t))
            .filter(Objects::nonNull);
    }

    private LocalDateTime getMatchingConfigResponseDate(
        List<ConfigurationDmnEvaluationResponse> calculatedConfigurations,
        Map<String, Object> taskAttributes,
        boolean isReconfigureRequest,
        String dateTypeName) {

        throwErrorWhenDateTypeIsNotPresentInAlreadyCalculatedConfigurationsAndIsNotReconfiguration(
            calculatedConfigurations, isReconfigureRequest, dateTypeName);

        return calculatedConfigurations.stream()
            .filter(Objects::nonNull)
            .filter(c1 -> c1.getName().getValue().equals(dateTypeName))
            .filter(c -> !c.getValue().getValue().isBlank())
            .map(c -> LocalDateTime.parse(c.getValue().getValue(), DATE_TIME_FORMATTER))
            .findFirst()
            .orElseGet(() -> defaultWithTaskAttributes(taskAttributes, isReconfigureRequest, dateTypeName));
    }

    private void throwErrorWhenDateTypeIsNotPresentInAlreadyCalculatedConfigurationsAndIsNotReconfiguration(
        List<ConfigurationDmnEvaluationResponse> calculatedConfigurations,
        boolean isReconfigureRequest, String dateTypeName) {
        boolean dateTypePresentInCalculatedConfigurations = calculatedConfigurations.stream()
            .filter(Objects::nonNull)
            .anyMatch(c1 -> c1.getName().getValue().equals(dateTypeName));
        if (!dateTypePresentInCalculatedConfigurations
            && !isAlreadyConfiguredDate(isReconfigureRequest, dateTypeName)) {
            throw new DateCalculationException(String.format(INVALID_DATE_REFERENCE_FIELD, dateTypeName));
        }
    }

    private LocalDateTime defaultWithTaskAttributes(
        Map<String, Object> taskAttributes,
        boolean isReconfigureRequest,
        String dateTypeName) {
        return isAlreadyConfiguredDate(isReconfigureRequest, dateTypeName)
            ? getTaskAttributeDate(taskAttributes, dateTypeName)
            : null;
    }

    private boolean isAlreadyConfiguredDate(boolean isReconfigureRequest, String dateTypeName) {
        return isReconfigureRequest && List.of("dueDate", "priorityDate", "nextHearingDate").contains(dateTypeName);
    }

    private LocalDateTime getTaskAttributeDate(Map<String, Object> taskAttributes, String keyName) {
        Object dateObject = taskAttributes.get(keyName);
        return Optional.ofNullable(dateObject).map(d -> parseDateTime(d.toString())).orElse(null);
    }
}
