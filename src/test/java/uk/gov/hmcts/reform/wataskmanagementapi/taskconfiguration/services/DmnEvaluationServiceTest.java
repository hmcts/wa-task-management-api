package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services;

import feign.FeignException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.request.DecisionTableRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.request.DmnRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.response.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.response.EvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.response.PermissionsDmnEvaluationResponse;

import java.util.List;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.DecisionTable.WA_TASK_CONFIGURATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.DecisionTable.WA_TASK_PERMISSIONS;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue.jsonValue;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue.stringValue;

@ExtendWith(MockitoExtension.class)
class DmnEvaluationServiceTest {

    private static final String BEARER_SERVICE_TOKEN = "Bearer service token";
    private static final String TASK_TYPE_ID = "taskType";
    private static final String TASK_ATTRIBUTES = "{ \"taskTypeId\": " + TASK_TYPE_ID + "}";
    DmnEvaluationService dmnEvaluationService;
    @Mock
    private CamundaServiceApi camundaServiceApi;
    @Mock
    private AuthTokenGenerator authTokenGenerator;

    @BeforeEach
    void setUp() {
        dmnEvaluationService = new DmnEvaluationService(camundaServiceApi, authTokenGenerator);
    }

    @Test
    void should_succeed_and_return_a_list_of_permissions() {
        String ccdData = getCcdData();
        List<PermissionsDmnEvaluationResponse> mockedResponse = asList(
            new PermissionsDmnEvaluationResponse(
                stringValue("tribunal-caseworker"),
                stringValue("Read,Refer,Own,Manage,Cancel"),
                null,
                null,
                null,
                stringValue("LEGAL_OPERATIONS")
            ),
            new PermissionsDmnEvaluationResponse(
                stringValue("senior-tribunal-caseworker"),
                stringValue("Read,Refer,Own,Manage,Cancel"),
                null,
                null,
                null,
                stringValue("LEGAL_OPERATIONS")
            )
        );

        doReturn(mockedResponse).when(camundaServiceApi).evaluatePermissionsDmnTable(
            BEARER_SERVICE_TOKEN,
            WA_TASK_PERMISSIONS.getTableKey("ia", "asylum"),
            "ia",
            new DmnRequest<>(new DecisionTableRequest(jsonValue(ccdData), jsonValue(TASK_ATTRIBUTES)))
        );

        when(authTokenGenerator.generate()).thenReturn(BEARER_SERVICE_TOKEN);

        List<PermissionsDmnEvaluationResponse> response = dmnEvaluationService.evaluateTaskPermissionsDmn(
            "ia",
            "Asylum",
            ccdData,
            TASK_ATTRIBUTES
        );

        assertThat(response.size(), is(2));

        assertThat(response.get(0).getName(), is(stringValue("tribunal-caseworker")));
        assertThat(response.get(0).getValue(), is(stringValue("Read,Refer,Own,Manage,Cancel")));
        assertNull(response.get(0).getAutoAssignable());
        assertNull(response.get(0).getAuthorisations());
        assertNull(response.get(0).getAssignmentPriority());

        assertThat(response.get(1).getName(), is(stringValue("senior-tribunal-caseworker")));
        assertThat(response.get(1).getValue(), is(stringValue("Read,Refer,Own,Manage,Cancel")));
        assertNull(response.get(1).getAutoAssignable());
        assertNull(response.get(1).getAuthorisations());
        assertNull(response.get(1).getAssignmentPriority());

    }

    @Test
    void should_throw_illegal_state_exception_when_feign_exception_is_caught() {
        String ccdData = getCcdData();

        when(camundaServiceApi.evaluatePermissionsDmnTable(
            BEARER_SERVICE_TOKEN,
            WA_TASK_PERMISSIONS.getTableKey("ia", "asylum"),
            "ia",
            new DmnRequest<>(new DecisionTableRequest(jsonValue(ccdData), jsonValue(TASK_ATTRIBUTES)))
        )).thenThrow(FeignException.class);

        when(authTokenGenerator.generate()).thenReturn(BEARER_SERVICE_TOKEN);

        assertThatThrownBy(() -> dmnEvaluationService
            .evaluateTaskPermissionsDmn("ia", "Asylum", ccdData, TASK_ATTRIBUTES))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Could not evaluate from decision table wa-task-permissions-ia-asylum")
            .hasCauseInstanceOf(FeignException.class);
    }

    @Test
    void should_succeed_and_return_a_list_of_configurations() {
        String ccdData = getCcdData();

        List<? extends EvaluationResponse> mockedResponse = asList(
            new ConfigurationDmnEvaluationResponse(
                stringValue("someConfigName1"),
                stringValue("someConfigValue1")
            ),
            new ConfigurationDmnEvaluationResponse(
                stringValue("someConfigName2"),
                stringValue("someConfigValue2")
            )
        );

        doReturn(mockedResponse).when(camundaServiceApi).evaluateConfigurationDmnTable(
            BEARER_SERVICE_TOKEN,
            WA_TASK_CONFIGURATION.getTableKey("ia", "asylum"),
            "ia",
            new DmnRequest<>(new DecisionTableRequest(jsonValue(ccdData), jsonValue(TASK_ATTRIBUTES)))
        );

        when(authTokenGenerator.generate()).thenReturn(BEARER_SERVICE_TOKEN);

        List<ConfigurationDmnEvaluationResponse> response = dmnEvaluationService.evaluateTaskConfigurationDmn(
            "ia",
            "Asylum",
            ccdData,
            TASK_ATTRIBUTES
        );

        assertThat(response.size(), is(2));

        assertThat(response.get(0).getName(), is(stringValue("someConfigName1")));
        assertThat(response.get(0).getValue(), is(stringValue("someConfigValue1")));
        assertThat(response.get(1).getName(), is(stringValue("someConfigName2")));
        assertThat(response.get(1).getValue(), is(stringValue("someConfigValue2")));
    }

    @Test
    void should_throw_illegal_state_exception_when_feign_exception_is_caught_when_get_configuration() {
        String ccdData = getCcdData();

        when(camundaServiceApi.evaluateConfigurationDmnTable(
            BEARER_SERVICE_TOKEN,
            WA_TASK_CONFIGURATION.getTableKey("ia", "asylum"),
            "ia",
            new DmnRequest<>(new DecisionTableRequest(jsonValue(ccdData), jsonValue(TASK_ATTRIBUTES)))
        )).thenThrow(FeignException.class);

        when(authTokenGenerator.generate()).thenReturn(BEARER_SERVICE_TOKEN);

        assertThatThrownBy(() -> dmnEvaluationService
            .evaluateTaskConfigurationDmn("ia", "Asylum", ccdData, TASK_ATTRIBUTES))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Could not evaluate from decision table wa-task-configuration-ia-asylum")
            .hasCauseInstanceOf(FeignException.class);
    }

    @NotNull
    private String getCcdData() {
        return "{"
            + "\"jurisdiction\": \"ia\","
            + "\"case_type_id\": \"Asylum\","
            + "\"security_classification\": \"PUBLIC\","
            + "\"data\": {}"
            + "}";
    }
}
