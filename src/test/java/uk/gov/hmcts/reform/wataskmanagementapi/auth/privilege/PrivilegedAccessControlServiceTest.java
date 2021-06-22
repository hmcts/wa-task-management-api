package uk.gov.hmcts.reform.wataskmanagementapi.auth.privilege;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.authorisation.validators.ServiceAuthTokenValidator;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.config.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.config.features.FeatureFlag;

import java.util.Collections;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrivilegedAccessControlServiceTest {

    private static final String SERVICE_AUTH_TOKEN = "some serviceAuthorizationToken";
    private static final String PRIVILEGED_SERVICE_NAME = "somePrivilegedServiceName";
    private static final String USER_ID = "someUserId";
    @Mock
    private LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider;
    @Mock
    private ServiceAuthTokenValidator serviceAuthTokenValidator;
    @Mock
    private AccessControlResponse accessControlResponse;
    @Mock
    private UserInfo userInfo;

    private PrivilegedAccessControlService privilegedAccessControlService;

    @BeforeEach
    void setup() {
        when(accessControlResponse.getUserInfo())
            .thenReturn(userInfo);
        when(userInfo.getUid())
            .thenReturn(USER_ID);

        privilegedAccessControlService = new PrivilegedAccessControlService(
            serviceAuthTokenValidator,
            launchDarklyFeatureFlagProvider,
            Collections.singletonList(PRIVILEGED_SERVICE_NAME)
        );
    }

    @Test
    void hasPrivilegedAccess_should_return_true_if_feature_is_enabled_and_service_whitelisted() {

        when(launchDarklyFeatureFlagProvider.getBooleanValue(FeatureFlag.PRIVILEGED_ACCESS_FEATURE, USER_ID))
            .thenReturn(true);
        when(serviceAuthTokenValidator.getServiceName(SERVICE_AUTH_TOKEN))
            .thenReturn(PRIVILEGED_SERVICE_NAME);

        boolean result = privilegedAccessControlService.hasPrivilegedAccess(SERVICE_AUTH_TOKEN, accessControlResponse);

        assertTrue(result);
    }

    @Test
    void hasPrivilegedAccess_should_return_false_if_feature_is_disabled() {

        when(launchDarklyFeatureFlagProvider.getBooleanValue(FeatureFlag.PRIVILEGED_ACCESS_FEATURE, USER_ID))
            .thenReturn(false);

        boolean result = privilegedAccessControlService.hasPrivilegedAccess(SERVICE_AUTH_TOKEN, accessControlResponse);

        assertFalse(result);
    }

    @Test
    void hasPrivilegedAccess_should_return_false_if_feature_is_enabled_and_service_is_not_whitelisted() {

        when(launchDarklyFeatureFlagProvider.getBooleanValue(FeatureFlag.PRIVILEGED_ACCESS_FEATURE, USER_ID))
            .thenReturn(true);
        when(serviceAuthTokenValidator.getServiceName(SERVICE_AUTH_TOKEN))
            .thenReturn("anotherService");

        boolean result = privilegedAccessControlService.hasPrivilegedAccess(SERVICE_AUTH_TOKEN, accessControlResponse);

        assertFalse(result);
    }

    @Test
    void hasPrivilegedAccess_should_throw_null_pointer_exception_if_required_parameters_are_null() {

        assertThatThrownBy(() -> privilegedAccessControlService.hasPrivilegedAccess(
            null,
            accessControlResponse))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("ServiceAuthorization must not be null");

        when(userInfo.getUid())
            .thenReturn(null);

        assertThatThrownBy(() -> privilegedAccessControlService.hasPrivilegedAccess(
            SERVICE_AUTH_TOKEN,
            accessControlResponse))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("UserId must not be null");
    }


}
