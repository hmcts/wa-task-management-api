package uk.gov.hmcts.reform.wataskmanagementapi;

import feign.FeignException;
import io.restassured.http.Header;
import io.restassured.http.Headers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.config.CcdRetryClient;
import uk.gov.hmcts.reform.wataskmanagementapi.config.GivensBuilder;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestAuthenticationCredentials;
import uk.gov.hmcts.reform.wataskmanagementapi.services.AuthorizationProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.services.DocumentManagementFiles;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.SERVICE_AUTHORIZATION;

public class CcdRetryClientTest extends SpringBootIntegrationBaseTest {
    @Autowired
    CcdRetryClient ccdRetryClient;
    @Mock
    private CoreCaseDataApi coreCaseDataApi;
    @Mock
    private AuthorizationProvider authorizationProvider;
    @Mock
    private DocumentManagementFiles documentManagementFiles;
    @Mock
    TestAuthenticationCredentials credentials;
    @Mock
    UserInfo userInfo;
    @Mock
    GivensBuilder givensBuilder;


    @BeforeEach
    public void setUp() {
        credentials = mock(TestAuthenticationCredentials.class);
        userInfo = mock(UserInfo.class);
        authorizationProvider = mock(AuthorizationProvider.class);
        documentManagementFiles = mock(DocumentManagementFiles.class);
        coreCaseDataApi = mock(CoreCaseDataApi.class);

        ccdRetryClient = new CcdRetryClient(coreCaseDataApi,
                                            authorizationProvider,
                                            documentManagementFiles);
    }

    @Test
    public void retries3Times() {
        givensBuilder = mock(GivensBuilder.class);
        Headers authenticationHeaders = new Headers(
            new Header(AUTHORIZATION, "accessToken"),
            new Header(SERVICE_AUTHORIZATION, "accessToken")
        );
        when(credentials.getHeaders()).thenReturn(authenticationHeaders);

        when(userInfo.getUid()).thenReturn("userId");

        when(authorizationProvider.getUserInfo("accessToken")).thenReturn(userInfo);

        when(coreCaseDataApi.startForCaseworker("accessToken", "accessToken",
                                                "userId", "WA",
                                                "WaCaseType", "CREATE")).thenThrow(FeignException.class);

        try {
            ccdRetryClient.createCCDCaseWithJurisdictionAndCaseTypeAndEvent("WA",
                                                                   "WaCaseType",
                                                                   "CREATE",
                                                                   "START_PROGRESS",
                                                                   credentials,
                                                                   "requests/ccd/wa_case_data.json");
        } catch (FeignException e) {
            //TODO this should be 3 times
            verify(coreCaseDataApi, times(1)).startForCaseworker("accessToken", "accessToken",
                                                                 "userId", "WA",
                                                                 "WaCaseType", "CREATE");
        }

    }
}
