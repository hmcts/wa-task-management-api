package uk.gov.hmcts.reform.wataskmanagementapi.services;

import feign.FeignException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CompleteTaskVariables;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@Import(CamundaRetryServiceTest.TestRetryConfig.class)
class CamundaRetryServiceTest {

    @TestConfiguration
    @EnableRetry
    @Import(CamundaRetryService.class)
    static class TestRetryConfig {
        // Enables Spring Retry proxies + registers CamundaRetryService as a bean
    }

    @MockitoBean
    private CamundaServiceApi camundaServiceApi;

    @MockitoBean
    private AuthTokenGenerator authTokenGenerator;

    @Autowired
    private CamundaRetryService camundaRetryService;

    @Test
    void should_retry_complete_task_when_feign_exception_occurred() {
        String taskId = "task-123";

        when(authTokenGenerator.generate()).thenReturn("S2S_TOKEN_1", "S2S_TOKEN_2", "S2S_TOKEN_3");

        doThrow(feignException(500))
            .doThrow(feignException(500))
            .doNothing()
            .when(camundaServiceApi).completeTask(anyString(), eq(taskId), any(CompleteTaskVariables.class));

        camundaRetryService.completeTaskWithRetry(taskId);

        verify(camundaServiceApi, times(3)).completeTask(anyString(), eq(taskId), any(CompleteTaskVariables.class));
        verify(authTokenGenerator, times(3)).generate();
        verifyNoMoreInteractions(camundaServiceApi);
    }

    @Test
    void should_regenerate_token_when_retry() {
        String taskId = "task-456";

        when(authTokenGenerator.generate()).thenReturn("S2S_TOKEN_1", "S2S_TOKEN_2", "S2S_TOKEN_3");

        // Fail twice, then succeed
        doThrow(feignException(401))
            .doThrow(feignException(401))
            .doNothing()
            .when(camundaServiceApi).completeTask(anyString(), eq(taskId), any(CompleteTaskVariables.class));

        camundaRetryService.completeTaskWithRetry(taskId);

        // Verify each attempt used the fresh token from that attempt
        verify(camundaServiceApi).completeTask(eq("S2S_TOKEN_1"), eq(taskId), any(CompleteTaskVariables.class));
        verify(camundaServiceApi).completeTask(eq("S2S_TOKEN_2"), eq(taskId), any(CompleteTaskVariables.class));
        verify(camundaServiceApi).completeTask(eq("S2S_TOKEN_3"), eq(taskId), any(CompleteTaskVariables.class));

        // Bonus: ensures generation happened per attempt
        verify(authTokenGenerator, times(3)).generate();
    }

    private static FeignException feignException(int status) {
        Request request = Request.create(
            Request.HttpMethod.POST,
            "http://localhost/task/some-id/complete",
            Collections.emptyMap(),
            new byte[0],
            StandardCharsets.UTF_8,
            null
        );

        Response response = Response.builder()
            .status(status)
            .reason("error")
            .request(request)
            .headers(Collections.emptyMap())
            .build();

        return FeignException.errorStatus("CamundaServiceApi#completeTask", response);
    }
}
