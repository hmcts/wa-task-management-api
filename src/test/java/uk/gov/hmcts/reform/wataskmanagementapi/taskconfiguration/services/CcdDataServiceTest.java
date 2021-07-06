package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.auth.idam.IdamTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.clients.CcdDataServiceApi;

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

        String caseData = "{ "
                          + "\"jurisdiction\": \"ia\", "
                          + "\"case_type_id\": \"Asylum\", "
                          + "\"security_classification\": \"PUBLIC\","
                          + "\"data\": {}"
                          + " }";

        when(idamTokenGenerator.generate()).thenReturn(userToken);
        when(authTokenGenerator.generate()).thenReturn(serviceToken);

        when(ccdDataServiceApi.getCase(userToken, serviceToken, caseId)).thenReturn(caseData);

        String actualCaseData = ccdDataService.getCaseData(caseId);

        assertEquals(caseData, actualCaseData);
    }
}
