package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.List;

import static org.mockito.Mockito.when;

@TestConfiguration
public class DueDateTypeConfiguratorTestConfig {
    @Bean
    public PublicHolidaysCollection publicHolidaysCollection() {
        PublicHolidaysCollection mockCollection = Mockito.mock(PublicHolidaysCollection.class);
        // Return empty set of public holidays by default
        when(mockCollection.getPublicHolidays(Mockito.anyList()))
            .thenReturn(java.util.Collections.emptySet());
        return mockCollection;
    }

    @Bean
    public WorkingDayIndicator workingDayIndicator(PublicHolidaysCollection publicHolidaysCollection) {
        return new WorkingDayIndicator(publicHolidaysCollection);
    }

    @Bean
    public DueDateCalculator dueDateCalculator() {
        return new DueDateCalculator();
    }

    @Bean
    public DueDateTimeCalculator dueDateTimeCalculator() {
        return new DueDateTimeCalculator();
    }

    @Bean
    public DueDateIntervalCalculator dueDateIntervalCalculator(WorkingDayIndicator workingDayIndicator) {
        return new DueDateIntervalCalculator(workingDayIndicator);
    }

    @Bean
    public DueDateOriginRefCalculator dueDateOriginRefCalculator(WorkingDayIndicator workingDayIndicator) {
        return new DueDateOriginRefCalculator(workingDayIndicator);
    }

    @Bean
    public DueDateOriginEarliestCalculator dueDateOriginEarliestCalculator(
            WorkingDayIndicator workingDayIndicator) {
        return new DueDateOriginEarliestCalculator(workingDayIndicator);
    }

    @Bean
    public DueDateOriginLatestCalculator dueDateOriginLatestCalculator(WorkingDayIndicator workingDayIndicator) {
        return new DueDateOriginLatestCalculator(workingDayIndicator);
    }

    @Bean
    public PriorityDateCalculator priorityDateCalculator() {
        return new PriorityDateCalculator();
    }

    @Bean
    public PriorityDateTimeCalculator priorityDateTimeCalculator() {
        return new PriorityDateTimeCalculator();
    }

    @Bean
    public PriorityDateIntervalCalculator priorityDateIntervalCalculator(
            WorkingDayIndicator workingDayIndicator) {
        return new PriorityDateIntervalCalculator(workingDayIndicator);
    }

    @Bean
    public PriorityDateOriginRefCalculator priorityDateOriginRefCalculator(
            WorkingDayIndicator workingDayIndicator) {
        return new PriorityDateOriginRefCalculator(workingDayIndicator);
    }

    @Bean
    public PriorityDateOriginEarliestCalculator priorityDateOriginEarliestCalculator(
            WorkingDayIndicator workingDayIndicator) {
        return new PriorityDateOriginEarliestCalculator(workingDayIndicator);
    }

    @Bean
    public PriorityDateOriginLatestCalculator priorityDateOriginLatestCalculator(
            WorkingDayIndicator workingDayIndicator) {
        return new PriorityDateOriginLatestCalculator(workingDayIndicator);
    }

    @Bean
    public NextHearingDateCalculator nextHearingDateCalculator() {
        return new NextHearingDateCalculator();
    }

    @Bean
    public NextHearingDateIntervalCalculator nextHearingDateIntervalCalculator(
            WorkingDayIndicator workingDayIndicator) {
        return new NextHearingDateIntervalCalculator(workingDayIndicator);
    }

    @Bean
    public NextHearingDateOriginRefCalculator nextHearingDateOriginRefCalculator(
            WorkingDayIndicator workingDayIndicator) {
        return new NextHearingDateOriginRefCalculator(workingDayIndicator);
    }

    @Bean
    public NextHearingDateOriginEarliestCalculator nextHearingDateOriginEarliestCalculator(
            WorkingDayIndicator workingDayIndicator) {
        return new NextHearingDateOriginEarliestCalculator(workingDayIndicator);
    }

    @Bean
    public NextHearingDateOriginLatestCalculator nextHearingDateOriginLatestCalculator(
            WorkingDayIndicator workingDayIndicator) {
        return new NextHearingDateOriginLatestCalculator(workingDayIndicator);
    }

    @Bean
    public IntermediateDateCalculator intermediateDateCalculator() {
        return new IntermediateDateCalculator();
    }

    @Bean
    public IntermediateDateIntervalCalculator intermediateDateIntervalCalculator(
            WorkingDayIndicator workingDayIndicator) {
        return new IntermediateDateIntervalCalculator(workingDayIndicator);
    }

    @Bean
    public IntermediateDateOriginRefCalculator intermediateDateOriginRefCalculator(
            WorkingDayIndicator workingDayIndicator) {
        return new IntermediateDateOriginRefCalculator(workingDayIndicator);
    }

    @Bean
    public IntermediateDateOriginEarliestCalculator intermediateDateOriginEarliestCalculator(
            WorkingDayIndicator workingDayIndicator) {
        return new IntermediateDateOriginEarliestCalculator(workingDayIndicator);
    }

    @Bean
    public IntermediateDateOriginLatestCalculator intermediateDateOriginLatestCalculator(
            WorkingDayIndicator workingDayIndicator) {
        return new IntermediateDateOriginLatestCalculator(workingDayIndicator);
    }

    @Bean
    public DateTypeConfigurator dateTypeConfigurator(List<DateCalculator> dateCalculators) {
        return new DateTypeConfigurator(dateCalculators);
    }
}
