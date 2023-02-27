package uk.gov.hmcts.reform.wataskmanagementapi.services;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.IdamTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CcdDataServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.ccd.CaseDetails;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CcdDataServiceTest {

    @Mock
    CcdDataServiceApi ccdDataServiceApi;

    @Mock
    AuthTokenGenerator authTokenGenerator;

    @Mock
    IdamTokenGenerator idamTokenGenerator;

    @Mock
    private CaseDetails caseDetails;

    private CcdDataService ccdDataService;

    @Before
    public void setUp() {
        ccdDataService = new CcdDataService(ccdDataServiceApi, authTokenGenerator, idamTokenGenerator);
    }

    @Test
    public void should_get_case_data() {
        String caseId = UUID.randomUUID().toString();
        String userToken = "user_token";
        String serviceToken = "service_token";

        when(idamTokenGenerator.generate()).thenReturn(userToken);
        when(authTokenGenerator.generate()).thenReturn(serviceToken);

        when(ccdDataServiceApi.getCase(userToken, serviceToken, caseId)).thenReturn(caseDetails);

        CaseDetails actualCaseData = ccdDataService.getCaseData(caseId);

        assertEquals(actualCaseData, caseDetails);
    }
}
