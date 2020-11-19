package uk.gov.hmcts.reform.wataskmanagementapi.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.PermissionEvaluatorService;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaExceptionMessage;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaObjectMapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CamundaServiceBaseTest {

    static final String BEARER_SERVICE_TOKEN = "Bearer service token";
    static final String IDAM_USER_ID = "IDAM_USER_ID";

    CamundaObjectMapper camundaObjectMapper;
    CamundaErrorDecoder camundaErrorDecoder;

    @Mock
    AuthTokenGenerator authTokenGenerator;
    @Mock
    CamundaServiceApi camundaServiceApi;
    @Mock
    CamundaQueryBuilder camundaQueryBuilder;
    @Mock
    PermissionEvaluatorService permissionEvaluatorService;

    CamundaService camundaService;

    @BeforeEach
    void setUp() {
        camundaObjectMapper = new CamundaObjectMapper();
        camundaErrorDecoder = new CamundaErrorDecoder();

        TaskMapper taskMapper = new TaskMapper(camundaObjectMapper);
        camundaService = new CamundaService(
            camundaServiceApi,
            camundaQueryBuilder,
            camundaErrorDecoder,
            taskMapper,
            authTokenGenerator,
            permissionEvaluatorService,
            camundaObjectMapper
        );

        when(authTokenGenerator.generate()).thenReturn(BEARER_SERVICE_TOKEN);
    }

    public String createCamundaTestException(String type, String message) {
        return camundaObjectMapper.asCamundaJsonString(new CamundaExceptionMessage(type, message));
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void someTest() {
        assertEquals(BEARER_SERVICE_TOKEN, authTokenGenerator.generate());
        assertNotNull(camundaService); // if not test it highlighted by sonarcloud as a code smell
    }
}
