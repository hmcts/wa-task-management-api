package uk.gov.hmcts.reform.wataskmanagementapi.auth.idam;

import io.restassured.http.Header;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootIntegrationBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;
import uk.gov.hmcts.reform.wataskmanagementapi.services.AuthorizationHeadersProvider;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdamServiceTest extends SpringBootIntegrationBaseTest {

    @Autowired
    protected AuthorizationHeadersProvider authorizationHeadersProvider;

    private String USER_TOKEN;

    @Autowired
    private IdamService idamService;

    @Autowired
    private IdamWebApi idamWebApi;

    @Test
    public void should_cache_response() {

        UserInfo first = new UserInfo(
            "email1@example.net",
            "1",
            Arrays.asList("role1", "role2"),
            "name1",
            "givenName1",
            "firstName1"
        );

        UserInfo second = new UserInfo(
            "email2@example.net",
            "2",
            Arrays.asList("role1", "role2"),
            "name2",
            "givenName2",
            "firstName2"
        );

        Header userToken = authorizationHeadersProvider.getLawFirmAuthorizationOnly();
        USER_TOKEN = userToken.getValue();

        when(idamService.getUserInfo(USER_TOKEN)).thenReturn(first, second);

        //First invocation should return first mock value
        String resultFirstInvocation = idamService.getUserId(USER_TOKEN);
        assertEquals(first.getUid(), resultFirstInvocation);

        // Second invocation should return cached value and not the second value as defined in mock
        String resultSecondInvocation = idamService.getUserId(USER_TOKEN);
        assertEquals(first.getUid(), resultSecondInvocation);

        verify(idamWebApi, times(1)).userInfo(USER_TOKEN);

        // Third invocation with different access token should triggers the second invocation of mock
        Header userToken2 = authorizationHeadersProvider.getCaseworkerAAuthorizationOnly();
        USER_TOKEN = userToken2.getValue();

        String resultThirdInvocation = idamService.getUserId(USER_TOKEN);
        assertEquals(second.getUid(), resultThirdInvocation);

    }

}
