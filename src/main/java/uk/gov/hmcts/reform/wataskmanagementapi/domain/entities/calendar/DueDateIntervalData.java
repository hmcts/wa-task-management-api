package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.calendar;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class DueDateIntervalData {

    private String dueDateOrigin;
    private Long dueDateIntervalDays;
    private List<String> dueDateNonWorkingCalendar;
    private List<String> dueDateNonWorkingDaysOfWeek;
    private boolean dueDateSkipNonWorkingDays;
    private String dueDateMustBeWorkingDay;
    public static final String DUE_DATE_MUST_BE_WORKING_DAY_NEXT = "Next";
    public static final String DUE_DATE_MUST_BE_WORKING_DAY_PREVIOUS = "Previous";
    public static final String DUE_DATE_MUST_BE_WORKING_DAY_NO = "No";
    private String dueDateTime;
}
