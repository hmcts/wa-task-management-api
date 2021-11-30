package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import org.hibernate.exception.JDBCConnectionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootIntegrationBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.response.RoleAssignmentResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskRoleResource;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.RoleAssignmentServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskDatabaseService;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;
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

class GetTaskRolePermissionsControllerTest extends SpringBootIntegrationBaseTest {

    @MockBean
    private IdamWebApi idamWebApi;
    @MockBean
    private AuthTokenGenerator serviceAuthTokenGenerator;
    @MockBean
    private ServiceAuthorisationApi serviceAuthorisationApi;
    @MockBean
    private CamundaServiceApi camundaServiceApi;
    @MockBean
    private RoleAssignmentServiceApi roleAssignmentServiceApi;
    @SpyBean
    private CFTTaskDatabaseService cftTaskDatabaseService;

    private UserInfo mockedUserInfo;
    private ServiceMocks mockServices;

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
    void should_return_200_with_empty_list_when_task_does_not_have_any_roles()
        throws Exception {

        String taskId = UUID.randomUUID().toString();
        final List<String> roleNames = singletonList("tribunal-caseworker");

        List<RoleAssignment> allTestRoles =
            mockServices.createTestRoleAssignments(roleNames);

        when(roleAssignmentServiceApi.getRolesForUser(any(), anyString(), anyString()))
            .thenReturn(new RoleAssignmentResource(allTestRoles));

        final TaskResource taskResource = spy(TaskResource.class);
        when(cftTaskDatabaseService.findByIdOnly(taskId)).thenReturn(Optional.of(taskResource));
        when(Optional.of(taskResource).get().getTaskRoleResources()).thenReturn(emptySet());

        mockMvc.perform(
            get("/task/" + taskId + "/roles")
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(
                ResultMatcher.matchAll(
                    status().isOk(),
                    content().contentType(APPLICATION_JSON_VALUE),
                    jsonPath("$.roles").isEmpty()
                ));
    }

    @Test
    void should_return_503_with_application_problem_response_when_db_is_not_available() throws Exception {
        String taskId = UUID.randomUUID().toString();
        final List<String> roleNames = singletonList("tribunal-caseworker");

        List<RoleAssignment> allTestRoles =
            mockServices.createTestRoleAssignments(roleNames);

        when(roleAssignmentServiceApi.getRolesForUser(any(), anyString(), anyString()))
            .thenReturn(new RoleAssignmentResource(allTestRoles));

        doThrow(JDBCConnectionException.class)
            .when(cftTaskDatabaseService).findByIdOnly(taskId);

        mockMvc.perform(
            get("/task/" + taskId + "/roles")
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        ).andExpect(
            ResultMatcher.matchAll(
                status().is5xxServerError(),
                content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
                jsonPath("$.type")
                    .value("https://github.com/hmcts/wa-task-management-api/problem/service-unavailable"),
                jsonPath("$.title").value("Service Unavailable"),
                jsonPath("$.status").value(503),
                jsonPath("$.detail").value(
                    "Database is unavailable.")
            ));
    }

    @Test
    void should_return_403_with_role_assignment_verification_problem_when_task_does_not_have_required_permissions()
        throws Exception {
        String taskId = UUID.randomUUID().toString();
        final List<String> roleNames = singletonList("tribunal-caseworker");

        List<RoleAssignment> allTestRoles =
            mockServices.createTestRoleAssignments(roleNames);

        when(roleAssignmentServiceApi.getRolesForUser(any(), anyString(), anyString()))
            .thenReturn(new RoleAssignmentResource(allTestRoles));

        TaskRoleResource taskRoleResource = new TaskRoleResource(
            "roleName", false, false, false, false, false,
            false, new String[]{}, 1, false, "roleCategory"
        );

        Set<TaskRoleResource> taskRoleResourceSet = Set.of(taskRoleResource);

        final TaskResource taskResource = spy(TaskResource.class);
        when(cftTaskDatabaseService.findByIdOnly(taskId)).thenReturn(Optional.of(taskResource));
        when(Optional.of(taskResource).get().getTaskRoleResources()).thenReturn(taskRoleResourceSet);
        when(cftTaskDatabaseService.findByIdOnly(taskId)).thenReturn(Optional.of(taskResource));

        when(cftTaskDatabaseService.findTaskBySpecification(any())).thenReturn(Optional.empty());

        mockMvc.perform(
            get("/task/" + taskId + "/roles")
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(
                ResultMatcher.matchAll(
                    status().isForbidden(),
                    content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
                    jsonPath("$.type").value(
                        "https://github.com/hmcts/wa-task-management-api/problem/role-assignment-verification-failure"),
                    jsonPath("$.title").value("Role Assignment Verification"),
                    jsonPath("$.status").value(403),
                    jsonPath("$.detail").value(
                        "Role Assignment Verification: "
                        + "The request failed the Role Assignment checks performed.")
                ));
    }
}
