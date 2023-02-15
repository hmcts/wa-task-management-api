package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import static java.util.Arrays.stream;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateCalculator.DATE_TIME_FORMATTER;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateCalculator.DEFAULT_ZONED_DATE_TIME;

public enum DateType {
    DUE_DATE("dueDate", DEFAULT_ZONED_DATE_TIME, DATE_TIME_FORMATTER, 2),
    NEXT_HEARING_DATE("nextHearingDate", null, DATE_TIME_FORMATTER, 1),
    PRIORITY_DATE("priorityDate", null, DATE_TIME_FORMATTER, 3);

    private final String type;
    private final LocalDateTime defaultTime;
    private final DateTimeFormatter dateTimeFormatter;
    private final int order;

    DateType(String type, LocalDateTime defaultTime, DateTimeFormatter dateTimeFormatter, int order) {
        this.type = type;
        this.defaultTime = defaultTime;
        this.dateTimeFormatter = dateTimeFormatter;
        this.order = order;
    }

    public static Optional<DateType> from(String name) {
        return stream(values())
            .filter(v -> v.getType().equalsIgnoreCase(name))
            .findFirst();
    }

    public String getType() {
        return type;
    }

    public LocalDateTime getDefaultTime() {
        return defaultTime;
    }

    public DateTimeFormatter getDateTimeFormatter() {
        return dateTimeFormatter;
    }

    public int getOrder() {
        return order;
    }
}
