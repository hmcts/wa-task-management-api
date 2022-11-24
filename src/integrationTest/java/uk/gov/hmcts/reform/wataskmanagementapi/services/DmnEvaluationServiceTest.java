package uk.gov.hmcts.reform.wataskmanagementapi.services;

import com.github.benmanes.caffeine.cache.Ticker;
import com.google.common.testing.FakeTicker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.Application;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.DmnRequest;

import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.SERVICE_AUTHORIZATION_TOKEN;

@SpringBootTest
@ContextConfiguration
@ActiveProfiles({"integration"})
public class DmnEvaluationServiceTest {

    @MockBean
    private AuthTokenGenerator serviceAuthTokenGenerator;

    @MockBean
    private CamundaServiceApi camundaServiceApi;

    @Autowired
    private DmnEvaluationService dmnEvaluationService;

    @Test
    void should_call_camunda_api_once_when_retrieving_task_type_dmn() {
        String dmnName = "Task Types DMN";

        when(serviceAuthTokenGenerator.generate())
            .thenReturn(SERVICE_AUTHORIZATION_TOKEN);

        dmnEvaluationService.retrieveTaskTypesDmn("wa", dmnName);
        dmnEvaluationService.retrieveTaskTypesDmn("wa", dmnName);
        dmnEvaluationService.retrieveTaskTypesDmn("wa", dmnName);
        dmnEvaluationService.retrieveTaskTypesDmn("wa", dmnName);
        dmnEvaluationService.retrieveTaskTypesDmn("ia", dmnName);
        dmnEvaluationService.retrieveTaskTypesDmn("ia", dmnName);
        dmnEvaluationService.retrieveTaskTypesDmn("ia", dmnName);
        dmnEvaluationService.retrieveTaskTypesDmn("ia", dmnName);
        dmnEvaluationService.retrieveTaskTypesDmn("ia", dmnName);

        verify(camundaServiceApi, times(1))
            .getTaskTypesDmnTable(
                SERVICE_AUTHORIZATION_TOKEN,
                "wa",
                dmnName
            );

        verify(camundaServiceApi, times(1))
            .getTaskTypesDmnTable(
                SERVICE_AUTHORIZATION_TOKEN,
                "ia",
                dmnName
            );

    }

    @Test
    void should_call_camunda_api_once_when_evaluating_task_type_dmn() {

        String dmnKey = "wa-task-types-wa-wacasetype";

        when(serviceAuthTokenGenerator.generate())
            .thenReturn(SERVICE_AUTHORIZATION_TOKEN);

        dmnEvaluationService.evaluateTaskTypesDmn("wa", dmnKey);
        dmnEvaluationService.evaluateTaskTypesDmn("wa", dmnKey);
        dmnEvaluationService.evaluateTaskTypesDmn("wa", dmnKey);
        dmnEvaluationService.evaluateTaskTypesDmn("wa", dmnKey);
        dmnEvaluationService.evaluateTaskTypesDmn("ia", dmnKey);
        dmnEvaluationService.evaluateTaskTypesDmn("ia", dmnKey);
        dmnEvaluationService.evaluateTaskTypesDmn("ia", dmnKey);
        dmnEvaluationService.evaluateTaskTypesDmn("ia", dmnKey);
        dmnEvaluationService.evaluateTaskTypesDmn("ia", dmnKey);

        verify(camundaServiceApi, times(1))
            .evaluateTaskTypesDmnTable(
                SERVICE_AUTHORIZATION_TOKEN,
                dmnKey,
                "wa",
                new DmnRequest<>()
            );

        verify(camundaServiceApi, times(1))
            .evaluateTaskTypesDmnTable(
                SERVICE_AUTHORIZATION_TOKEN,
                dmnKey,
                "ia",
                new DmnRequest<>()
            );

    }


    @Test
    void should_use_cache_until_expires_then_call_service() {

        // given
        String dmnKey = "wa-task-types-wa-wacasetype";

        when(serviceAuthTokenGenerator.generate())
            .thenReturn(SERVICE_AUTHORIZATION_TOKEN);

        // when first attempt, call the service
        dmnEvaluationService.evaluateTaskTypesDmn("sscs", dmnKey);

        // then
        verify(camundaServiceApi, times(1))
            .evaluateTaskTypesDmnTable(
                SERVICE_AUTHORIZATION_TOKEN,
                dmnKey,
                "sscs",
                new DmnRequest<>()
            );


        // when 10 mins later use cache response
        TestConfiguration.fakeTicker.advance(10, TimeUnit.MINUTES);
        dmnEvaluationService.evaluateTaskTypesDmn("sscs", dmnKey);

        // then
        verify(camundaServiceApi, times(1))
            .evaluateTaskTypesDmnTable(
                SERVICE_AUTHORIZATION_TOKEN,
                dmnKey,
                "sscs",
                new DmnRequest<>()
            );


        // when 61 mins later cache expired should call the service 1 more
        TestConfiguration.fakeTicker.advance(61, TimeUnit.MINUTES);
        dmnEvaluationService.evaluateTaskTypesDmn("sscs", dmnKey);

        // then
        verify(camundaServiceApi, times(2))
            .evaluateTaskTypesDmnTable(
                SERVICE_AUTHORIZATION_TOKEN,
                dmnKey,
                "sscs",
                new DmnRequest<>()
            );
    }

    /*
     * This class provides us change system time to test caching
     */
    @Configuration
    @Import(Application.class)
    public static class TestConfiguration {

        static FakeTicker fakeTicker = new FakeTicker();

        @Bean
        public Ticker ticker() {
            return fakeTicker::read;
        }

    }
}
