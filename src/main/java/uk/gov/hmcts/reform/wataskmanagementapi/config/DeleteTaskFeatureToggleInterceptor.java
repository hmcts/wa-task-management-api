package uk.gov.hmcts.reform.wataskmanagementapi.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static uk.gov.hmcts.reform.wataskmanagementapi.config.features.FeatureFlag.WA_DELETE_TASK_BY_CASE_ID;

public class DeleteTaskFeatureToggleInterceptor implements HandlerInterceptor {

    @Autowired
    private LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider;

    @Override
    public boolean preHandle(final HttpServletRequest request,
                             final HttpServletResponse response,
                             final Object handler) {

        return launchDarklyFeatureFlagProvider.getBooleanValue(
                WA_DELETE_TASK_BY_CASE_ID,
                "ccd-case-disposer",
                "ccd-case-disposer@hmcts.net");
    }
}
