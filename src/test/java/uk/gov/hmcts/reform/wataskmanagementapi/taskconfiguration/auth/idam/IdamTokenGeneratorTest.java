package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.auth.idam;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.Token;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.auth.idam.entities.UserIdamTokenGeneratorInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.clients.IdamServiceApi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class IdamTokenGeneratorTest {

    @Mock
    IdamServiceApi idamServiceApi;
    @Mock
    Token token;
    @Mock
    UserInfo userInfo;

    private IdamTokenGenerator idamTokenGenerator;

    final String idamRedirectUrl = "idamRedirectUrl";
    final String idamClientId = "idamClientId";
    final String idamClientSecret = "idamClientSecret";
    final String systemUserName = "systemUserName";
    final String systemUserPass = "systemUserPass";
    final String systemUserScope = "systemUserScope";

    @Before
    public void setUp() {
        UserIdamTokenGeneratorInfo userIdamTokenGeneratorInfo = UserIdamTokenGeneratorInfo.builder()
            .idamClientId(idamClientId)
            .idamClientSecret(idamClientSecret)
            .idamRedirectUrl(idamRedirectUrl)
            .userName(systemUserName)
            .userPassword(systemUserPass)
            .idamScope(systemUserScope)
            .build();

        idamTokenGenerator = new IdamTokenGenerator(
            userIdamTokenGeneratorInfo,
            idamServiceApi
        );
    }

    @Test
    public void should_generate() {
        final String returnToken = "returnToken";

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("grant_type", "password");
        map.add("redirect_uri", idamRedirectUrl);
        map.add("client_id", idamClientId);
        map.add("client_secret", idamClientSecret);
        map.add("username", systemUserName);
        map.add("password", systemUserPass);
        map.add("scope", systemUserScope);

        when(idamServiceApi.token(map)).thenReturn(token);
        when(token.getAccessToken()).thenReturn(returnToken);

        final String actualToken = idamTokenGenerator.generate();

        assertEquals("Bearer " + returnToken, actualToken);
    }

    @Test
    public void getUserInfo() {
        final String bearerAccessToken = "Bearer accessToken";
        when(idamServiceApi.userInfo(bearerAccessToken)).thenReturn(userInfo);

        final UserInfo actualUserInfo = idamTokenGenerator.getUserInfo(bearerAccessToken);

        assertEquals(actualUserInfo, userInfo);
    }
}
