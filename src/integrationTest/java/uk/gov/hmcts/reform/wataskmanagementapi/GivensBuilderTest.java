package uk.gov.hmcts.reform.wataskmanagementapi;

import feign.FeignException;
import io.restassured.http.Header;
import io.restassured.http.Headers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.config.CcdRetryableClient;
import uk.gov.hmcts.reform.wataskmanagementapi.config.GivensBuilder;
import uk.gov.hmcts.reform.wataskmanagementapi.config.RestApiActions;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestAuthenticationCredentials;
import uk.gov.hmcts.reform.wataskmanagementapi.services.AuthorizationProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.services.DocumentManagementFiles;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.SERVICE_AUTHORIZATION;

public class GivensBuilderTest {

    protected GivensBuilder given;
    @Mock
    TestAuthenticationCredentials credentials;

    @Mock
    private RestApiActions camundaApiActions;
    @Mock
    private RestApiActions workflowApiActions;
    @Mock
    private RestApiActions restApiActions;
    @Mock
    private AuthorizationProvider authorizationProvider;
    @Mock
    private DocumentManagementFiles documentManagementFiles;

    private CcdRetryableClient ccdRetryableClient;
    @Mock
    CoreCaseDataApi coreCaseDataApi;
    @Mock
    UserInfo userInfo;


    @Before
    public void setUp() {
        credentials = mock(TestAuthenticationCredentials.class);
        userInfo = mock(UserInfo.class);
        authorizationProvider = mock(AuthorizationProvider.class);
        documentManagementFiles = mock(DocumentManagementFiles.class);
        coreCaseDataApi = mock(CoreCaseDataApi.class);

        ccdRetryableClient = new CcdRetryableClient(coreCaseDataApi);
        given = new GivensBuilder(
            camundaApiActions,
            restApiActions,
            authorizationProvider,
            ccdRetryableClient,
            documentManagementFiles,
            workflowApiActions
        );
    }

    @Test
    public void testRetryableCreateCcdCase() {
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
            given.createCCDCaseWithJurisdictionAndCaseTypeAndEvent("WA",
                                                                   "WaCaseType",
                                                                   "CREATE",
                                                                   "START_PROGRESS",
                                                                   credentials,
                                                                   "requests/ccd/wa_case_data.json");
        } catch (FeignException e) {
            verify(coreCaseDataApi, times(3)).startForCaseworker("accessToken", "accessToken",
                                                                 "userId", "WA",
                                                                 "WaCaseType", "CREATE");
        }
    }
}
