package uk.gov.hmcts.reform.wataskmanagementapi.config;

import jakarta.annotation.PostConstruct;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.Token;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;
import uk.gov.hmcts.reform.wataskmanagementapi.services.IdamServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

public class IdamMockitoStubber {

    private static final String BEARER_PREFIX = "Bearer ";

    private final IdamWebApi idamWebApi;
    private final IdamServiceApi idamServiceApi;

    public IdamMockitoStubber(IdamWebApi idamWebApi, IdamServiceApi idamServiceApi) {
        this.idamWebApi = idamWebApi;
        this.idamServiceApi = idamServiceApi;
    }

    @PostConstruct
    void stubDefaults() {
        String accessToken = stripBearer(ServiceMocks.IDAM_AUTHORIZATION_TOKEN);
        UserInfo defaultUserInfo = UserInfo.builder()
            .uid(ServiceMocks.IDAM_USER_ID)
            .email(ServiceMocks.IDAM_USER_EMAIL)
            .roles(List.of())
            .build();

        lenient().when(idamWebApi.userInfo(anyString())).thenReturn(defaultUserInfo);
        lenient().when(idamWebApi.token(any())).thenReturn(new Token(accessToken, "scope"));

        // Default no-ops for test support endpoints
        lenient().doNothing().when(idamServiceApi).createTestUser(any());
        lenient().doNothing().when(idamServiceApi).deleteTestUser(anyString());
    }

    private static String stripBearer(String token) {
        if (token == null) {
            return "stub-token";
        }
        if (token.startsWith(BEARER_PREFIX)) {
            return token.substring(BEARER_PREFIX.length());
        }
        return token;
    }
}
