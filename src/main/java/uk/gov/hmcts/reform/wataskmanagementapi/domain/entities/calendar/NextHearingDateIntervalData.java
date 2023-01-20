package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.calendar;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class NextHearingDateIntervalData {

    private String nextHearingDateOrigin;
    private Long nextHearingDateIntervalDays;
    private List<String> nextHearingDateNonWorkingCalendar;
    private List<String> nextHearingDateNonWorkingDaysOfWeek;
    private boolean nextHearingDateSkipNonWorkingDays;
    private String nextHearingDateMustBeWorkingDay;
    public static final String NEXT_HEARING_DATE_MUST_BE_WORKING_DAY_NEXT = "Next";
    public static final String NEXT_HEARING_DATE_MUST_BE_WORKING_DAY_PREVIOUS = "Previous";
    public static final String NEXT_HEARING_DATE_MUST_BE_WORKING_DAY_NO = "No";
    private String nextHearingDateTime;
}
