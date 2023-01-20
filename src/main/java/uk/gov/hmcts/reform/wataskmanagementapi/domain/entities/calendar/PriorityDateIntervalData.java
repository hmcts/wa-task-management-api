package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.calendar;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class PriorityDateIntervalData {

    private String priorityDateOrigin;
    private Long priorityDateIntervalDays;
    private List<String> priorityDateNonWorkingCalendar;
    private List<String> priorityDateNonWorkingDaysOfWeek;
    private boolean priorityDateSkipNonWorkingDays;
    private String priorityDateMustBeWorkingDay;
    public static final String PRIORITY_DATE_MUST_BE_WORKING_DAY_NEXT = "Next";
    public static final String PRIORITY_DATE_MUST_BE_WORKING_DAY_PREVIOUS = "Previous";
    public static final String PRIORITY_DATE_MUST_BE_WORKING_DAY_NO = "No";
    private String priorityDateTime;
}
