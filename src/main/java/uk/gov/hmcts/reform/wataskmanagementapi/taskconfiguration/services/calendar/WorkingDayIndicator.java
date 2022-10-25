package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.calendar;

import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Tells if given day is a working day.
 */
@Component
public class WorkingDayIndicator {

    private final PublicHolidaysCollection publicHolidaysCollection;

    public WorkingDayIndicator(PublicHolidaysCollection publicHolidaysApiClient) {
        this.publicHolidaysCollection = publicHolidaysApiClient;
    }

    /**
     * Verifies if given date is a working day in UK (England and Wales only).
     */
    public boolean isWorkingDay(LocalDate date, String uri, List<String> nonWorkingDaysOfWeek) {
        return !isWeekend(date)
            && !isPublicHoliday(date, uri)
            && !isCustomNonWorkingDay(nonWorkingDaysOfWeek, date);
    }

    public boolean isWeekend(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    private boolean isPublicHoliday(LocalDate date, String uri) {
        return publicHolidaysCollection.getPublicHolidays(uri).contains(date);
    }

    public LocalDate getNextWorkingDay(LocalDate date, String uri, List<String> nonWorkingDaysOfWeek) {
        requireNonNull(date);
        date = date.plusDays(1);

        return isWorkingDay(date, uri, nonWorkingDaysOfWeek)
            ? date
            : getNextWorkingDay(date, uri, nonWorkingDaysOfWeek);
    }

    private boolean isCustomNonWorkingDay(List<String> nonWorkingDaysOfWeek, LocalDate localDate) {
        if (nonWorkingDaysOfWeek == null || nonWorkingDaysOfWeek.isEmpty()) {
            return false;
        }
        DayOfWeek dayOfWeek = localDate.getDayOfWeek();
        return nonWorkingDaysOfWeek.contains(dayOfWeek.toString());
    }
}
