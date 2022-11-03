package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import feign.FeignException;
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
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.RoleAssignmentServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks;

import java.util.List;

import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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

        final List<String> roleNames = singletonList("tribunal-caseworker");
        List<RoleAssignment> allTestRoles = mockServices.createTestRoleAssignments(roleNames);

        when(roleAssignmentServiceApi.getRolesForUser(any(), anyString(), anyString()))
            .thenReturn(new RoleAssignmentResource(allTestRoles));

        mockMvc.perform(
            get(ENDPOINT_PATH + "?jurisdiction=wa")
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        ).andDo(mvcResult -> {
            log.info("task_types: {}", mvcResult.getResponse().getContentAsString());
        }).andExpectAll(
            status().isOk(),
            content().contentType(APPLICATION_JSON_VALUE),
            jsonPath("$.task_types").isNotEmpty(),
            jsonPath("$.task_types.length()").value(7),
            jsonPath("$.task_types[0].task_type.task_type_id")
                .value("processApplication"),
            jsonPath("$.task_types[0]..task_type.task_type_name")
                .value("Process Application"),
            jsonPath("$.task_types[1].task_type.task_type_id")
                .value("reviewAppealSkeletonArgument"),
            jsonPath("$.task_types[1]..task_type.task_type_name")
                .value("Review Appeal Skeleton Argument"),
            jsonPath("$.task_types[2].task_type.task_type_id")
                .value("decideOnTimeExtension"),
            jsonPath("$.task_types[2]..task_type.task_type_name")
                .value("Decide On Time Extension"),
            jsonPath("$.task_types[3].task_type.task_type_id")
                .value("followUpOverdueCaseBuilding"),
            jsonPath("$.task_types[3]..task_type.task_type_name")
                .value("Follow-up overdue case building"),
            jsonPath("$.task_types[4].task_type.task_type_id")
                .value("attendCma"),
            jsonPath("$.task_types[4]..task_type.task_type_name")
                .value("Attend Cma"),
            jsonPath("$.task_types[5].task_type.task_type_id")
                .value("reviewRespondentResponse"),
            jsonPath("$.task_types[5]..task_type.task_type_name")
                .value("Review Respondent Response"),
            jsonPath("$.task_types[6].task_type.task_type_id")
                .value("followUpOverdueRespondentEvidence"),
            jsonPath("$.task_types[6]..task_type.task_type_name")
                .value("Follow-up overdue respondent evidence")
        );
    }

    @Test
    void should_return_400_when_jurisdiction_not_provided() throws Exception {

        final List<String> roleNames = singletonList("tribunal-caseworker");
        List<RoleAssignment> allTestRoles = mockServices.createTestRoleAssignments(roleNames);

        when(roleAssignmentServiceApi.getRolesForUser(any(), anyString(), anyString()))
            .thenReturn(new RoleAssignmentResource(allTestRoles));

        mockMvc.perform(
            get(ENDPOINT_PATH + "?jurisdiction=")
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        ).andExpectAll(
            status().isBadRequest(),
            content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
            //jsonPath("$.type")
            //    .value("https://github.com/hmcts/wa-task-management-api/problem/constraint-violation"),
            jsonPath("$.title")
                .value("Constraint Violation"),
            jsonPath("$.status")
                .value(400),
            jsonPath("$.violations.[0].field")
                .value("jurisdiction"),
            jsonPath("$.violations.[0].message")
                .value("A jurisdiction parameter key and value is required.")
        );
    }

    @Test
    void should_return_200_with_empty_when_dmn_not_found() throws Exception {

        final List<String> roleNames = singletonList("tribunal-caseworker");
        List<RoleAssignment> allTestRoles = mockServices.createTestRoleAssignments(roleNames);

        when(roleAssignmentServiceApi.getRolesForUser(any(), anyString(), anyString()))
            .thenReturn(new RoleAssignmentResource(allTestRoles));

        mockMvc.perform(
            get(ENDPOINT_PATH + "?jurisdiction=invalid_jurisdiction")
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        ).andExpectAll(
            status().isOk(),
            content().contentType(APPLICATION_JSON_VALUE),
            jsonPath("$").isEmpty()
        );
    }

    @Test
    void should_return_200_with_empty_when_dmn_found_but_empty() throws Exception {

        final List<String> roleNames = singletonList("tribunal-caseworker");
        List<RoleAssignment> allTestRoles = mockServices.createTestRoleAssignments(roleNames);

        when(roleAssignmentServiceApi.getRolesForUser(any(), anyString(), anyString()))
            .thenReturn(new RoleAssignmentResource(allTestRoles));

        mockMvc.perform(
            get(ENDPOINT_PATH + "?jurisdiction=ia")
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        ).andExpectAll(
            status().isOk(),
            content().contentType(APPLICATION_JSON_VALUE),
            jsonPath("$").isEmpty()
        );
    }

    @Test
    void should_return_503_with_application_problem_response_when_camunda_api_is_not_available() throws Exception {
        final List<String> roleNames = singletonList("tribunal-caseworker");
        List<RoleAssignment> allTestRoles = mockServices.createTestRoleAssignments(roleNames);

        when(roleAssignmentServiceApi.getRolesForUser(any(), anyString(), anyString()))
            .thenReturn(new RoleAssignmentResource(allTestRoles));

        doThrow(FeignException.FeignClientException.class)
            .when(camundaServiceApi).getTaskTypesDmnTable(any(), anyString(), anyString());

        mockMvc.perform(
            get(ENDPOINT_PATH + "?jurisdiction=wa")
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        ).andExpectAll(
            status().is5xxServerError(),
            content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
            jsonPath("$.type")
                .value("https://github.com/hmcts/wa-task-management-api/problem/service-unavailable"),
            jsonPath("$.title").value("Service Unavailable"),
            jsonPath("$.status").value(503),
            jsonPath("$.detail").value(
                "Database is unavailable.")
        );
    }
}


