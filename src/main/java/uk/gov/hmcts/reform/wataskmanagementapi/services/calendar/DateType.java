package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import static java.util.Arrays.stream;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateCalculator.DEFAULT_ZONED_DATE_TIME;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateCalculator.DUE_DATE_TIME_FORMATTER;

public enum DateType {
    DUE_DATE("dueDate", DEFAULT_ZONED_DATE_TIME, DUE_DATE_TIME_FORMATTER),
    NEXT_HEARING_DATE("nextHearingDate", DEFAULT_ZONED_DATE_TIME, DUE_DATE_TIME_FORMATTER),
    PRIORITY_DATE("priorityDate", DEFAULT_ZONED_DATE_TIME, DUE_DATE_TIME_FORMATTER);

    private final String type;
    private final LocalDateTime defaultTime;
    private final DateTimeFormatter dateTimeFormatter;

    DateType(String type, LocalDateTime defaultTime, DateTimeFormatter dateTimeFormatter) {
        this.type = type;
        this.defaultTime = defaultTime;
        this.dateTimeFormatter = dateTimeFormatter;
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
}
