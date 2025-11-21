package uk.gov.hmcts.reform.wataskmanagementapi.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.IdamTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.TerminationProcess;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.utils.CancellationProcessValidator;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.HistoryVariableInstance;

import java.util.Optional;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TerminationProcessHelperTest {
    @Mock
    CamundaService camundaService;

    @Mock
    IdamTokenGenerator idamTokenGenerator;

    @Mock
    CancellationProcessValidator cancellationProcessValidator;
    TerminationProcessHelper terminationProcessHelper;

    @BeforeEach
    public void setUp() {
        terminationProcessHelper = new TerminationProcessHelper(
            camundaService,
            idamTokenGenerator,
            cancellationProcessValidator
        );
    }

    @Test
    void should_return_termination_process_when_feature_flag_is_enabled_and_variable_is_present() {
        String taskId = "taskId123";
        String cancellationProcess = "CASE_EVENT_CANCELLATION";
        when(cancellationProcessValidator.isCancellationProcessFeatureEnabled(any()))
            .thenReturn(true);
        when(cancellationProcessValidator.validate(anyString(), anyString(), any()))
            .thenReturn(Optional.of(cancellationProcess));
        when(camundaService.getVariableFromHistory(taskId, "cancellationProcess"))
            .thenReturn(Optional.of(new HistoryVariableInstance("id", "cancellationProcess", cancellationProcess)));

        Optional<TerminationProcess> result = terminationProcessHelper.fetchTerminationProcessFromCamunda(taskId);

        assertTrue(result.isPresent());
        Optional<TerminationProcess> expectedTerminationProcess = TerminationProcess.fromValue(cancellationProcess);
        assertEquals(expectedTerminationProcess.get(), result.get());
    }

    @Test
    void should_return_optional_empty_when_feature_flag_is_enabled_and_invalid_cancellation_process_is_present() {
        String taskId = "taskId123";
        String cancellationProcess = "INVALID_CANCELLATION";
        when(cancellationProcessValidator.isCancellationProcessFeatureEnabled(any()))
            .thenReturn(true);
        when(cancellationProcessValidator.validate(anyString(), anyString(), any()))
            .thenReturn(Optional.empty());
        when(camundaService.getVariableFromHistory(taskId, "cancellationProcess"))
            .thenReturn(Optional.of(new HistoryVariableInstance("id", "cancellationProcess", cancellationProcess)));

        Optional<TerminationProcess> result = terminationProcessHelper.fetchTerminationProcessFromCamunda(taskId);

        assertTrue(result.isEmpty());

    }

    @Test
    void should_return_empty_optional_when_feature_flag_is_disabled() {
        String taskId = "taskId123";
        when(cancellationProcessValidator.isCancellationProcessFeatureEnabled(any()))
            .thenReturn(false);

        Optional<TerminationProcess> result = terminationProcessHelper.fetchTerminationProcessFromCamunda(taskId);

        assertTrue(result.isEmpty());
    }

    @Test
    void should_return_empty_optional_when_cancellation_process_variable_is_not_present() {
        String taskId = "taskId123";

        when(cancellationProcessValidator.isCancellationProcessFeatureEnabled(any()))
            .thenReturn(true);
        when(camundaService.getVariableFromHistory(taskId, "cancellationProcess")).thenReturn(Optional.empty());

        Optional<TerminationProcess> result = terminationProcessHelper.fetchTerminationProcessFromCamunda(taskId);

        assertTrue(result.isEmpty());
    }



}
