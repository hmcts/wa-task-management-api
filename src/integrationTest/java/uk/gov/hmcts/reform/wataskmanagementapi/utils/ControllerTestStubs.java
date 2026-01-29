package uk.gov.hmcts.reform.wataskmanagementapi.utils;

import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;

import static org.mockito.Mockito.lenient;

public final class ControllerTestStubs {

    private ControllerTestStubs() {
    }

    public static UserInfo stubUserInfo(IdamWebApi idamWebApi,
                                        AuthTokenGenerator serviceAuthTokenGenerator,
                                        String idamAuthToken,
                                        String serviceAuthToken,
                                        String userId,
                                        String userName) {
        UserInfo userInfo = UserInfo.builder()
            .uid(userId)
            .name(userName)
            .build();

        lenient().when(serviceAuthTokenGenerator.generate()).thenReturn(serviceAuthToken);
        lenient().when(idamWebApi.userInfo(idamAuthToken)).thenReturn(userInfo);

        return userInfo;
    }
}
