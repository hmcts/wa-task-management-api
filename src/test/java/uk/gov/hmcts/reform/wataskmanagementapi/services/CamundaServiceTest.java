package uk.gov.hmcts.reform.wataskmanagementapi.services;

import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.exceptions.TestFeignClientException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.ResourceNotFoundException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CamundaServiceTest {

    @Mock
    private CamundaServiceApi camundaServiceApi;

    @Mock
    private CamundaErrorDecoder camundaErrorDecoder;

    private CamundaService camundaService;

    @BeforeEach
    public void setUp() {
        camundaService = new CamundaService(camundaServiceApi, camundaErrorDecoder);
    }

    @Test
    void getTask_should_succeed() {

        String taskId = UUID.randomUUID().toString();

        CamundaTask mockedTask = mock(CamundaTask.class);
        when(camundaServiceApi.getTask(taskId)).thenReturn(mockedTask);

        CamundaTask response = camundaService.getTask(taskId);

        verify(camundaServiceApi, times(1)).getTask(taskId);
        verifyNoMoreInteractions(camundaServiceApi);

        assertEquals(mockedTask, response);
    }

    @Test
    void getTask_should_throw_a_resource_not_found_exception_when_feign_exception_is_thrown() {

        String taskId = UUID.randomUUID().toString();

        when(camundaServiceApi.getTask(taskId)).thenThrow(FeignException.class);

        assertThatThrownBy(() -> camundaService.getTask(taskId))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasCauseInstanceOf(FeignException.class);

    }

    @Test
    void claimTask_should_succeed() {

        String taskId = UUID.randomUUID().toString();
        String userId = UUID.randomUUID().toString();

        camundaService.claimTask(taskId, userId);
        verify(camundaServiceApi, times(1)).claimTask(eq(taskId), anyMap());
        verifyNoMoreInteractions(camundaServiceApi);
    }

    @Test
    void claimTask_should_throw_resource_not_found_exception_when_other_exception_is_thrown() {

        String taskId = UUID.randomUUID().toString();
        String userId = UUID.randomUUID().toString();

        TestFeignClientException exception =
            new TestFeignClientException(
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase()
            );

        doThrow(exception)
            .when(camundaServiceApi).claimTask(eq(taskId), anyMap());

        assertThatThrownBy(() -> camundaService.claimTask(taskId, userId))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasCauseInstanceOf(FeignException.class);

    }

}
