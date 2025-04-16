package uk.gov.hmcts.reform.wataskmanagementapi.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static com.microsoft.applicationinsights.core.dependencies.http.HttpStatus.SC_SERVICE_UNAVAILABLE;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.features.FeatureFlag.WA_DELETE_TASK_BY_CASE_ID;

@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DeleteTaskFeatureToggleInterceptorTest {

    @Mock
    private LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider;
    @InjectMocks
    private DeleteTaskFeatureToggleInterceptor deleteTaskFeatureToggleInterceptor;

    @Test
    void shouldReturnTrueWhenDeleteTaskEndpointIsEnabled() throws IOException {
        final HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        final HttpServletResponse httpServletResponse = new MockHttpServletResponse();
        final Object object = mock(Object.class);

        when(launchDarklyFeatureFlagProvider.getBooleanValue(WA_DELETE_TASK_BY_CASE_ID,
                "ccd-case-disposer",
                "ccd-case-disposer@hmcts.net")).thenReturn(true);

        final boolean isValidRequest = deleteTaskFeatureToggleInterceptor
                .preHandle(httpServletRequest, httpServletResponse, object);

        assertThat(isValidRequest).isTrue();
    }

    @Test
    void shouldReturnFalseWhenDeleteTaskEndpointIsDisabled() throws IOException {
        final HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        final HttpServletResponse httpServletResponse = new MockHttpServletResponse();
        final Object object = mock(Object.class);

        when(launchDarklyFeatureFlagProvider.getBooleanValue(WA_DELETE_TASK_BY_CASE_ID,
                "ccd-case-disposer",
                "ccd-case-disposer@hmcts.net")).thenReturn(false);

        final boolean isValidRequest = deleteTaskFeatureToggleInterceptor
                .preHandle(httpServletRequest, httpServletResponse, object);

        assertThat(httpServletResponse.getStatus()).isEqualTo(SC_SERVICE_UNAVAILABLE);
        assertThat(isValidRequest).isFalse();
    }
}
