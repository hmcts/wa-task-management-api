package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.wataskmanagementapi.config.IntegrationIdamStubConfig;
import uk.gov.hmcts.reform.wataskmanagementapi.config.IntegrationSecurityTestConfig;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.ConfigurationDmnEvaluationResponse;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateCalculator.DEFAULT_DATE;

@SpringBootTest
@Import({IntegrationSecurityTestConfig.class, IntegrationIdamStubConfig.class})
@ActiveProfiles({"integration"})
public class NextHearingDateTypeConfiguratorTest {

    public static final LocalDateTime GIVEN_DATE = LocalDateTime.of(2022, 10, 13, 18, 0, 0);

    public static final LocalDateTime BST_DATE_BACKWARD = LocalDateTime.of(2022, 10, 26, 18, 0, 0);
    public static final LocalDateTime BST_DATE_FORWARD = LocalDateTime.of(2023, 3, 26, 18, 0, 0);

    public static final String EXPECTED_DEFAULT_DUE_DATE
        = DEFAULT_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    @Autowired
    private DateTypeConfigurator dateTypeConfigurator;
    private String isReConfigurationRequest = "false";
    private final Map<String, Object> taskAttributes = new HashMap<>();

    @ParameterizedTest
    @CsvSource(value = {
        "true,true"
    })
    public void shouldCalculateNextHearingDateWhenMultipleNextHearingDateOriginsAreAvailable(
        String canConfigure,
        String initiationNextHearingDateFound) {

        String firstNextHearingDate = GIVEN_DATE.plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String secondNextHearingDate = GIVEN_DATE.plusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String thirdNextHearingDate = GIVEN_DATE.plusDays(3).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse defaultNextHearingDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateOrigin"))
            .value(CamundaValue.stringValue(firstNextHearingDate + "T16:00"))
            .canReconfigure(CamundaValue.booleanValue(Boolean.parseBoolean(canConfigure)))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateOrigin"))
            .value(CamundaValue.stringValue(secondNextHearingDate + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(Boolean.parseBoolean(canConfigure)))
            .build();

        ConfigurationDmnEvaluationResponse thirdNextHearingDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateOrigin"))
            .value(CamundaValue.stringValue(thirdNextHearingDate + "T10:00"))
            .canReconfigure(CamundaValue.booleanValue(Boolean.parseBoolean(canConfigure)))
            .build();
        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDates(
                List.of(defaultNextHearingDateOrigin, nextHearingDateOrigin, thirdNextHearingDateOrigin),
                Boolean.parseBoolean(initiationNextHearingDateFound),
                Boolean.parseBoolean(isReConfigurationRequest),
                taskAttributes
            );

        assertThat(configurationDmnEvaluationResponses).hasSize(1)
            .isEqualTo(List.of(ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("nextHearingDate"))
                                   .value(CamundaValue.stringValue(thirdNextHearingDate + "T10:00"))
                                   .canReconfigure(CamundaValue.booleanValue(Boolean.parseBoolean(
                                       isReConfigurationRequest)))
                                   .build()));
    }

    @ParameterizedTest
    @CsvSource(value = {
        "true,T20:00"
    })
    public void shouldCalculateNextHearingDateWhenMultipleNextHearingDateAndTimesAreAvailable(
        String canConfigure,
        String expectedTime) {

        String firstNextHearingDate = GIVEN_DATE.plusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String secondNextHearingDate = GIVEN_DATE.plusDays(9).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse defaultNextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(firstNextHearingDate + "T16:00"))
            .canReconfigure(CamundaValue.booleanValue(Boolean.parseBoolean(canConfigure)))
            .build();

        ConfigurationDmnEvaluationResponse defaultNextHearingDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateTime"))
            .value(CamundaValue.stringValue("10:00"))
            .canReconfigure(CamundaValue.booleanValue(Boolean.parseBoolean(canConfigure)))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(secondNextHearingDate + "T18:00"))
            .canReconfigure(CamundaValue.booleanValue(Boolean.parseBoolean(canConfigure)))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateTime"))
            .value(CamundaValue.stringValue("20:00"))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDates(
                List.of(
                    defaultNextHearingDate,
                    defaultNextHearingDateTime,
                    nextHearingDate,
                    nextHearingDateTime
                ),
                false,
                Boolean.parseBoolean(isReConfigurationRequest),
                taskAttributes
            );

        assertThat(configurationDmnEvaluationResponses).hasSize(3)
            .isEqualTo(List.of(
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("nextHearingDate"))
                    .value(CamundaValue.stringValue(secondNextHearingDate + expectedTime))
                    .canReconfigure(CamundaValue.booleanValue(Boolean.parseBoolean(isReConfigurationRequest)))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("dueDate"))
                    .value(CamundaValue.stringValue(EXPECTED_DEFAULT_DUE_DATE + "T16:00"))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("priorityDate"))
                    .value(CamundaValue.stringValue(EXPECTED_DEFAULT_DUE_DATE + "T16:00"))
                    .build()
            ));
    }

    @ParameterizedTest
    @CsvSource(value = {
        "true, true"
    })
    public void shouldCalculateNextHearingDateWhenMultipleNextHearingDatesAreAvailable(
        String canConfigure,
        String initiationNextHearingDateFound) {

        String firstNextHearingDate = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String secondNextHearingDate = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse defaultNextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(firstNextHearingDate + "T16:00"))
            .canReconfigure(CamundaValue.booleanValue(Boolean.parseBoolean(canConfigure)))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(secondNextHearingDate + "T18:00"))
            .canReconfigure(CamundaValue.booleanValue(Boolean.parseBoolean(canConfigure)))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDates(
                List.of(defaultNextHearingDate, nextHearingDate),
                Boolean.parseBoolean(initiationNextHearingDateFound),
                Boolean.parseBoolean(isReConfigurationRequest),
                taskAttributes
            );

        assertThat(configurationDmnEvaluationResponses).hasSize(1)
            .isEqualTo(List.of(ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("nextHearingDate"))
                                   .value(CamundaValue.stringValue(secondNextHearingDate + "T18:00"))
                                   .canReconfigure(CamundaValue.booleanValue(Boolean.parseBoolean(
                                       isReConfigurationRequest)))
                                   .build()));
    }

    @ParameterizedTest
    @CsvSource(value = {
        "true,true"
    })
    public void shouldCalculateNextHearingDateWhenMultipleNextHearingDateTimesAreAvailable(
        String canConfigure,
        String initiationNextHearingDateFound
    ) {

        ConfigurationDmnEvaluationResponse defaultNextHearingDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateTime"))
            .value(CamundaValue.stringValue("16:00"))
            .canReconfigure(CamundaValue.booleanValue(Boolean.parseBoolean(canConfigure)))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateTime"))
            .value(CamundaValue.stringValue("18:00"))
            .canReconfigure(CamundaValue.booleanValue(Boolean.parseBoolean(canConfigure)))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDates(
                List.of(defaultNextHearingDateTime, nextHearingDateTime),
                Boolean.parseBoolean(initiationNextHearingDateFound),
                Boolean.parseBoolean(isReConfigurationRequest),
                taskAttributes
            );

        String defaultNextHearingDate = DEFAULT_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        assertThat(configurationDmnEvaluationResponses).hasSize(0);
    }

    @ParameterizedTest
    @CsvSource(value = {
        "true,true"
    })
    public void shouldCalculateNextHearingDateWhenDefaultNextHearingDateWithoutTimeAndTimeAreAvailable(
        String canConfigure,
        String initiationNextHearingDateFound
    ) {

        String givenNextHearingDate = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse defaultNextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(givenNextHearingDate))
            .canReconfigure(CamundaValue.booleanValue(Boolean.parseBoolean(canConfigure)))
            .build();

        ConfigurationDmnEvaluationResponse defaultNextHearingDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateTime"))
            .value(CamundaValue.stringValue("16:00"))
            .canReconfigure(CamundaValue.booleanValue(Boolean.parseBoolean(canConfigure)))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDates(
                List.of(defaultNextHearingDate, defaultNextHearingDateTime),
                Boolean.parseBoolean(initiationNextHearingDateFound),
                Boolean.parseBoolean(isReConfigurationRequest),
                taskAttributes
            );

        assertThat(configurationDmnEvaluationResponses).hasSize(1)
            .isEqualTo(List.of(ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("nextHearingDate"))
                                   .value(CamundaValue.stringValue(givenNextHearingDate + "T16:00"))
                                   .canReconfigure(CamundaValue.booleanValue(Boolean.parseBoolean(
                                       isReConfigurationRequest)))
                                   .build()));
    }

    @ParameterizedTest
    @CsvSource(value = {
        "true,true"
    })
    public void shouldNotCalculateNextHearingDateWhenOnlyNextHearingDateTimeIsAvailable(
        String canConfigure,
        String initiationNextHearingDateFound
    ) {

        ConfigurationDmnEvaluationResponse defaultNextHearingDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateTime"))
            .value(CamundaValue.stringValue("16:00"))
            .canReconfigure(CamundaValue.booleanValue(Boolean.parseBoolean(canConfigure)))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDates(
                List.of(defaultNextHearingDateTime),
                Boolean.parseBoolean(initiationNextHearingDateFound),
                Boolean.parseBoolean(isReConfigurationRequest),
                taskAttributes
            );

        assertThat(configurationDmnEvaluationResponses).hasSize(0);
    }

    @ParameterizedTest
    @CsvSource(value = {
        "true,true"
    })
    public void shouldCalculateNextHearingDateWhenOnlyDefaultNextHearingDateWithTimeIsAvailable(
        String canConfigure,
        String initiationNextHearingDateFound
    ) {

        String givenNextHearingDate = GIVEN_DATE.plusDays(7).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse defaultNextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(givenNextHearingDate + "T19:00"))
            .canReconfigure(CamundaValue.booleanValue(Boolean.parseBoolean(canConfigure)))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDates(
                List.of(defaultNextHearingDate),
                Boolean.parseBoolean(initiationNextHearingDateFound),
                Boolean.parseBoolean(isReConfigurationRequest),
                taskAttributes
            );

        assertThat(configurationDmnEvaluationResponses).hasSize(1)
            .isEqualTo(List.of(ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("nextHearingDate"))
                                   .value(CamundaValue.stringValue(givenNextHearingDate + "T19:00"))
                                   .canReconfigure(CamundaValue.booleanValue(Boolean.parseBoolean(
                                       isReConfigurationRequest)))
                                   .build()));
    }

    @Test
    public void shouldNotReturnNextHearingDateWhenNoNextHearingDatePropertiesAreAvailableAndJurisdictionIsIA() {
        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDates(List.of(), false, true, taskAttributes);

        assertThat(configurationDmnEvaluationResponses).isEmpty();
    }

    @Test
    public void shouldReturnDefaultNextHearingDateWhenNoNextHearingDatePropertiesAreAvailableAndJurisdictionIsIA() {
        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDates(List.of(), false, false, taskAttributes);

        String expectedNextHearingDate = DEFAULT_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        assertThat(configurationDmnEvaluationResponses).hasSize(2)
            .isEqualTo(List.of(
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("dueDate"))
                    .value(CamundaValue.stringValue(EXPECTED_DEFAULT_DUE_DATE + "T16:00"))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("priorityDate"))
                    .value(CamundaValue.stringValue(EXPECTED_DEFAULT_DUE_DATE + "T16:00"))
                    .build()
            ));
    }

    @Test
    public void shouldReturnDefaultNextHearingDateWhenPropertiesAreNotAvailableAndInitiationNextHearingDateNotFound() {

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDates(List.of(), false, false, taskAttributes);

        String expectedDueDate = DEFAULT_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        assertThat(configurationDmnEvaluationResponses).hasSize(2)
            .isEqualTo(List.of(
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("dueDate"))
                    .value(CamundaValue.stringValue(EXPECTED_DEFAULT_DUE_DATE + "T16:00"))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("priorityDate"))
                    .value(CamundaValue.stringValue(EXPECTED_DEFAULT_DUE_DATE + "T16:00"))
                    .build()
            ));
    }

    @ParameterizedTest
    @CsvSource(value = {
        "true,true"
    })
    public void shouldCalculateNextHearingDateWhenOnlyNextHearingDateAndNextHearingDateOriginBothProvided(
        String canConfigure,
        String initiationNextHearingDateFound
    ) {
        String givenNextHearingDate = GIVEN_DATE.plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String givenNextHearingDateOrigin = GIVEN_DATE.plusDays(4).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(givenNextHearingDate + "T16:00"))
            .canReconfigure(CamundaValue.booleanValue(Boolean.parseBoolean(canConfigure)))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateOrigin"))
            .value(CamundaValue.stringValue(givenNextHearingDateOrigin + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(Boolean.parseBoolean(canConfigure)))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDates(
                List.of(nextHearingDate, nextHearingDateOrigin),
                Boolean.parseBoolean(initiationNextHearingDateFound),
                Boolean.parseBoolean(isReConfigurationRequest),
                taskAttributes
            );

        assertThat(configurationDmnEvaluationResponses).hasSize(1)
            .isEqualTo(List.of(ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("nextHearingDate"))
                                   .value(CamundaValue.stringValue(givenNextHearingDate + "T16:00"))
                                   .canReconfigure(CamundaValue.booleanValue(Boolean.parseBoolean(
                                       isReConfigurationRequest)))
                                   .build()));

    }

    @Test
    public void shouldCalculateNextHearingDateWhenOnlyNextHearingDateOriginIsProvided() {
        String givenNextHearingDate = GIVEN_DATE.plusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse nextHearingDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateOrigin"))
            .value(CamundaValue.stringValue(givenNextHearingDate + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDates(List.of(nextHearingDateOrigin), false,
                            Boolean.parseBoolean(isReConfigurationRequest),
                            taskAttributes
            );

        assertThat(configurationDmnEvaluationResponses).hasSize(3)
            .isEqualTo(List.of(
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("nextHearingDate"))
                    .value(CamundaValue.stringValue(givenNextHearingDate + "T20:00"))
                    .canReconfigure(CamundaValue.booleanValue(Boolean.parseBoolean(isReConfigurationRequest)))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("dueDate"))
                    .value(CamundaValue.stringValue(EXPECTED_DEFAULT_DUE_DATE + "T16:00"))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("priorityDate"))
                    .value(CamundaValue.stringValue(EXPECTED_DEFAULT_DUE_DATE + "T16:00"))
                    .build()
            ));

    }

    @Test
    public void shouldCalculateDateFromNextHearingDateEvenWhenNextHearingDateOriginPropertiesAreProvided() {

        String expectedNextHearingDate = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse defaultNextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(expectedNextHearingDate + "T16:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateOrigin"))
            .value(CamundaValue.stringValue(expectedNextHearingDate + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateTime"))
            .value(CamundaValue.stringValue("18:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDates(List.of(defaultNextHearingDate, nextHearingDateOrigin, nextHearingDateTime), false,
                            Boolean.parseBoolean(isReConfigurationRequest),
                            taskAttributes
            );

        assertThat(configurationDmnEvaluationResponses).hasSize(3)
            .isEqualTo(List.of(
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("nextHearingDate"))
                    .value(CamundaValue.stringValue(expectedNextHearingDate + "T18:00"))
                    .canReconfigure(CamundaValue.booleanValue(Boolean.parseBoolean(isReConfigurationRequest)))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("dueDate"))
                    .value(CamundaValue.stringValue(EXPECTED_DEFAULT_DUE_DATE + "T16:00"))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("priorityDate"))
                    .value(CamundaValue.stringValue(EXPECTED_DEFAULT_DUE_DATE + "T16:00"))
                    .build()
            ));
    }

    @Test
    public void shouldNotCalculateNextHearingDateWhenOnlyDefaultNextHearingDateTimeIsAvailable() {

        ConfigurationDmnEvaluationResponse defaultNextHearingDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateTime"))
            .value(CamundaValue.stringValue("16:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDates(List.of(defaultNextHearingDateTime), false,
                            Boolean.parseBoolean(isReConfigurationRequest),
                            taskAttributes
            );

        assertThat(configurationDmnEvaluationResponses).hasSize(2)
            .isEqualTo(List.of(
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("dueDate"))
                    .value(CamundaValue.stringValue(EXPECTED_DEFAULT_DUE_DATE + "T16:00"))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("priorityDate"))
                    .value(CamundaValue.stringValue(EXPECTED_DEFAULT_DUE_DATE + "T16:00"))
                    .build()
            ));
    }

    @Test
    public void shouldCalculateNextHearingDateWhenOnlyDefaultNextHearingDateWithoutTimeIsAvailable() {

        String expectedNextHearingDate = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse defaultNextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(expectedNextHearingDate))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDates(List.of(defaultNextHearingDate), false,
                            Boolean.parseBoolean(isReConfigurationRequest),
                            taskAttributes
            );

        assertThat(configurationDmnEvaluationResponses).hasSize(3)
            .isEqualTo(List.of(
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("nextHearingDate"))
                    .value(CamundaValue.stringValue(expectedNextHearingDate + "T16:00"))
                    .canReconfigure(CamundaValue.booleanValue(false))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("dueDate"))
                    .value(CamundaValue.stringValue(EXPECTED_DEFAULT_DUE_DATE + "T16:00"))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("priorityDate"))
                    .value(CamundaValue.stringValue(EXPECTED_DEFAULT_DUE_DATE + "T16:00"))
                    .build()
            ));
    }

    @ParameterizedTest
    @CsvSource(value = {
        "false,true,Next,6,6,T18:00",
        "false,true,Next,8,8,T18:00",
        "false,true,No,6,6,T18:00",
        "false,false,Next,6,6,T18:00",
        "false,false,Next,2,2,T18:00",
        "false,false,No,6,6,T18:00",
        "false,false,Previous,2,2,T18:00"
    })
    public void shouldCalculateDateWhenAllNextHearingDateOriginPropertiesAreProvidedAndNonWorkingDayNotConsidered(
        String canConfigure,
        String nextHearingDateSkipNonWorkingDaysFlag,
        String nextHearingDateMustBeWorkingDayFlag,
        String intervalDays,
        String expectedDays,
        String expectedTime) {
        String localDateTime = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        boolean canConfigurable = Boolean.parseBoolean(canConfigure);

        ConfigurationDmnEvaluationResponse nextHearingDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateOrigin"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(canConfigurable))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateIntervalDays"))
            .value(CamundaValue.stringValue(intervalDays))
            .canReconfigure(CamundaValue.booleanValue(canConfigurable))
            .build();
        ConfigurationDmnEvaluationResponse nextHearingDateNonWorkingCalendar
            = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateNonWorkingCalendar"))
            .value(CamundaValue.stringValue("https://www.gov.uk/bank-holidays/england-and-wales.json"))
            .canReconfigure(CamundaValue.booleanValue(canConfigurable))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDateNonWorkingDaysOfWeek
            = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateNonWorkingDaysOfWeek"))
            .value(CamundaValue.stringValue(""))
            .canReconfigure(CamundaValue.booleanValue(canConfigurable))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDateSkipNonWorkingDays
            = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateSkipNonWorkingDays"))
            .value(CamundaValue.stringValue(nextHearingDateSkipNonWorkingDaysFlag))
            .value(CamundaValue.stringValue("SATURDAY,SUNDAY"))
            .canReconfigure(CamundaValue.booleanValue(canConfigurable))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDateMustBeWorkingDay
            = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateMustBeWorkingDay"))
            .value(CamundaValue.stringValue(nextHearingDateMustBeWorkingDayFlag))
            .canReconfigure(CamundaValue.booleanValue(canConfigurable))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateTime"))
            .value(CamundaValue.stringValue("18:00"))
            .canReconfigure(CamundaValue.booleanValue(canConfigurable))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDates(
                List.of(
                    nextHearingDateIntervalDays,
                    nextHearingDateNonWorkingCalendar,
                    nextHearingDateMustBeWorkingDay,
                    nextHearingDateNonWorkingDaysOfWeek,
                    nextHearingDateSkipNonWorkingDays,
                    nextHearingDateOrigin,
                    nextHearingDateTime
                ),
                false,
                Boolean.parseBoolean(isReConfigurationRequest),
                taskAttributes
            );

        String expectedNextHearingDate = GIVEN_DATE.plusDays(Integer.parseInt(expectedDays))
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        assertThat(configurationDmnEvaluationResponses).hasSize(3)
            .isEqualTo(List.of(
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("nextHearingDate"))
                    .value(CamundaValue.stringValue(expectedNextHearingDate + expectedTime))
                    .canReconfigure(CamundaValue.booleanValue(canConfigurable))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("dueDate"))
                    .value(CamundaValue.stringValue(EXPECTED_DEFAULT_DUE_DATE + "T16:00"))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("priorityDate"))
                    .value(CamundaValue.stringValue(EXPECTED_DEFAULT_DUE_DATE + "T16:00"))
                    .build()
            ));
    }

    @ParameterizedTest
    @CsvSource(value = {
        "false,true,Next,6,8,T18:00",
        "false,true,next,8,12,T18:00",
        "false,true,no,6,8,T18:00",
        "false,false,Next,6,6,T18:00",
        "false,false,Next,2,4,T18:00",
        "false,false,no,6,6,T18:00",
        "false,false,Previous,2,1,T18:00"
    })
    public void shouldCalculateDateWhenAllNextHearingDateOriginPropertiesAreProvided(
        String canConfigure,
        String nextHearingDateSkipNonWorkingDaysFlag,
        String nextHearingDateMustBeWorkingDayFlag,
        String intervalDays,
        String expectedDays,
        String expectedTime) {
        String localDateTime = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        boolean canConfigurable = Boolean.parseBoolean(canConfigure);

        ConfigurationDmnEvaluationResponse nextHearingDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateOrigin"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(canConfigurable))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateIntervalDays"))
            .value(CamundaValue.stringValue(intervalDays))
            .canReconfigure(CamundaValue.booleanValue(canConfigurable))
            .build();
        ConfigurationDmnEvaluationResponse nextHearingDateNonWorkingCalendar
            = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateNonWorkingCalendar"))
            .value(CamundaValue.stringValue("https://www.gov.uk/bank-holidays/england-and-wales.json"))
            .canReconfigure(CamundaValue.booleanValue(canConfigurable))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDateNonWorkingDaysOfWeek
            = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateNonWorkingDaysOfWeek"))
            .value(CamundaValue.stringValue("SATURDAY,SUNDAY"))
            .canReconfigure(CamundaValue.booleanValue(canConfigurable))
            .build();
        ConfigurationDmnEvaluationResponse nextHearingDateSkipNonWorkingDays
            = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateSkipNonWorkingDays"))
            .value(CamundaValue.stringValue(nextHearingDateSkipNonWorkingDaysFlag))
            .canReconfigure(CamundaValue.booleanValue(canConfigurable))
            .build();
        ConfigurationDmnEvaluationResponse nextHearingDateMustBeWorkingDay
            = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateMustBeWorkingDay"))
            .value(CamundaValue.stringValue(nextHearingDateMustBeWorkingDayFlag))
            .canReconfigure(CamundaValue.booleanValue(canConfigurable))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateTime"))
            .value(CamundaValue.stringValue("18:00"))
            .canReconfigure(CamundaValue.booleanValue(canConfigurable))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDates(
                List.of(
                    nextHearingDateIntervalDays,
                    nextHearingDateNonWorkingCalendar,
                    nextHearingDateMustBeWorkingDay,
                    nextHearingDateNonWorkingDaysOfWeek,
                    nextHearingDateSkipNonWorkingDays,
                    nextHearingDateOrigin,
                    nextHearingDateTime
                ),
                false,
                Boolean.parseBoolean(isReConfigurationRequest),
                taskAttributes
            );

        String expectedNextHearingDate = GIVEN_DATE.plusDays(Integer.parseInt(expectedDays))
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        assertThat(configurationDmnEvaluationResponses).hasSize(3)
            .isEqualTo(List.of(
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("nextHearingDate"))
                    .value(CamundaValue.stringValue(expectedNextHearingDate + expectedTime))
                    .canReconfigure(CamundaValue.booleanValue(canConfigurable))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("dueDate"))
                    .value(CamundaValue.stringValue(EXPECTED_DEFAULT_DUE_DATE + "T16:00"))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("priorityDate"))
                    .value(CamundaValue.stringValue(EXPECTED_DEFAULT_DUE_DATE + "T16:00"))
                    .build()
            ));
    }

    @Test
    public void shouldCalculateDateWhenAllNextHearingDateOriginPropertiesAreProvidedWithoutNextHearingDateTime() {
        String localDateTime = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse nextHearingDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateOrigin"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateIntervalDays"))
            .value(CamundaValue.stringValue("6"))
            .build();
        ConfigurationDmnEvaluationResponse nextHearingDateNonWorkingCalendar
            = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateNonWorkingCalendar"))
            .value(CamundaValue.stringValue("https://www.gov.uk/bank-holidays/england-and-wales.json"))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDateNonWorkingDaysOfWeek
            = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateNonWorkingDaysOfWeek"))
            .value(CamundaValue.stringValue("SATURDAY,SUNDAY"))
            .build();
        ConfigurationDmnEvaluationResponse nextHearingDateSkipNonWorkingDays
            = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateSkipNonWorkingDays"))
            .value(CamundaValue.stringValue("true"))
            .build();
        ConfigurationDmnEvaluationResponse nextHearingDateMustBeWorkingDay
            = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateMustBeWorkingDay"))
            .value(CamundaValue.stringValue("true"))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDates(
                List.of(nextHearingDateIntervalDays, nextHearingDateNonWorkingCalendar, nextHearingDateMustBeWorkingDay,
                        nextHearingDateNonWorkingDaysOfWeek, nextHearingDateSkipNonWorkingDays, nextHearingDateOrigin
                ),
                false,
                false,
                taskAttributes
            );

        String expectedNextHearingDate = GIVEN_DATE.plusDays(Integer.parseInt("8"))
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        assertThat(configurationDmnEvaluationResponses).hasSize(3)
            .isEqualTo(List.of(
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("nextHearingDate"))
                    .value(CamundaValue.stringValue(expectedNextHearingDate + "T20:00"))
                    .canReconfigure(CamundaValue.booleanValue(false))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("dueDate"))
                    .value(CamundaValue.stringValue(EXPECTED_DEFAULT_DUE_DATE + "T16:00"))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("priorityDate"))
                    .value(CamundaValue.stringValue(EXPECTED_DEFAULT_DUE_DATE + "T16:00"))
                    .build()
            ));
    }

    @Test
    public void shouldNoCalculateNextHearingDateWhenMultipleNextHearingDateTimesAreAvailable() {

        ConfigurationDmnEvaluationResponse defaultNextHearingDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateTime"))
            .value(CamundaValue.stringValue("16:00"))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateTime"))
            .value(CamundaValue.stringValue("18:00"))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDates(List.of(defaultNextHearingDateTime, nextHearingDateTime), false, true, taskAttributes);

        assertThat(configurationDmnEvaluationResponses).hasSize(0);
    }

    @Test
    public void shouldNotCalculateNextHearingDateWhenDefaultNextHearingDateWithoutTimeAndTimeAreAvailable() {

        String expectedNextHearingDate = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse defaultNextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(expectedNextHearingDate))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        ConfigurationDmnEvaluationResponse defaultNextHearingDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateTime"))
            .value(CamundaValue.stringValue("16:00"))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDates(List.of(defaultNextHearingDate, defaultNextHearingDateTime), false, true, taskAttributes);

        assertThat(configurationDmnEvaluationResponses).hasSize(0);
    }

    @Test
    public void shouldNotCalculateNextHearingDateWhenOnlyDefaultUnConfigurableNextHearingDateTimeIsAvailable() {

        ConfigurationDmnEvaluationResponse defaultNextHearingDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateTime"))
            .value(CamundaValue.stringValue("16:00"))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDates(List.of(defaultNextHearingDateTime), false, true, taskAttributes);

        assertThat(configurationDmnEvaluationResponses).hasSize(0);
    }

    @Test
    public void shouldNotReturnNextHearingDateWhenOnlyDefaultUnconfigurableNextHearingDateWithoutTimeIsAvailable() {

        String expectedNextHearingDate = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse defaultNextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(expectedNextHearingDate))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDates(
                List.of(defaultNextHearingDate),
                false,
                true,
                taskAttributes
            );

        assertThat(configurationDmnEvaluationResponses).hasSize(0);
    }

    @Test
    public void shouldNotReturnDNextHearingDateWhenPropertiesAreNotAvailableAndJurisdictionIsWAAndIsReconfiguration() {
        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDates(List.of(), false, true, taskAttributes);

        assertThat(configurationDmnEvaluationResponses).hasSize(0);
    }

    @Test
    public void shouldCalculateDateWhenOriginDateIsBSTNextHearingDateNonBSTBackword() {
        String localDateTime = BST_DATE_BACKWARD.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        //Clocks go back an hour at 2:00am
        ConfigurationDmnEvaluationResponse nextHearingDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateOrigin"))
            .value(CamundaValue.stringValue(localDateTime + "T01:30"))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateIntervalDays"))
            .value(CamundaValue.stringValue("4"))
            .build();
        ConfigurationDmnEvaluationResponse nextHearingDateNonWorkingCalendar
            = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateNonWorkingCalendar"))
            .value(CamundaValue.stringValue("https://www.gov.uk/bank-holidays/england-and-wales.json"))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDateNonWorkingDaysOfWeek
            = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateNonWorkingDaysOfWeek"))
            .value(CamundaValue.stringValue(""))
            .build();
        ConfigurationDmnEvaluationResponse nextHearingDateSkipNonWorkingDays
            = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateSkipNonWorkingDays"))
            .value(CamundaValue.stringValue("false"))
            .build();
        ConfigurationDmnEvaluationResponse nextHearingDateMustBeWorkingDay
            = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateMustBeWorkingDay"))
            .value(CamundaValue.stringValue("false"))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateTime"))
            .value(CamundaValue.stringValue("02:30"))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDates(
                List.of(
                    nextHearingDateIntervalDays,
                    nextHearingDateNonWorkingCalendar,
                    nextHearingDateMustBeWorkingDay,
                    nextHearingDateNonWorkingDaysOfWeek,
                    nextHearingDateSkipNonWorkingDays,
                    nextHearingDateOrigin,
                    nextHearingDateTime
                ),
                false, false,
                taskAttributes
            );

        String expectedNextHearingDate = "2022-10-30T02:30";

        assertThat(configurationDmnEvaluationResponses).hasSize(3)
            .isEqualTo(List.of(
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("nextHearingDate"))
                    .value(CamundaValue.stringValue(expectedNextHearingDate))
                    .canReconfigure(CamundaValue.booleanValue(false))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("dueDate"))
                    .value(CamundaValue.stringValue(EXPECTED_DEFAULT_DUE_DATE + "T16:00"))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("priorityDate"))
                    .value(CamundaValue.stringValue(EXPECTED_DEFAULT_DUE_DATE + "T16:00"))
                    .build()
            ));
    }

    @Test
    public void shouldCalculateDateWhenOriginDateIsBSTNextHearingDateNonBSTForward() {
        String localDateTime = BST_DATE_FORWARD.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        //Clocks go forward an hour at 1:00am
        ConfigurationDmnEvaluationResponse nextHearingDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateOrigin"))
            .value(CamundaValue.stringValue(localDateTime + "T00:30"))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateIntervalDays"))
            .value(CamundaValue.stringValue("4"))
            .build();
        ConfigurationDmnEvaluationResponse nextHearingDateNonWorkingCalendar
            = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateNonWorkingCalendar"))
            .value(CamundaValue.stringValue("https://www.gov.uk/bank-holidays/england-and-wales.json"))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDateNonWorkingDaysOfWeek
            = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateNonWorkingDaysOfWeek"))
            .value(CamundaValue.stringValue(""))
            .build();
        ConfigurationDmnEvaluationResponse nextHearingDateSkipNonWorkingDays
            = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateSkipNonWorkingDays"))
            .value(CamundaValue.stringValue("false"))
            .build();
        ConfigurationDmnEvaluationResponse nextHearingDateMustBeWorkingDay
            = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateMustBeWorkingDay"))
            .value(CamundaValue.stringValue("false"))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateTime"))
            .value(CamundaValue.stringValue("01:30"))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDates(
                List.of(
                    nextHearingDateIntervalDays,
                    nextHearingDateNonWorkingCalendar,
                    nextHearingDateMustBeWorkingDay,
                    nextHearingDateNonWorkingDaysOfWeek,
                    nextHearingDateSkipNonWorkingDays,
                    nextHearingDateOrigin,
                    nextHearingDateTime
                ),
                false, false,
                taskAttributes
            );

        String expectedNextHearingDate = "2023-03-30T01:30";

        assertThat(configurationDmnEvaluationResponses).hasSize(3)
            .isEqualTo(List.of(
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("nextHearingDate"))
                    .value(CamundaValue.stringValue(expectedNextHearingDate))
                    .canReconfigure(CamundaValue.booleanValue(false))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("dueDate"))
                    .value(CamundaValue.stringValue(EXPECTED_DEFAULT_DUE_DATE + "T16:00"))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("priorityDate"))
                    .value(CamundaValue.stringValue(EXPECTED_DEFAULT_DUE_DATE + "T16:00"))
                    .build()
            ));
    }

    @ParameterizedTest
    @CsvSource({"true", "false"})
    public void shouldNotProvideDefaultForNextHearingDateWhenNoneOfAttributesArePresent(boolean configurable) {
        ConfigurationDmnEvaluationResponse calculatedDates = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("calculatedDates"))
            .value(CamundaValue.stringValue("nextHearingDate,dueDate,priorityDate"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();
        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDates(List.of(calculatedDates), false, configurable, taskAttributes);

        assertThat(configurationDmnEvaluationResponses)
            .filteredOn(r -> r.getName().getValue().equals("nextHearingDate"))
            .isEmpty();
    }

    @Test
    public void shouldNotDefaultWhenOriginRefAttributesPresentButAreEmpty() {
        ConfigurationDmnEvaluationResponse nextHearingDateOriginRef = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateOriginRef"))
            .value(CamundaValue.stringValue("nextHearingDuration"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();
        ConfigurationDmnEvaluationResponse calculatedDates = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("calculatedDates"))
            .value(CamundaValue.stringValue("nextHearingDuration,nextHearingDate,dueDate,priorityDate"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses
            = dateTypeConfigurator.configureDates(
            List.of(calculatedDates, nextHearingDateOriginRef),
            false,
            true,
            taskAttributes
        );
        assertThat(configurationDmnEvaluationResponses).isEmpty();
    }

    @Test
    public void shouldNotRecalculateDateWhenDueDateIsUnconfigurableButOriginIsConfigurable() {
        String nextHearingDateValue = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String nextHearingDateOriginValue = GIVEN_DATE.plusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(nextHearingDateValue + "T18:00"))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateOrigin"))
            .value(CamundaValue.stringValue(nextHearingDateOriginValue + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDates(List.of(nextHearingDate, nextHearingDateOrigin), false, true, taskAttributes);

        assertThat(configurationDmnEvaluationResponses).isEmpty();
    }

    @Test
    public void shouldNotRecalculateDateWhenDueDateIsUnconfigurableButOriginLatestIsConfigurable() {
        String nextHearingDateValue = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(nextHearingDateValue + "T18:00"))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateOriginEarliest"))
            .value(CamundaValue.stringValue("nextHearingDate,priorityDate"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDates(List.of(nextHearingDate, nextHearingDateOrigin), false, true, taskAttributes);

        assertThat(configurationDmnEvaluationResponses).isEmpty();
    }

    @Test
    public void shouldNotRecalculateDateWhenDueDateIsUnconfigurableButOriginDurationIsConfigurable() {
        String nextHearingDateValue = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String nextHearingDateDurationValue = GIVEN_DATE.plusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(nextHearingDateValue + "T18:00"))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateDuration"))
            .value(CamundaValue.stringValue(nextHearingDateDurationValue + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDates(List.of(nextHearingDate, nextHearingDateOrigin), false, true, taskAttributes);

        assertThat(configurationDmnEvaluationResponses).isEmpty();
    }

    @Test
    public void shouldReturnEmptyNextHearingDate() {
        //if nextHearingDate is empty, then it should return empty nextHearingDate
        String nextHearingDateValue = "";
        String nextHearingDateDurationValue = "";

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(nextHearingDateValue))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateDuration"))
            .value(CamundaValue.stringValue(nextHearingDateDurationValue + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDates(List.of(nextHearingDate, nextHearingDateOrigin), false,
                            true, taskAttributes
            );

        assertThat(configurationDmnEvaluationResponses)
            .isEqualTo(List.of(
                nextHearingDate
            ));
    }
}
