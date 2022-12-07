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
    private boolean dueDateMustBeWorkingDay;
    private String dueDateTime;
}
