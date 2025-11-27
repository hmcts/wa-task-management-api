package uk.gov.hmcts.reform.wataskmanagementapi.services;

import com.github.benmanes.caffeine.cache.Ticker;
import com.google.common.testing.FakeTicker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.DmnRequest;

import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.SERVICE_AUTHORIZATION_TOKEN;

@SpringBootTest
@ActiveProfiles({"integration"})
@Transactional//@Testcontainers
//@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class DmnEvaluationServiceCacheTest {

    @MockitoBean
    private AuthTokenGenerator authTokenGenerator;

    @MockitoBean
    private CamundaServiceApi camundaServiceApi;

    @MockitoSpyBean
    private DmnEvaluationService dmnEvaluationService;

    /*
     * This class provides us change system time to test caching
     */
    @TestConfiguration
    public static class OverrideBean {

        static FakeTicker FAKE_TICKER = new FakeTicker();

        @Bean
        public Ticker ticker() {
            return FAKE_TICKER::read;
        }
    }

    @Nested
    @DisplayName("Retrieve task type")
    class Test1 {
        @BeforeEach
        void setup() {
            when(authTokenGenerator.generate()).thenReturn(SERVICE_AUTHORIZATION_TOKEN);
        }

        @Test
        void should_call_camunda_api_once_when_retrieving_task_type_dmn() {
            String dmnKey = "wa-task-types-";


            IntStream.range(0, 4).forEach(x -> dmnEvaluationService.retrieveTaskTypesDmn("wa", dmnKey));
            IntStream.range(0, 5).forEach(x -> dmnEvaluationService.retrieveTaskTypesDmn("ia", dmnKey));

            verify(camundaServiceApi, times(1))
                .getTaskTypesDmnTable(
                    SERVICE_AUTHORIZATION_TOKEN,
                    "wa",
                    dmnKey + "wa%",
                    true
                );

            verify(camundaServiceApi, times(1))
                .getTaskTypesDmnTable(
                    SERVICE_AUTHORIZATION_TOKEN,
                    "ia",
                     dmnKey + "ia%",
                    true
                );

        }
    }

    @Nested
    @DisplayName("Evaluate task type")
    class Test2 {
        @BeforeEach
        void setup() {
            when(authTokenGenerator.generate()).thenReturn(SERVICE_AUTHORIZATION_TOKEN);
        }

        @Test
        void should_call_camunda_api_once_when_evaluating_task_type_dmn() {

            String dmnKey1 = "wa-task-types-wa-wacasetype1";
            String dmnKey2 = "wa-task-types-wa-wacasetype2";

            IntStream.range(0, 4).forEach(x -> dmnEvaluationService.evaluateTaskTypesDmn("wa", dmnKey1));
            IntStream.range(0, 5).forEach(x -> dmnEvaluationService.evaluateTaskTypesDmn("wa", dmnKey2));

            verify(camundaServiceApi, times(1))
                .evaluateTaskTypesDmnTable(
                    SERVICE_AUTHORIZATION_TOKEN,
                    dmnKey1,
                    "wa",
                    new DmnRequest<>()
                );

            verify(camundaServiceApi, times(1))
                .evaluateTaskTypesDmnTable(
                    SERVICE_AUTHORIZATION_TOKEN,
                    dmnKey2,
                    "wa",
                    new DmnRequest<>()
                );

        }
    }

    @Nested
    @DisplayName("Cache expire")
    class Test3 {
        @BeforeEach
        void setup() {
            when(authTokenGenerator.generate()).thenReturn(SERVICE_AUTHORIZATION_TOKEN);
        }

        @Test
        void should_use_cache_until_expires_then_call_service() {

            // given
            String dmnKey = "wa-task-types-wa-wacasetype";

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
            OverrideBean.FAKE_TICKER.advance(40, TimeUnit.MINUTES);
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
            OverrideBean.FAKE_TICKER.advance(61, TimeUnit.MINUTES);
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
    }
}
