package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateCalculator.DATE_TIME_FORMATTER;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateCalculator.DEFAULT_ZONED_DATE_TIME;

public enum DateType {
    DUE_DATE("dueDate", DEFAULT_ZONED_DATE_TIME, DATE_TIME_FORMATTER),
    NEXT_HEARING_DATE("nextHearingDate", null, DATE_TIME_FORMATTER),
    PRIORITY_DATE("priorityDate", null, DATE_TIME_FORMATTER);

    private final String type;
    private final LocalDateTime defaultTime;
    private final DateTimeFormatter dateTimeFormatter;

    DateType(String type, LocalDateTime defaultTime, DateTimeFormatter dateTimeFormatter) {
        this.type = type;
        this.defaultTime = defaultTime;
        this.dateTimeFormatter = dateTimeFormatter;
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
