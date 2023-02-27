package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import org.hibernate.exception.JDBCConnectionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootIntegrationBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.Token;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.response.RoleAssignmentResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.RoleAssignmentServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.CompleteTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.options.CompletionOptions;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskRoleResource;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskDatabaseService;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.persistence.EntityManager;

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
    @MockBean
    private CFTTaskDatabaseService cftTaskDatabaseService;
    @Autowired
    private EntityManager entityManager;

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

        TaskResource taskResource = new TaskResource(
            taskId, "taskName", "taskType", CFTTaskState.ASSIGNED
        );

        when(cftTaskDatabaseService.findByIdOnly(taskId)).thenReturn(Optional.of(taskResource));

        taskResource.setTaskRoleResources(taskRoleResourceSet);


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


    @Test
    public void should_return_a_403_when_the_user_did_not_have_any_roles() throws Exception {
        List<RoleAssignment> roles = new ArrayList<>();

        RoleAssignmentResource roleAssignmentResource = new RoleAssignmentResource(roles);

        //Assigner
        when(roleAssignmentServiceApi.getRolesForUser(
            any(), any(), any()
        )).thenReturn(roleAssignmentResource);

        mockMvc.perform(
            get("/task/taskId/roles")
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        ).andExpectAll(
            status().is4xxClientError(),
            content().contentType(APPLICATION_JSON_VALUE),
            jsonPath("$.error").value("Unauthorized"),
            jsonPath("$.status").value(401),
            jsonPath("$.message").value(
                "User did not have sufficient permissions to perform this action"));
    }

    @Test
    public void should_return_a_404_if_task_does_not_exist() throws Exception {
        mockServices.mockUserInfo();
        List<RoleAssignment> roleAssignmentsWithJurisdiction = mockServices.createRoleAssignmentsWithJurisdiction(
            "SCSS", "caseId1");
        // create role assignments Organisation and SCSS , Case Id
        RoleAssignmentResource accessControlResponse = new RoleAssignmentResource(
            roleAssignmentsWithJurisdiction
        );

        when(roleAssignmentServiceApi.getRolesForUser(
            any(), any(), any()
        )).thenReturn(accessControlResponse);

        when(idamWebApi.token(any())).thenReturn(new Token(IDAM_AUTHORIZATION_TOKEN, "scope"));
        when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);

        CompleteTaskRequest request = new CompleteTaskRequest(new CompletionOptions(true));
        String nonExistentTaskId = "00000000-0000-0000-0000-000000000000";

        mockMvc.perform(
            get("/task/" + nonExistentTaskId + "/roles")
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(request))
        ).andExpectAll(
            status().is4xxClientError(),
            content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
            jsonPath("$.type").value("https://github.com/hmcts/wa-task-management-api/problem/task-not-found-error"),
            jsonPath("$.title").value("Task Not Found Error"),
            jsonPath("$.status").value(404),
            jsonPath("$.detail").value(
                "Task Not Found Error: The task could not be found.")
        );
    }
}
