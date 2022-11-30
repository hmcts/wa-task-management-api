package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootIntegrationBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.response.RoleAssignmentResource;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.RoleAssignmentServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.TaskTypesDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.TaskTypesDmnResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.services.DmnEvaluationService;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks;

import java.util.List;
import java.util.Set;

import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.SERVICE_AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.IDAM_AUTHORIZATION_TOKEN;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.SERVICE_AUTHORIZATION_TOKEN;

@SuppressWarnings("checkstyle:LineLength")
@Slf4j
@ExtendWith(MockitoExtension.class)
class TaskTypesControllerTest extends SpringBootIntegrationBaseTest {

    private static final String ENDPOINT_PATH = "/task/task-types";
    private static final String DMN_NAME = "Task Types DMN";

    @MockBean
    private IdamWebApi idamWebApi;

    @MockBean
    private CamundaServiceApi camundaServiceApi;

    @MockBean
    private RoleAssignmentServiceApi roleAssignmentServiceApi;

    @MockBean
    private ServiceAuthorisationApi serviceAuthorisationApi;

    @MockBean
    private AuthTokenGenerator serviceAuthTokenGenerator;

    @MockBean
    private DmnEvaluationService dmnEvaluationService;

    private ServiceMocks mockServices;
    private UserInfo mockedUserInfo;

    @BeforeEach
    void setUp() {
        mockedUserInfo = UserInfo.builder().uid(ServiceMocks.IDAM_USER_ID).name("someUser").build();
        lenient().when(serviceAuthTokenGenerator.generate()).thenReturn(SERVICE_AUTHORIZATION_TOKEN);
        lenient().when(idamWebApi.userInfo(IDAM_AUTHORIZATION_TOKEN)).thenReturn(mockedUserInfo);
        mockServices = new ServiceMocks(
            idamWebApi,
            serviceAuthorisationApi,
            camundaServiceApi,
            roleAssignmentServiceApi
        );
    }

    @Test
    void should_return_all_task_types() throws Exception {

        String jurisdiction = "wa";
        final List<String> roleNames = singletonList("tribunal-caseworker");
        List<RoleAssignment> allTestRoles = mockServices.createTestRoleAssignments(roleNames);

        when(roleAssignmentServiceApi.getRolesForUser(any(), anyString(), anyString()))
            .thenReturn(new RoleAssignmentResource(allTestRoles));

        TaskTypesDmnResponse taskTypesDmnResponse = new TaskTypesDmnResponse(
            "wa-task-types-wa-wacasetype", jurisdiction, "wa-task-types-wa-wacasetype.dmn");

        Set<TaskTypesDmnResponse> taskTypesDmnResponses = Set.of(taskTypesDmnResponse);
        when(dmnEvaluationService.getTaskTypesDmn(jurisdiction, DMN_NAME))
            .thenReturn(taskTypesDmnResponses);

        CamundaValue<String> taskTypeId = new CamundaValue<>("processApplication", "String");
        CamundaValue<String> taskTypeName = new CamundaValue<>("Process Application", "String");
        TaskTypesDmnEvaluationResponse taskTypesDmnEvaluationResponse = new TaskTypesDmnEvaluationResponse(taskTypeId, taskTypeName);
        List<TaskTypesDmnEvaluationResponse> taskTypesDmnEvaluationResponses = List.of(taskTypesDmnEvaluationResponse);
        when(dmnEvaluationService.evaluateTaskTypesDmn(jurisdiction, taskTypesDmnResponse.getKey()))
            .thenReturn(taskTypesDmnEvaluationResponses);

        mockMvc.perform(
            get(ENDPOINT_PATH + "?jurisdiction=" + jurisdiction)
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        ).andDo(mvcResult -> {
            log.info("task_types: {}", mvcResult.getResponse().getContentAsString());
        }).andExpectAll(
            status().isOk(),
            jsonPath("$.task_types").isNotEmpty(),
            jsonPath("$.task_types.length()").value(1),
            jsonPath("$.task_types[0].task_type.task_type_id")
                .value("processApplication"),
            jsonPath("$.task_types[0]..task_type.task_type_name")
                .value("Process Application")
        );
    }

}


