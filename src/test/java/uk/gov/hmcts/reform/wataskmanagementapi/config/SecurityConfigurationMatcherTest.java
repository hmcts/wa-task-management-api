package uk.gov.hmcts.reform.wataskmanagementapi.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.authorisation.filters.ServiceAuthFilter;
import uk.gov.hmcts.reform.wataskmanagementapi.services.SystemDateProvider;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SecurityConfigurationMatcherTest.TaskInitiationController.class, properties = {
    "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/jwks"
})
@Import({SecurityConfiguration.class, IdamSecurityProperties.class, SecurityConfigurationMatcherTest.TestConfig.class})
class SecurityConfigurationMatcherTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void taskOperationPostRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/task/operation"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void taskDeletePostRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/task/delete"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void searchForCompletablePostRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/task/search-for-completable"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void taskInitiationPostIsPermittedWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/task/123e4567-e89b-12d3-a456-426614174000/initiation"))
            .andExpect(status().isNoContent());
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        ServiceAuthFilter serviceAuthFilter() {
            ServiceAuthFilter serviceAuthFilter = mock(ServiceAuthFilter.class);
            doAnswer(invocation -> {
                ServletRequest request = invocation.getArgument(0);
                ServletResponse response = invocation.getArgument(1);
                FilterChain filterChain = invocation.getArgument(2);
                filterChain.doFilter(request, response);
                return null;
            }).when(serviceAuthFilter).doFilter(
                any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));
            return serviceAuthFilter;
        }

        @Bean
        SystemDateProvider systemDateProvider() {
            return mock(SystemDateProvider.class);
        }

        @Bean
        LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider() {
            return mock(LaunchDarklyFeatureFlagProvider.class);
        }
    }

    @RestController
    static class TaskInitiationController {

        @PostMapping("/task/{taskId}/initiation")
        @ResponseStatus(HttpStatus.NO_CONTENT)
        void initiate(@PathVariable("taskId") String taskId) {
            // An empty handler lets the test distinguish a permitted request from an unauthorized one.
        }
    }
}
