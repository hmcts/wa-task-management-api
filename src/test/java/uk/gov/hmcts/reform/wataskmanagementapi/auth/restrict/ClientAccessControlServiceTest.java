package uk.gov.hmcts.reform.wataskmanagementapi.auth.restrict;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.authorisation.validators.ServiceAuthTokenValidator;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.config.ClientAccessProperties;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientAccessControlServiceTest {

    private static final String SERVICE_AUTH_TOKEN = "some serviceAuthorizationToken";
    private static final String PRIVILEGED_ACCESS_SERVICE_NAME = "somePrivilegedServiceName";
    private static final String EXCLUSIVE_ACCESS_SERVICE_NAME = "someExclusiveServiceName";
    private static final String UNRESTRICTED_EXCLUSIVE_ACCESS_SERVICE_NAME = "someUnrestrictedExclusiveServiceName";
    private static final String CASE_TYPE_ID = "someCaseTypeId";
    private static final String USER_ID = "someUserId";
    private static final String EMAIL = "test@test.com";
    @Mock
    private ServiceAuthTokenValidator serviceAuthTokenValidator;
    @Mock
    private AccessControlResponse accessControlResponse;
    @Mock
    private UserInfo userInfo;

    private ClientAccessControlService clientAccessControlService;

    @BeforeEach
    void setup() {
        lenient().when(accessControlResponse.getUserInfo())
            .thenReturn(userInfo);
        lenient().when(userInfo.getUid())
            .thenReturn(USER_ID);
        lenient().when(userInfo.getEmail())
            .thenReturn(EMAIL);

        ClientAccessProperties clientAccessProperties = new ClientAccessProperties();
        clientAccessProperties.setPrivilegedAccessClients(Collections.singletonList(PRIVILEGED_ACCESS_SERVICE_NAME));
        clientAccessProperties.setExclusiveAccessClients(List.of(
            EXCLUSIVE_ACCESS_SERVICE_NAME,
            UNRESTRICTED_EXCLUSIVE_ACCESS_SERVICE_NAME
        ));
        clientAccessProperties.setServiceCaseTypeAccess(Map.of(
            EXCLUSIVE_ACCESS_SERVICE_NAME,
            List.of(CASE_TYPE_ID)
        ));

        clientAccessControlService = new ClientAccessControlService(serviceAuthTokenValidator, clientAccessProperties);
    }

    @Test
    void hasPrivilegedAccess_should_return_true_if_feature_is_enabled_and_service_whitelisted() {

        when(serviceAuthTokenValidator.getServiceName(SERVICE_AUTH_TOKEN))
            .thenReturn(PRIVILEGED_ACCESS_SERVICE_NAME);

        boolean result = clientAccessControlService.hasPrivilegedAccess(SERVICE_AUTH_TOKEN, accessControlResponse);

        assertTrue(result);
    }

    @Test
    void hasPrivilegedAccess_should_return_false_if_feature_is_disabled() {

        boolean result = clientAccessControlService.hasPrivilegedAccess(SERVICE_AUTH_TOKEN, accessControlResponse);

        assertFalse(result);
    }

    @Test
    void hasPrivilegedAccess_should_return_false_if_feature_is_enabled_and_service_is_not_whitelisted() {

        when(serviceAuthTokenValidator.getServiceName(SERVICE_AUTH_TOKEN))
            .thenReturn("anotherService");

        boolean result = clientAccessControlService.hasPrivilegedAccess(SERVICE_AUTH_TOKEN, accessControlResponse);

        assertFalse(result);
    }

    @Test
    void hasPrivilegedAccess_should_throw_null_pointer_exception_if_required_parameters_are_null() {

        assertThatThrownBy(() -> clientAccessControlService.hasPrivilegedAccess(
            null,
            accessControlResponse
        ))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("ServiceAuthorization must not be null");

        when(userInfo.getUid())
            .thenReturn(null);

        assertThatThrownBy(() -> clientAccessControlService.hasPrivilegedAccess(
            SERVICE_AUTH_TOKEN,
            accessControlResponse
        ))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("UserId must not be null");
    }


    @Test
    void hasExclusiveAccess_should_return_true_if_service_whitelisted() {

        when(serviceAuthTokenValidator.getServiceName(SERVICE_AUTH_TOKEN))
            .thenReturn(EXCLUSIVE_ACCESS_SERVICE_NAME);

        boolean result = clientAccessControlService.hasExclusiveAccess(SERVICE_AUTH_TOKEN);

        assertTrue(result);
    }

    @Test
    void hasExclusiveAccess_should_return_false_if_service_is_not_whitelisted() {
        when(serviceAuthTokenValidator.getServiceName(SERVICE_AUTH_TOKEN))
            .thenReturn("anotherService");

        boolean result = clientAccessControlService.hasExclusiveAccess(SERVICE_AUTH_TOKEN);

        assertFalse(result);
    }

    @Test
    void hasExclusiveAccess_should_throw_null_pointer_exception_if_required_parameters_are_null() {

        assertThatThrownBy(() -> clientAccessControlService.hasExclusiveAccess(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("ServiceAuthorization must not be null");

    }

    @Test
    void hasExclusiveCaseTypeAccess_should_return_true_if_service_has_case_type_access() {
        when(serviceAuthTokenValidator.getServiceName(SERVICE_AUTH_TOKEN))
            .thenReturn(EXCLUSIVE_ACCESS_SERVICE_NAME);

        boolean result = clientAccessControlService.hasExclusiveCaseTypeAccess(SERVICE_AUTH_TOKEN, CASE_TYPE_ID);

        assertTrue(result);
    }

    @Test
    void hasExclusiveCaseTypeAccess_should_return_false_if_service_is_not_exclusive() {
        when(serviceAuthTokenValidator.getServiceName(SERVICE_AUTH_TOKEN))
            .thenReturn("anotherService");

        boolean result = clientAccessControlService.hasExclusiveCaseTypeAccess(SERVICE_AUTH_TOKEN, CASE_TYPE_ID);

        assertFalse(result);
    }

    @Test
    void hasExclusiveCaseTypeAccess_should_return_true_if_exclusive_service_has_no_case_type_restrictions() {
        when(serviceAuthTokenValidator.getServiceName(SERVICE_AUTH_TOKEN))
            .thenReturn(UNRESTRICTED_EXCLUSIVE_ACCESS_SERVICE_NAME);

        boolean result = clientAccessControlService.hasExclusiveCaseTypeAccess(SERVICE_AUTH_TOKEN, "anotherCaseType");

        assertTrue(result);
    }

    @Test
    void hasExclusiveCaseTypeAccess_should_return_false_if_restricted_service_has_no_case_type_match() {
        when(serviceAuthTokenValidator.getServiceName(SERVICE_AUTH_TOKEN))
            .thenReturn(EXCLUSIVE_ACCESS_SERVICE_NAME);

        boolean result = clientAccessControlService.hasExclusiveCaseTypeAccess(SERVICE_AUTH_TOKEN, "anotherCaseType");

        assertFalse(result);
    }

    @Test
    void hasExclusiveCaseTypeAccess_should_return_false_if_case_type_is_blank() {
        boolean result = clientAccessControlService.hasExclusiveCaseTypeAccess(SERVICE_AUTH_TOKEN, " ");

        assertFalse(result);
    }

    @Test
    void hasPrivilegedAccess_should_return_true_if_service_whitelisted() {

        when(serviceAuthTokenValidator.getServiceName(SERVICE_AUTH_TOKEN))
            .thenReturn(PRIVILEGED_ACCESS_SERVICE_NAME);

        boolean result = clientAccessControlService.hasPrivilegedAccess(SERVICE_AUTH_TOKEN);

        assertTrue(result);
    }

    @Test
    void hasPrivilegedAccess_should_return_false_if_service_is_not_whitelisted() {
        when(serviceAuthTokenValidator.getServiceName(SERVICE_AUTH_TOKEN))
            .thenReturn("anotherService");

        boolean result = clientAccessControlService.hasPrivilegedAccess(SERVICE_AUTH_TOKEN);

        assertFalse(result);
    }

    @Test
    void hasPrivilegedAccess_should_throw_null_pointer_exception_if_ServiceAuthToken_is_null() {

        assertThatThrownBy(() -> clientAccessControlService.hasPrivilegedAccess(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("ServiceAuthorization must not be null");

    }
}
