package uk.gov.hmcts.reform.wataskmanagementapi.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DeleteTaskFeatureToggleConfigurationTest {

    @Test
    void shouldAddRestApiPreInvokeInterceptor() {
        final DeleteTaskFeatureToggleConfiguration restApiPreInvokeConfiguration =
                new DeleteTaskFeatureToggleConfiguration();
        final InterceptorRegistry interceptorRegistry = mock(InterceptorRegistry.class);

        when(interceptorRegistry.addInterceptor(any())).thenReturn(mock(InterceptorRegistration.class));

        restApiPreInvokeConfiguration.addInterceptors(interceptorRegistry);

        verify(interceptorRegistry, times(1))
                .addInterceptor(any(DeleteTaskFeatureToggleInterceptor.class));
    }
}