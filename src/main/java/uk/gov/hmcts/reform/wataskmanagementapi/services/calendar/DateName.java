package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import static java.util.Arrays.stream;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateCalculator.DEFAULT_ZONED_DATE_TIME;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateCalculator.DUE_DATE_TIME_FORMATTER;

public enum DateName {
    DUE_DATE("dueDate", DEFAULT_ZONED_DATE_TIME, DUE_DATE_TIME_FORMATTER),
    NEXT_HEARING_DATE("nextHearingDate", null, null),
    PRIORITY_DATE("priorityDate", null, null);

    private final String name;
    private final LocalDateTime defaultTime;
    private final DateTimeFormatter dateTimeFormatter;

    DateName(String name, LocalDateTime defaultTime, DateTimeFormatter dateTimeFormatter) {
        this.name = name;
        this.defaultTime = defaultTime;
        this.dateTimeFormatter = dateTimeFormatter;
    }

    public static Optional<DateName> from(String name) {
        return stream(values())
            .filter(v -> v.getName().equalsIgnoreCase(name))
            .findFirst();
    }

    public String getName() {
        return name;
    }

    public LocalDateTime getDefaultTime() {
        return defaultTime;
    }

    public DateTimeFormatter getDateTimeFormatter() {
        return dateTimeFormatter;
    }
}
