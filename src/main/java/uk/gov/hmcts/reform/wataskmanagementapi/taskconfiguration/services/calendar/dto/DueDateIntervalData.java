package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.calendar.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class DueDateIntervalData {

    private String dueDateOrigin;
    private Long dueDateIntervalDays;
    private String dueDateNonWorkingCalendar;
    private List<String> dueDateNonWorkingDaysOfWeek;
    private boolean dueDateSkipNonWorkingDays;
    private boolean dueDateMustBeWorkingDay;
    private String dueDateTime;
}
