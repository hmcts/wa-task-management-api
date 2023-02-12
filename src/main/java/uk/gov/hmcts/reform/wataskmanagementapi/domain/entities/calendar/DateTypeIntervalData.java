package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.calendar;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Builder(toBuilder = true)
@Getter
public class DateTypeIntervalData {

    private String dateTypeOrigin;
    private Long dateTypeIntervalDays;
    private List<String> dateTypeNonWorkingCalendar;
    private List<String> dateTypeNonWorkingDaysOfWeek;
    private boolean dateTypeSkipNonWorkingDays;
    private String dateTypeMustBeWorkingDay;
    public static final String DATE_TYPE_MUST_BE_WORKING_DAY_NEXT = "Next";
    public static final String DATE_TYPE_MUST_BE_WORKING_DAY_PREVIOUS = "Previous";
    public static final String DATE_TYPE_MUST_BE_WORKING_DAY_NO = "No";
    private String dateTypeTime;
    private LocalDateTime calculatedRefDate;
    private LocalDateTime calculatedEarliestDate;
}