package uk.gov.hmcts.reform.wataskmanagementapi.services;

import feign.FeignException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.DecisionTableRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.DmnRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.EvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.PermissionsDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.TaskTypesDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.TaskTypesDmnResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
    private static final String DMN_NAME = "Task Types DMN";

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
                stringValue("LEGAL_OPERATIONS"),
                stringValue(null)
            ),
            new PermissionsDmnEvaluationResponse(
                stringValue("senior-tribunal-caseworker"),
                stringValue("Read,Refer,Own,Manage,Cancel"),
                null,
                null,
                null,
                stringValue("LEGAL_OPERATIONS"),
                stringValue(null)
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

    @Test
    void should_succeed_and_return_a_list_of_task_type_dmn() {

        List<? extends EvaluationResponse> mockedResponse = singletonList(
            new TaskTypesDmnResponse(
                "someKey",
                "wa",
                "someResource"
            )
        );

        doReturn(mockedResponse)
            .when(camundaServiceApi)
            .getTaskTypesDmnTable(
                BEARER_SERVICE_TOKEN,
                "wa",
                DMN_NAME
            );

        when(authTokenGenerator.generate()).thenReturn(BEARER_SERVICE_TOKEN);

        Set<TaskTypesDmnResponse> response = dmnEvaluationService.getTaskTypesDmn(
            "wa",
            DMN_NAME
        );

        assertThat(response.size(), is(1));

        List<TaskTypesDmnResponse> taskTypesDmnResponseList = new ArrayList<>(response);

        assertThat(taskTypesDmnResponseList.get(0).getTenantId(), is("wa"));
        assertThat(taskTypesDmnResponseList.get(0).getKey(), is("someKey"));
        assertThat(taskTypesDmnResponseList.get(0).getResource(), is("someResource"));

        verify(camundaServiceApi, times(1))
            .getTaskTypesDmnTable(anyString(), anyString(), anyString());
    }

    @Test
    void should_throw_exception_when_retrieving_dmn_list() {

        doThrow(FeignException.class)
            .when(camundaServiceApi)
            .getTaskTypesDmnTable(
                BEARER_SERVICE_TOKEN,
                "wa",
                DMN_NAME
            );

        when(authTokenGenerator.generate()).thenReturn(BEARER_SERVICE_TOKEN);

        assertThatThrownBy(() -> dmnEvaluationService
            .getTaskTypesDmn("wa", DMN_NAME)
        )
            .isInstanceOf(IllegalStateException.class)
            .hasCauseInstanceOf(FeignException.class)
            .hasMessage(String.format("Could not get %s from camunda for %s", DMN_NAME, "wa"));
    }

    @Test
    void should_evaluate_task_type_dmn() {

        List<? extends EvaluationResponse> mockedResponse = asList(
            new TaskTypesDmnEvaluationResponse(
                stringValue("taskId_1"),
                stringValue("taskName_1")
            ),
            new TaskTypesDmnEvaluationResponse(
                stringValue("taskId_2"),
                stringValue("taskName_2")
            )
        );

        DmnRequest<DecisionTableRequest> dmnRequest = new DmnRequest<>();

        doReturn(mockedResponse)
            .when(camundaServiceApi)
            .evaluateTaskTypesDmnTable(BEARER_SERVICE_TOKEN,
                DMN_NAME,
                "wa",
                dmnRequest);

        when(authTokenGenerator.generate()).thenReturn(BEARER_SERVICE_TOKEN);

        List<TaskTypesDmnEvaluationResponse> response = dmnEvaluationService.evaluateTaskTypesDmn(
            "wa",
            DMN_NAME
        );

        assertThat(response.size(), is(2));

        List<TaskTypesDmnEvaluationResponse> taskTypesDmnEvaluationResponses = new ArrayList<>(response);

        assertThat(taskTypesDmnEvaluationResponses.get(0).getTaskTypeId().getValue(), is("taskId_1"));
        assertThat(taskTypesDmnEvaluationResponses.get(0).getTaskTypeName().getValue(), is("taskName_1"));
        assertThat(taskTypesDmnEvaluationResponses.get(1).getTaskTypeId().getValue(), is("taskId_2"));
        assertThat(taskTypesDmnEvaluationResponses.get(1).getTaskTypeName().getValue(), is("taskName_2"));

        verify(camundaServiceApi, times(1))
            .evaluateTaskTypesDmnTable(
                eq(BEARER_SERVICE_TOKEN),
                eq(DMN_NAME),
                eq("wa"),
                eq(dmnRequest));
    }

    @Test
    void should_throw_exception_when_evaluating_task_type_dmn() {

        doThrow(FeignException.class)
            .when(camundaServiceApi)
            .evaluateTaskTypesDmnTable(
                BEARER_SERVICE_TOKEN,
                DMN_NAME,
                "wa",
                new DmnRequest<>()
            );

        when(authTokenGenerator.generate()).thenReturn(BEARER_SERVICE_TOKEN);

        assertThatThrownBy(() -> dmnEvaluationService
            .evaluateTaskTypesDmn("wa", DMN_NAME)
        )
            .isInstanceOf(IllegalStateException.class)
            .hasCauseInstanceOf(FeignException.class)
            .hasMessage(String.format("Could not evaluate from decision table %s", DMN_NAME));
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
