package uk.gov.hmcts.reform.wataskmanagementapi.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.microsoft.applicationinsights.core.dependencies.http.HttpStatus.SC_SERVICE_UNAVAILABLE;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.features.FeatureFlag.WA_DELETE_TASK_BY_CASE_ID;

public class DeleteTaskFeatureToggleInterceptor implements HandlerInterceptor {

    @Autowired
    private LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider;

    @Override
    public boolean preHandle(final HttpServletRequest request,
                             final HttpServletResponse response,
                             final Object handler) throws IOException {

        final boolean isLaunchDarklyFeatureFlagEnabled = launchDarklyFeatureFlagProvider.getBooleanValue(
                WA_DELETE_TASK_BY_CASE_ID,
                "ccd-case-disposer",
                "ccd-case-disposer@hmcts.net");

        if (!isLaunchDarklyFeatureFlagEnabled) {
            response.sendError(SC_SERVICE_UNAVAILABLE, "Task deletion endpoint is unavailable");
        }

        return isLaunchDarklyFeatureFlagEnabled;
    }
}
