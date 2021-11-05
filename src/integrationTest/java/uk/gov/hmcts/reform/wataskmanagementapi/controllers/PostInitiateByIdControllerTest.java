package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootIntegrationBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.restrict.ClientAccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.ActorIdType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleCategory;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.response.RoleAssignmentResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.repository.TaskResourceRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.RoleAssignmentServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.InitiateTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskAttribute;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.clients.CcdDataServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.response.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.response.PermissionsDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.ccd.CaseDetails;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.SERVICE_AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.InitiateTaskOperation.INITIATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_ASSIGNEE;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_DUE_DATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_NAME;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTime.CAMUNDA_DATA_TIME_FORMATTER;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue.stringValue;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.IDAM_AUTHORIZATION_TOKEN;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.IDAM_USER_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.SERVICE_AUTHORIZATION_TOKEN;

class PostInitiateByIdControllerTest extends SpringBootIntegrationBaseTest {
    private static final String ENDPOINT_PATH = "/task/%s";
    private static String ENDPOINT_BEING_TESTED;
    @MockBean
    private ClientAccessControlService clientAccessControlService;
    @Autowired
    private TaskResourceRepository taskResourceRepository;
    private ServiceMocks mockServices;
    @MockBean
    private IdamWebApi idamWebApi;
    @MockBean
    private CamundaServiceApi camundaServiceApi;
    @MockBean
    private uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.clients.CamundaServiceApi camundaTaskConfig;
    @MockBean
    private CcdDataServiceApi ccdDataServiceApi;
    @MockBean
    private AuthTokenGenerator authTokenGenerator;
    @MockBean
    private RoleAssignmentServiceApi roleAssignmentServiceApi;
    @MockBean
    private ServiceAuthorisationApi serviceAuthorisationApi;
    @Mock
    private CaseDetails caseDetails;
    private String taskId;

    @BeforeEach
    void setUp() {
        taskId = UUID.randomUUID().toString();
        ENDPOINT_BEING_TESTED = String.format(ENDPOINT_PATH, taskId);

        mockServices = new ServiceMocks(
            idamWebApi,
            serviceAuthorisationApi,
            camundaServiceApi,
            roleAssignmentServiceApi
        );

        mockServices.mockServiceAPIs();
    }

    @AfterAll
    void tearDown() {
        taskResourceRepository.deleteAll();
    }

    @Test
    void should_return_403_with_application_problem_response_when_client_is_not_allowed() throws Exception {

        when(clientAccessControlService.hasExclusiveAccess(SERVICE_AUTHORIZATION_TOKEN))
            .thenReturn(false);

        ZonedDateTime createdDate = ZonedDateTime.now();
        ZonedDateTime dueDate = createdDate.plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);

        InitiateTaskRequest req = new InitiateTaskRequest(INITIATION, asList(
            new TaskAttribute(TASK_TYPE, "followUpOverdueReasonsForAppeal"),
            new TaskAttribute(TASK_NAME, "follow Up Overdue Reasons For Appeal"),
            new TaskAttribute(TASK_DUE_DATE, formattedDueDate)

        ));

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(req))
        ).andExpect(
            ResultMatcher.matchAll(
                status().isForbidden(),
                content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
                jsonPath("$.type")
                    .value("https://github.com/hmcts/wa-task-management-api/problem/forbidden"),
                jsonPath("$.title").value("Forbidden"),
                jsonPath("$.status").value(403),
                jsonPath("$.detail").value(
                    "Forbidden: The action could not be completed because the client/user "
                    + "had insufficient rights to a resource.")
            ));
    }

    @Test
    void given_task_is_locked_when_other_transactions_then_cannot_make_changes() throws Exception {
        when(clientAccessControlService.hasExclusiveAccess(SERVICE_AUTHORIZATION_TOKEN))
            .thenReturn(true);

        when(caseDetails.getCaseType()).thenReturn("Asylum");
        when(caseDetails.getJurisdiction()).thenReturn("IA");
        when(caseDetails.getSecurityClassification()).thenReturn(("PUBLIC"));

        when(ccdDataServiceApi.getCase(any(), any(), eq("someCaseId")))
            .thenReturn(caseDetails);

        when(camundaTaskConfig.evaluateConfigurationDmnTable(any(), any(), any(), any()))
            .thenReturn(asList(
                new ConfigurationDmnEvaluationResponse(stringValue("caseName"), stringValue("someName")),
                new ConfigurationDmnEvaluationResponse(stringValue("appealType"), stringValue("protection")),
                new ConfigurationDmnEvaluationResponse(stringValue("region"), stringValue("1")),
                new ConfigurationDmnEvaluationResponse(stringValue("location"), stringValue("765324")),
                new ConfigurationDmnEvaluationResponse(stringValue("locationName"), stringValue("Taylor House")),
                new ConfigurationDmnEvaluationResponse(stringValue("workType"), stringValue("decision_making_work")),
                new ConfigurationDmnEvaluationResponse(stringValue("caseManagementCategory"), stringValue("Protection"))
            ));

        when(camundaTaskConfig.evaluatePermissionsDmnTable(any(), any(), any(), any()))
            .thenReturn(asList(
                new PermissionsDmnEvaluationResponse(
                    stringValue("tribunal-caseworker"),
                    stringValue("Read,Refer,Own,Manage,Cancel"),
                    stringValue("IA,WA"),
                    null,
                    null,
                    null
                ),
                new PermissionsDmnEvaluationResponse(
                    stringValue("senior-tribunal-caseworker"),
                    stringValue("Read,Refer,Own,Manage,Cancel"),
                    null,
                    null,
                    null,
                    null
                )
            ));

        when(roleAssignmentServiceApi.queryRoleAssignments(any(), any(), any()))
            .thenReturn(new RoleAssignmentResource(Collections.emptyList()));

        ExecutorService executorService = new ScheduledThreadPoolExecutor(2);
        executorService.execute(() -> {
            try {
                ZonedDateTime createdDate = ZonedDateTime.now();
                ZonedDateTime dueDate = createdDate.plusDays(1);
                String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);

                InitiateTaskRequest req = new InitiateTaskRequest(INITIATION, asList(
                    new TaskAttribute(TASK_TYPE, "followUpOverdueReasonsForAppeal"),
                    new TaskAttribute(TASK_NAME, "follow Up Overdue Reasons For Appeal"),
                    new TaskAttribute(TASK_CASE_ID, "someCaseId"),
                    new TaskAttribute(TASK_DUE_DATE, formattedDueDate)

                ));
                mockMvc.perform(
                    post(ENDPOINT_BEING_TESTED)
                        .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                        .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(asJsonString(req))
                ).andExpect(
                    ResultMatcher.matchAll(
                        status().isCreated(),
                        content().contentType(APPLICATION_JSON_VALUE)
                    ));
            } catch (Exception e) {
                fail();
            }
        });

        TimeUnit.SECONDS.sleep(1); // to ensure second call does not start before first call above

        ZonedDateTime createdDate = ZonedDateTime.now();
        ZonedDateTime dueDate = createdDate.plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);
        InitiateTaskRequest someOtherReq = new InitiateTaskRequest(INITIATION, asList(
            new TaskAttribute(TASK_TYPE, "markCaseAsPaid"),
            new TaskAttribute(TASK_NAME, "soe other task name"),
            new TaskAttribute(TASK_CASE_ID, "some other task case id"),
            new TaskAttribute(TASK_DUE_DATE, formattedDueDate)

        ));

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(someOtherReq))
        ).andExpect(
            ResultMatcher.matchAll(
                status().isServiceUnavailable(),
                content().contentType(APPLICATION_PROBLEM_JSON_VALUE)
            ));

        Optional<TaskResource> actualTask = taskResourceRepository.getByTaskId(taskId);
        assertTrue(actualTask.isPresent());
        assertEquals("followUpOverdueReasonsForAppeal", actualTask.get().getTaskType());
    }

    @Test
    void given_initiate_request_when_there_is_error_then_do_rollback() throws Exception {
        when(clientAccessControlService.hasExclusiveAccess(SERVICE_AUTHORIZATION_TOKEN))
            .thenReturn(true);

        when(caseDetails.getCaseType()).thenReturn("Asylum");
        when(caseDetails.getJurisdiction()).thenReturn("IA");
        when(caseDetails.getSecurityClassification()).thenReturn(("PUBLIC"));

        when(ccdDataServiceApi.getCase(any(), any(), eq("someCaseId")))
            .thenThrow(new RuntimeException("some error"));

        ZonedDateTime createdDate = ZonedDateTime.now();
        ZonedDateTime dueDate = createdDate.plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);

        InitiateTaskRequest req = new InitiateTaskRequest(INITIATION, asList(
            new TaskAttribute(TASK_TYPE, "followUpOverdueReasonsForAppeal"),
            new TaskAttribute(TASK_NAME, "follow Up Overdue Reasons For Appeal"),
            new TaskAttribute(TASK_CASE_ID, "someCaseId"),
            new TaskAttribute(TASK_DUE_DATE, formattedDueDate)
        ));

        mockMvc.perform(
                post(ENDPOINT_BEING_TESTED)
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .contentType(APPLICATION_JSON_VALUE)
                    .content(asJsonString(req))
            )
            .andDo(print())
            .andExpect(ResultMatcher.matchAll(status().isInternalServerError()));

        assertFalse(taskResourceRepository.getByTaskId(taskId).isPresent());
    }

    @Test
    void should_return_201_with_task_unassigned() throws Exception {
        when(clientAccessControlService.hasExclusiveAccess(SERVICE_AUTHORIZATION_TOKEN))
            .thenReturn(true);

        when(caseDetails.getCaseType()).thenReturn("Asylum");
        when(caseDetails.getJurisdiction()).thenReturn("IA");
        when(caseDetails.getSecurityClassification()).thenReturn(("PUBLIC"));

        when(ccdDataServiceApi.getCase(any(), any(), eq("someCaseId")))
            .thenReturn(caseDetails);

        when(camundaTaskConfig.evaluateConfigurationDmnTable(any(), any(), any(), any()))
            .thenReturn(asList(
                new ConfigurationDmnEvaluationResponse(stringValue("caseName"), stringValue("someName")),
                new ConfigurationDmnEvaluationResponse(stringValue("appealType"), stringValue("protection")),
                new ConfigurationDmnEvaluationResponse(stringValue("region"), stringValue("1")),
                new ConfigurationDmnEvaluationResponse(stringValue("location"), stringValue("765324")),
                new ConfigurationDmnEvaluationResponse(stringValue("locationName"), stringValue("Taylor House")),
                new ConfigurationDmnEvaluationResponse(stringValue("workType"), stringValue("decision_making_work")),
                new ConfigurationDmnEvaluationResponse(stringValue("caseManagementCategory"), stringValue("Protection"))
            ));

        when(camundaTaskConfig.evaluatePermissionsDmnTable(any(), any(), any(), any()))
            .thenReturn(asList(
                new PermissionsDmnEvaluationResponse(
                    stringValue("tribunal-caseworker"),
                    stringValue("Read,Refer,Own,Manage,Cancel"),
                    stringValue("IA,WA"),
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
            ));

        when(roleAssignmentServiceApi.queryRoleAssignments(any(), any(), any()))
            .thenReturn(new RoleAssignmentResource(Collections.emptyList()));

        ZonedDateTime createdDate = ZonedDateTime.now();
        ZonedDateTime dueDate = createdDate.plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);

        InitiateTaskRequest req = new InitiateTaskRequest(INITIATION, asList(
            new TaskAttribute(TASK_TYPE, "followUpOverdueReasonsForAppeal"),
            new TaskAttribute(TASK_NAME, "follow Up Overdue Reasons For Appeal"),
            new TaskAttribute(TASK_CASE_ID, "someCaseId"),
            new TaskAttribute(TASK_DUE_DATE, formattedDueDate)
        ));

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(req))
        ).andExpect(
            ResultMatcher.matchAll(
                status().isCreated(),
                content().contentType(APPLICATION_JSON_VALUE),
                jsonPath("$.task_id").value(taskId),
                jsonPath("$.task_name").value("follow Up Overdue Reasons For Appeal"),
                jsonPath("$.task_type").value("followUpOverdueReasonsForAppeal"),
                jsonPath("$.state").value("UNASSIGNED"),
                jsonPath("$.task_system").value("SELF"),
                jsonPath("$.security_classification").value("PUBLIC"),
                jsonPath("$.title").value("follow Up Overdue Reasons For Appeal"),
                jsonPath("$.auto_assigned").value(false),
                jsonPath("$.has_warnings").value("false"),
                jsonPath("$.case_id").value("someCaseId"),
                jsonPath("$.case_type_id").value("Asylum"),
                jsonPath("$.case_name").value("someName"),
                jsonPath("$.case_category").value("Protection"),
                jsonPath("$.jurisdiction").value("IA"),
                jsonPath("$.region").value("1"),
                jsonPath("$.location").value("765324"),
                jsonPath("$.location_name").value("Taylor House"),
                jsonPath("$.execution_type_code.execution_code").value("CASE_EVENT"),
                jsonPath("$.execution_type_code.execution_name").value("Case Management Task"),
                jsonPath("$.execution_type_code.description").value(
                    "The task requires a case management event to be executed by the user. "
                    + "(Typically this will be in CCD.)"),
                jsonPath("$.task_role_resources.[0].task_id").value(taskId),
                jsonPath("$.task_role_resources.[0].role_name")
                    .value(anyOf(is("tribunal-caseworker"), is("senior-tribunal-caseworker"))),
                jsonPath("$.task_role_resources.[0].read").value(true),
                jsonPath("$.task_role_resources.[0].own").value(true),
                jsonPath("$.task_role_resources.[0].execute").value(false),
                jsonPath("$.task_role_resources.[0].manage").value(true),
                jsonPath("$.task_role_resources.[0].cancel").value(true),
                jsonPath("$.task_role_resources.[0].refer").value(true),
                jsonPath("$.task_role_resources.[0].auto_assignable").value(false),
                jsonPath("$.task_role_resources.[1].task_id").value(taskId),
                jsonPath("$.task_role_resources.[1].role_name")
                    .value(anyOf(is("tribunal-caseworker"), is("senior-tribunal-caseworker"))),
                jsonPath("$.task_role_resources.[1].read").value(true),
                jsonPath("$.task_role_resources.[1].own").value(true),
                jsonPath("$.task_role_resources.[1].execute").value(false),
                jsonPath("$.task_role_resources.[1].manage").value(true),
                jsonPath("$.task_role_resources.[1].cancel").value(true),
                jsonPath("$.task_role_resources.[1].refer").value(true),
                jsonPath("$.task_role_resources.[1].auto_assignable").value(false)
            ));

    }


    @Test
    void should_return_201_with_task_assigned() throws Exception {

        when(clientAccessControlService.hasExclusiveAccess(SERVICE_AUTHORIZATION_TOKEN))
            .thenReturn(true);

        when(caseDetails.getCaseType()).thenReturn("Asylum");
        when(caseDetails.getJurisdiction()).thenReturn("IA");
        when(caseDetails.getSecurityClassification()).thenReturn(("PUBLIC"));

        when(ccdDataServiceApi.getCase(any(), any(), eq("someCaseId")))
            .thenReturn(caseDetails);

        when(camundaTaskConfig.evaluateConfigurationDmnTable(any(), any(), any(), any()))
            .thenReturn(asList(
                new ConfigurationDmnEvaluationResponse(stringValue("caseName"), stringValue("someName")),
                new ConfigurationDmnEvaluationResponse(stringValue("appealType"), stringValue("protection")),
                new ConfigurationDmnEvaluationResponse(stringValue("region"), stringValue("1")),
                new ConfigurationDmnEvaluationResponse(stringValue("location"), stringValue("765324")),
                new ConfigurationDmnEvaluationResponse(stringValue("locationName"), stringValue("Taylor House")),
                new ConfigurationDmnEvaluationResponse(stringValue("workType"), stringValue("decision_making_work")),
                new ConfigurationDmnEvaluationResponse(stringValue("caseManagementCategory"), stringValue("Protection"))
            ));

        when(camundaTaskConfig.evaluatePermissionsDmnTable(any(), any(), any(), any()))
            .thenReturn(asList(
                new PermissionsDmnEvaluationResponse(
                    stringValue("tribunal-caseworker"),
                    stringValue("Read,Refer,Own,Manage,Cancel"),
                    stringValue("IA,WA"),
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
            ));


        when(roleAssignmentServiceApi.queryRoleAssignments(any(), any(), any()))
            .thenReturn(new RoleAssignmentResource(
                Collections.singletonList(RoleAssignment.builder()
                    .id("someId")
                    .actorIdType(ActorIdType.IDAM)
                    .actorId(IDAM_USER_ID)
                    .roleName("tribunal-caseworker")
                    .roleCategory(RoleCategory.LEGAL_OPERATIONS)
                    .grantType(GrantType.SPECIFIC)
                    .roleType(RoleType.ORGANISATION)
                    .classification(Classification.PUBLIC)
                    .authorisations(asList("IA"))
                    .build())));
        ZonedDateTime createdDate = ZonedDateTime.now();
        ZonedDateTime dueDate = createdDate.plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);

        InitiateTaskRequest req = new InitiateTaskRequest(INITIATION, asList(
            new TaskAttribute(TASK_TYPE, "followUpOverdueReasonsForAppeal"),
            new TaskAttribute(TASK_ASSIGNEE, "someAssignee"),
            new TaskAttribute(TASK_NAME, "aTaskName"),
            new TaskAttribute(TASK_CASE_ID, "someCaseId"),
            new TaskAttribute(TASK_DUE_DATE, formattedDueDate)
        ));

        mockMvc.perform(
                post(ENDPOINT_BEING_TESTED)
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(asJsonString(req))
            ).andDo(MockMvcResultHandlers.print())
            .andExpect(
                ResultMatcher.matchAll(
                    status().isCreated(),
                    content().contentType(APPLICATION_JSON_VALUE),
                    jsonPath("$.task_id").value(taskId),
                    jsonPath("$.task_name").value("aTaskName"),
                    jsonPath("$.task_type").value("followUpOverdueReasonsForAppeal"),
                    jsonPath("$.state").value("ASSIGNED"),
                    jsonPath("$.task_system").value("SELF"),
                    jsonPath("$.security_classification").value("PUBLIC"),
                    jsonPath("$.title").value("aTaskName"),
                    jsonPath("$.auto_assigned").value(false),
                    jsonPath("$.has_warnings").value("false"),
                    jsonPath("$.case_id").value("someCaseId"),
                    jsonPath("$.case_type_id").value("Asylum"),
                    jsonPath("$.case_name").value("someName"),
                    jsonPath("$.case_category").value("Protection"),
                    jsonPath("$.jurisdiction").value("IA"),
                    jsonPath("$.region").value("1"),
                    jsonPath("$.location").value("765324"),
                    jsonPath("$.location_name").value("Taylor House"),
                    jsonPath("$.execution_type_code.execution_code").value("CASE_EVENT"),
                    jsonPath("$.execution_type_code.execution_name")
                        .value("Case Management Task"),
                    jsonPath("$.execution_type_code.description").value(
                        "The task requires a case management event to be executed by the user. "
                        + "(Typically this will be in CCD.)"),
                    jsonPath("$.task_role_resources.[0].task_id").value(taskId),
                    jsonPath("$.task_role_resources.[0].role_name")
                        .value(anyOf(is("tribunal-caseworker"), is("senior-tribunal-caseworker"))),
                    jsonPath("$.task_role_resources.[0].read").value(true),
                    jsonPath("$.task_role_resources.[0].own").value(true),
                    jsonPath("$.task_role_resources.[0].execute").value(false),
                    jsonPath("$.task_role_resources.[0].manage").value(true),
                    jsonPath("$.task_role_resources.[0].cancel").value(true),
                    jsonPath("$.task_role_resources.[0].refer").value(true),
                    jsonPath("$.task_role_resources.[0].auto_assignable").value(false),
                    jsonPath("$.task_role_resources.[1].task_id").value(taskId),
                    jsonPath("$.task_role_resources.[1].role_name")
                        .value(anyOf(is("tribunal-caseworker"), is("senior-tribunal-caseworker"))),
                    jsonPath("$.task_role_resources.[1].read").value(true),
                    jsonPath("$.task_role_resources.[1].own").value(true),
                    jsonPath("$.task_role_resources.[1].execute").value(false),
                    jsonPath("$.task_role_resources.[1].manage").value(true),
                    jsonPath("$.task_role_resources.[1].cancel").value(true),
                    jsonPath("$.task_role_resources.[1].refer").value(true),
                    jsonPath("$.task_role_resources.[1].auto_assignable").value(false)
                ));
    }
}

