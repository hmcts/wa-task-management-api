package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.calendar;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.response.ConfigurationDmnEvaluationResponse;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
public class DueDateCalculatorTest {

    private DueDateCalculator dueDateCalculator;

    @Mock
    private BankHolidaysApi bankHolidaysApi;
    @Mock
    private  BankHolidays bankHolidays;

    @Before
    public void before() {
        dueDateCalculator = new DueDateCalculator(new DueDateOriginBasedCalculator(
            new WorkingDayIndicator(new PublicHolidaysCollection())));
    }

    @Test
    public void shouldCalculateDueDateWhenMultipleDueDateAndTimesAreAvailable() {

        String expectedDueDate = LocalDateTime.now().plusDays(2)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse defaultDueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(expectedDueDate + "T16:00"))
            .build();

        ConfigurationDmnEvaluationResponse defaultDueDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateTime"))
            .value(CamundaValue.stringValue("16:00"))
            .build();

        ConfigurationDmnEvaluationResponse dueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(expectedDueDate + "T18:00"))
            .build();

        ConfigurationDmnEvaluationResponse dueDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateTime"))
            .value(CamundaValue.stringValue("18:00"))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dueDateCalculator
            .calculateDueDate(List.of(defaultDueDate, defaultDueDateTime, dueDate, dueDateTime), "WA");

        Assertions.assertThat(configurationDmnEvaluationResponses).hasSize(1)
            .isEqualTo(List.of(ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("dueDate"))
                                   .value(CamundaValue.stringValue(expectedDueDate + "T18:00"))
                                   .build()));
    }

    @Test
    public void shouldCalculateDueDateWhenMultipleDueDatesAreAvailable() {

        String expectedDueDate = LocalDateTime.now().plusDays(2)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse defaultDueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(expectedDueDate + "T16:00"))
            .build();

        ConfigurationDmnEvaluationResponse dueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(expectedDueDate + "T18:00"))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dueDateCalculator
            .calculateDueDate(List.of(defaultDueDate, dueDate), "WA");

        Assertions.assertThat(configurationDmnEvaluationResponses).hasSize(1)
            .isEqualTo(List.of(ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("dueDate"))
                                   .value(CamundaValue.stringValue(expectedDueDate + "T18:00"))
                                   .build()));
    }

    @Test
    public void shouldCalculateDueDateWhenMultipleDueDateTimesAreAvailable() {

        String expectedDueDate = LocalDateTime.now().plusDays(2)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse defaultDueDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateTime"))
            .value(CamundaValue.stringValue("16:00"))
            .build();

        ConfigurationDmnEvaluationResponse dueDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateTime"))
            .value(CamundaValue.stringValue("18:00"))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dueDateCalculator
            .calculateDueDate(List.of(defaultDueDateTime, dueDateTime), "WA");

        Assertions.assertThat(configurationDmnEvaluationResponses).hasSize(1)
            .isEqualTo(List.of(ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("dueDate"))
                                   .value(CamundaValue.stringValue(expectedDueDate + "T18:00"))
                                   .build()));
    }

    @Test
    public void shouldCalculateDueDateWhenDefaultDueDateWithoutTimeAndTimeAreAvailable() {

        String expectedDueDate = LocalDateTime.now().plusDays(2)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse defaultDueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(expectedDueDate))
            .build();

        ConfigurationDmnEvaluationResponse defaultDueDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateTime"))
            .value(CamundaValue.stringValue("16:00"))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dueDateCalculator
            .calculateDueDate(List.of(defaultDueDate, defaultDueDateTime), "WA");

        Assertions.assertThat(configurationDmnEvaluationResponses).hasSize(1)
            .isEqualTo(List.of(ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("dueDate"))
                                   .value(CamundaValue.stringValue(expectedDueDate + "T16:00"))
                                   .build()));
    }

    @Test
    public void shouldCalculateDueDateWhenOnlyDefaultDueDateTimeIsAvailable() {

        String expectedDueDate = LocalDateTime.now().plusDays(2)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse defaultDueDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateTime"))
            .value(CamundaValue.stringValue("16:00"))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dueDateCalculator
            .calculateDueDate(List.of(defaultDueDateTime), "WA");

        Assertions.assertThat(configurationDmnEvaluationResponses).hasSize(1)
            .isEqualTo(List.of(ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("dueDate"))
                                   .value(CamundaValue.stringValue(expectedDueDate + "T16:00"))
                                   .build()));
    }


    @Test
    public void shouldCalculateDueDateWhenOnlyDefaultDueDateWithTimeIsAvailable() {

        String expectedDueDate = LocalDateTime.now().plusDays(2)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse defaultDueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(expectedDueDate + "T16:00"))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dueDateCalculator
            .calculateDueDate(List.of(defaultDueDate), "WA");

        Assertions.assertThat(configurationDmnEvaluationResponses).hasSize(1)
            .isEqualTo(List.of(ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("dueDate"))
                                   .value(CamundaValue.stringValue(expectedDueDate + "T16:00"))
                                   .build()));
    }

    @Test
    public void shouldCalculateDueDateWhenOnlyDefaultDueDateWithoutTimeIsAvailable() {

        String expectedDueDate = LocalDateTime.now().plusDays(2)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse defaultDueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(expectedDueDate))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dueDateCalculator
            .calculateDueDate(List.of(defaultDueDate), "WA");

        Assertions.assertThat(configurationDmnEvaluationResponses).hasSize(1)
            .isEqualTo(List.of(ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("dueDate"))
                                   .value(CamundaValue.stringValue(expectedDueDate + "T16:00"))
                                   .build()));
    }

    @Test
    public void shouldNotReturnDueDateWhenNoDueDatePropertiesAreAvailableAndJurisdictionIsIA() {
        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dueDateCalculator
            .calculateDueDate(List.of(), "IA");

        Assertions.assertThat(configurationDmnEvaluationResponses).isEmpty();
    }

    @Test
    public void shouldReturnDefaultDueDateWhenNoDueDatePropertiesAreNotAvailableAndJurisdictionIsWA() {
        String expectedDueDate = LocalDateTime.now().plusDays(2)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dueDateCalculator
            .calculateDueDate(List.of(), "WA");

        Assertions.assertThat(configurationDmnEvaluationResponses).hasSize(1)
            .isEqualTo(List.of(ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("dueDate"))
                                   .value(CamundaValue.stringValue(expectedDueDate + "T16:00"))
                                   .build()));
    }

    @Test
    public void shouldReturnCalculateDueDateWhenDueDateOriginPropertiesAreProvided() {
        LocalDateTime localDateTime = LocalDateTime.now().plusDays(2);

        String expectedDueDate = localDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse defaultDueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(expectedDueDate + "T16:00"))
            .build();

        ConfigurationDmnEvaluationResponse dueDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOrigin"))
            .value(CamundaValue.stringValue(expectedDueDate + "T20:00"))
            .build();

        ConfigurationDmnEvaluationResponse dueDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateTime"))
            .value(CamundaValue.stringValue("18:00"))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dueDateCalculator
            .calculateDueDate(List.of(defaultDueDate, dueDateOrigin, dueDateTime), "WA");

        Assertions.assertThat(configurationDmnEvaluationResponses).hasSize(1)
            .isEqualTo(List.of(ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("dueDate"))
                                   .value(CamundaValue.stringValue(expectedDueDate + "T18:00"))
                                   .build()));
    }
}
