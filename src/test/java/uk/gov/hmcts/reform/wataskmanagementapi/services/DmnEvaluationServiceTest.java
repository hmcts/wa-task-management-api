package uk.gov.hmcts.reform.wataskmanagementapi.services;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import org.assertj.core.api.Assertions;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaExceptionMessage;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaObjectMapper;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.DecisionTableRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.DmnRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.EvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.PermissionsDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.TaskTypesDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.TaskTypesDmnResponse;

import java.util.ArrayList;
import java.util.HashMap;
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
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.DecisionTable.WA_TASK_CONFIGURATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.DecisionTable.WA_TASK_PERMISSIONS;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaValue.booleanValue;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaValue.integerValue;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaValue.jsonValue;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaValue.stringValue;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class DmnEvaluationServiceTest {

    private static final String BEARER_SERVICE_TOKEN = "Bearer service token";
    private static final String TASK_TYPE_ID = "taskType";
    private static final String TASK_ATTRIBUTES = "{ \"taskTypeId\": " + TASK_TYPE_ID + "}";
    private static final String DMN_NAME = "Task Types DMN";

    @Mock
    private CamundaServiceApi camundaServiceApi;
    @Mock
    private AuthTokenGenerator authTokenGenerator;
    @Mock
    private CamundaObjectMapper camundaObjectMapper;

    DmnRequest<DecisionTableRequest> dmnRequest = new DmnRequest<>();
    DmnEvaluationService dmnEvaluationService;
    Request request = Request.create(Request.HttpMethod.GET, "url",
        new HashMap<>(), null, new RequestTemplate());

    @BeforeEach
    void setUp() {
        dmnEvaluationService = new DmnEvaluationService(
            camundaServiceApi,
            authTokenGenerator,
            camundaObjectMapper
        );

        when(authTokenGenerator.generate()).thenReturn(BEARER_SERVICE_TOKEN);
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

        assertThatThrownBy(() -> dmnEvaluationService
            .evaluateTaskPermissionsDmn("ia", "Asylum", ccdData, TASK_ATTRIBUTES))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Could not evaluate from decision table wa-task-permissions-ia-asylum")
            .hasCauseInstanceOf(FeignException.class);
    }

    @Test
    void should_succeed_and_return_a_list_of_configurations() {
        String ccdData = getCcdData();

        List<ConfigurationDmnEvaluationResponse> mockedResponse = asList(
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

        Set<TaskTypesDmnResponse> response = dmnEvaluationService.retrieveTaskTypesDmn(
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
    void should_throw_502_exception_when_retrieving_task_type_dmn_and_camunda_service_throws_an_exception(
        CapturedOutput output) {

        FeignException exception = createFeignExceptionFor502();
        String jurisdiction = "wa502";
        doThrow(exception)
            .when(camundaServiceApi)
            .getTaskTypesDmnTable(
                BEARER_SERVICE_TOKEN,
                jurisdiction,
                DMN_NAME
            );

        CamundaExceptionMessage camundaExceptionMessage = new CamundaExceptionMessage("some_type",
            "some_message");

        when(camundaObjectMapper.readValue(exception.contentUTF8(), CamundaExceptionMessage.class))
            .thenReturn(camundaExceptionMessage);

        assertThatThrownBy(() -> dmnEvaluationService
            .retrieveTaskTypesDmn(jurisdiction, DMN_NAME)
        )
            .isInstanceOf(FeignException.BadRequest.class)
            .hasMessage("Downstream Dependency Error");

        String expectedMessage = String.format("An error occurred when getting task-type dmn. "
                                               + "Could not get Task Types DMN from camunda for %s. "
                                               + "Exception: Downstream Dependency Error", jurisdiction);

        Assertions.assertThat(output.getOut().contains(expectedMessage));


        expectedMessage = "An error occurred when getting task-type dmn. "
                          + "CamundaException type:some_type message:some_message";

        Assertions.assertThat(output.getOut().contains(expectedMessage));

    }

    @Test
    void should_throw_503_exception_when_retrieving_task_type_dmn_and_camunda_service_unavailable(
        CapturedOutput output) {

        FeignException exception = createFeignExceptionFor503();
        String jurisdiction = "wa503";
        doThrow(exception)
            .when(camundaServiceApi)
            .getTaskTypesDmnTable(
                BEARER_SERVICE_TOKEN,
                jurisdiction,
                DMN_NAME
            );

        assertThatThrownBy(() -> dmnEvaluationService
            .retrieveTaskTypesDmn(jurisdiction, DMN_NAME)
        )
            .isInstanceOf(FeignException.ServiceUnavailable.class)
            .hasMessage("Service unavailable");

        String expectedMessage = String.format(
            "An error occurred when getting task-type dmn due to service unavailable. "
            + "Could not get Task Types DMN from camunda for %s. "
            + "Exception: Service unavailable", jurisdiction);

        Assertions.assertThat(output.getOut().contains(expectedMessage));
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
    void should_throw_502_exception_when_evaluating_task_type_dmn_and_camunda_service_throws_an_exception(
        CapturedOutput output) {

        FeignException exception = createFeignExceptionFor502();
        String jurisdiction = "ia502";
        doThrow(exception)
            .when(camundaServiceApi)
            .evaluateTaskTypesDmnTable(
                BEARER_SERVICE_TOKEN,
                DMN_NAME,
                jurisdiction,
                dmnRequest
            );

        doThrow(NullPointerException.class)
            .when(camundaObjectMapper)
            .readValue(exception.contentUTF8(), CamundaExceptionMessage.class);

        assertThatThrownBy(() -> dmnEvaluationService
            .evaluateTaskTypesDmn(jurisdiction, DMN_NAME)
        )
            .isInstanceOf(FeignException.BadRequest.class)
            .hasMessage("Downstream Dependency Error");

        String expectedMessage = String.format("An error occurred when evaluating task-type dmn. "
                                               + "jurisdiction:%s - decisionTableKey:Task Types DMN. "
                                               + "Exception:Downstream Dependency Error", jurisdiction);
        Assertions.assertThat(output.getOut().contains(expectedMessage));

        expectedMessage = "An error occurred when reading CamundaException. Exception:Downstream Dependency Error";
        Assertions.assertThat(output.getOut().contains(expectedMessage));
    }

    @Test
    void should_throw_503_exception_when_evaluating_task_type_dmn_and_camunda_service_unavailable(
        CapturedOutput output) {

        FeignException exception = createFeignExceptionFor503();

        String jurisdiction = "ia503";
        doThrow(exception)
            .when(camundaServiceApi)
            .evaluateTaskTypesDmnTable(
                BEARER_SERVICE_TOKEN,
                DMN_NAME,
                jurisdiction,
                dmnRequest
            );

        assertThatThrownBy(() -> dmnEvaluationService
            .evaluateTaskTypesDmn(jurisdiction, DMN_NAME)
        )
            .isInstanceOf(FeignException.class)
            .hasMessage("Service unavailable");

        String expectedMessage = String.format(
            "An error occurred when evaluating task-type dmn due to service unavailable. "
            + "jurisdiction:%s - decisionTableKey:Task Types DMN. "
            + "Exception:Service unavailable", jurisdiction);
        Assertions.assertThat(output.getOut().contains(expectedMessage));
    }


    @Test
    void should_remove_spaces_from_configuration_dmn_response() {
        String ccdData = "{}";

        List<ConfigurationDmnEvaluationResponse> mockedResponse = asList(
            new ConfigurationDmnEvaluationResponse(
                stringValue("key1"),
                stringValue(" value1, value2, value3,value4   ")
            ),
            new ConfigurationDmnEvaluationResponse(
                stringValue("key2"),
                stringValue("value5"),
                null
            ),
            new ConfigurationDmnEvaluationResponse(
                stringValue("key3"),
                stringValue("value6,value7"),
                booleanValue(true)
            )
        );

        String jurisdiction = "wa";
        String caseTypeId = "wacasetype";

        doReturn(mockedResponse)
            .when(camundaServiceApi)
            .evaluateConfigurationDmnTable(
                BEARER_SERVICE_TOKEN,
                WA_TASK_CONFIGURATION.getTableKey(jurisdiction, caseTypeId),
                jurisdiction,
                new DmnRequest<>(new DecisionTableRequest(jsonValue(ccdData), jsonValue(TASK_ATTRIBUTES)))
            );

        List<ConfigurationDmnEvaluationResponse> response = dmnEvaluationService.evaluateTaskConfigurationDmn(
            jurisdiction,
            caseTypeId,
            ccdData,
            TASK_ATTRIBUTES
        );

        assertThat(response.size(), is(3));

        assertThat(response.get(0).getName(), is(stringValue("key1")));
        assertThat(response.get(0).getValue(), is(stringValue("value1,value2,value3,value4")));
        assertNull(response.get(0).getCanReconfigure());

        assertThat(response.get(1).getName(), is(stringValue("key2")));
        assertThat(response.get(1).getValue(), is(stringValue("value5")));
        assertNull(response.get(1).getCanReconfigure());

        assertThat(response.get(2).getName(), is(stringValue("key3")));
        assertThat(response.get(2).getValue(), is(stringValue("value6,value7")));
        assertThat(response.get(2).getCanReconfigure(), is(booleanValue(true)));
    }

    @Test
    void should_remove_spaces_from_permission_dmn_response() {
        String ccdData = "{}";

        List<PermissionsDmnEvaluationResponse> mockedResponse = asList(
            new PermissionsDmnEvaluationResponse(
                stringValue("tribunal-caseworker"),
                stringValue(" Read,  Refer,Own ,   Manage,Cancel "),
                stringValue("IA,WA"),
                integerValue(2),
                booleanValue(true),
                stringValue("LEGAL_OPERATIONS"),
                stringValue(null)
            ),
            new PermissionsDmnEvaluationResponse(
                stringValue("caseworker"),
                stringValue("Read"),
                stringValue(" IA, WA, SSCS "),
                null,
                booleanValue(false),
                stringValue("LEGAL_OPERATIONS"),
                stringValue(null)
            ),
            new PermissionsDmnEvaluationResponse(
                stringValue("senior-tribunal-caseworker"),
                stringValue("Read,Refer,Own,Manage"),
                null,
                null,
                null,
                stringValue("CTSC"),
                stringValue("categoryA")
            ),
            new PermissionsDmnEvaluationResponse(
                stringValue("invalid-caseworker"),
                stringValue("  "),
                null,
                null,
                null,
                stringValue(" "),
                stringValue(" categoryA, categoryB ")
            )
        );

        String jurisdiction = "wa";
        String caseTypeId = "wacasetype";

        doReturn(mockedResponse)
            .when(camundaServiceApi)
            .evaluatePermissionsDmnTable(
                BEARER_SERVICE_TOKEN,
                WA_TASK_PERMISSIONS.getTableKey(jurisdiction, caseTypeId),
                jurisdiction,
                new DmnRequest<>(new DecisionTableRequest(jsonValue(ccdData), jsonValue(TASK_ATTRIBUTES)))
            );

        List<PermissionsDmnEvaluationResponse> response = dmnEvaluationService.evaluateTaskPermissionsDmn(
            jurisdiction,
            caseTypeId,
            ccdData,
            TASK_ATTRIBUTES
        );

        assertThat(response.size(), is(4));

        assertThat(response.get(0).getName(), is(stringValue("tribunal-caseworker")));
        assertThat(response.get(0).getValue(), is(stringValue("Read,Refer,Own,Manage,Cancel")));
        assertThat(response.get(0).getAuthorisations(), is(stringValue("IA,WA")));
        assertThat(response.get(0).getAssignmentPriority(), is(integerValue(2)));
        assertThat(response.get(0).getAutoAssignable(), is(booleanValue(true)));
        assertThat(response.get(0).getRoleCategory(), is(stringValue("LEGAL_OPERATIONS")));
        assertThat(response.get(0).getCaseAccessCategory(), is(stringValue(null)));

        assertThat(response.get(1).getName(), is(stringValue("caseworker")));
        assertThat(response.get(1).getValue(), is(stringValue("Read")));
        assertThat(response.get(1).getAuthorisations(), is(stringValue("IA,WA,SSCS")));
        assertNull(response.get(1).getAssignmentPriority());
        assertThat(response.get(1).getAutoAssignable(), is(booleanValue(false)));
        assertThat(response.get(1).getRoleCategory(), is(stringValue("LEGAL_OPERATIONS")));
        assertThat(response.get(1).getCaseAccessCategory(), is(stringValue(null)));

        assertThat(response.get(2).getName(), is(stringValue("senior-tribunal-caseworker")));
        assertThat(response.get(2).getValue(), is(stringValue("Read,Refer,Own,Manage")));
        assertNull(response.get(2).getAuthorisations());
        assertNull(response.get(2).getAssignmentPriority());
        assertNull(response.get(2).getAutoAssignable());
        assertThat(response.get(2).getRoleCategory(), is(stringValue("CTSC")));
        assertThat(response.get(2).getCaseAccessCategory(), is(stringValue("categoryA")));


        assertThat(response.get(3).getName(), is(stringValue("invalid-caseworker")));
        assertThat(response.get(3).getValue(), is(stringValue("  ")));
        assertNull(response.get(3).getAuthorisations());
        assertNull(response.get(3).getAssignmentPriority());
        assertNull(response.get(3).getAutoAssignable());
        assertThat(response.get(3).getRoleCategory(), is(stringValue(" ")));
        assertThat(response.get(3).getCaseAccessCategory(), is(stringValue("categoryA,categoryB")));
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

    private FeignException createFeignExceptionFor502() {

        return new FeignException.BadRequest(
            "Downstream Dependency Error",
            request,
            null,
            null);

    }

    private FeignException createFeignExceptionFor503() {

        return new FeignException.ServiceUnavailable(
            "Service unavailable",
            request,
            null,
            null);
    }

}
