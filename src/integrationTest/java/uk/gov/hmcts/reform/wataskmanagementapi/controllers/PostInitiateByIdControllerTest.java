package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
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
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskRoleResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.repository.TaskResourceRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CcdDataServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.RoleAssignmentServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.config.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.InitiateTaskRequestAttributes;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskAttribute;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.PermissionsDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.SecurityClassification;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.ccd.CaseDetails;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.enums.TestRolesWithGrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskDatabaseService;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
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
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.UNASSIGNED;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.SERVICE_AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.InitiateTaskOperation.INITIATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_ASSIGNEE;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_DUE_DATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_NAME;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_STATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTime.CAMUNDA_DATA_TIME_FORMATTER;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue.booleanValue;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue.integerValue;
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
    @Autowired
    private CFTTaskDatabaseService cftTaskDatabaseService;
    private ServiceMocks mockServices;
    @MockBean
    private IdamWebApi idamWebApi;
    @MockBean
    private CamundaServiceApi camundaServiceApi;
    @MockBean
    private CcdDataServiceApi ccdDataServiceApi;
    @MockBean
    private AuthTokenGenerator authTokenGenerator;
    @MockBean
    private RoleAssignmentServiceApi roleAssignmentServiceApi;
    @MockBean
    private ServiceAuthorisationApi serviceAuthorisationApi;
    @MockBean
    private LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider;
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

        when(launchDarklyFeatureFlagProvider.getBooleanValue(any(), any(), any())).thenReturn(false);

        ZonedDateTime createdDate = ZonedDateTime.now();
        ZonedDateTime dueDate = createdDate.plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);

        InitiateTaskRequestAttributes req = new InitiateTaskRequestAttributes(INITIATION, asList(
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
        ).andExpectAll(
            status().isForbidden(),
            content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
            jsonPath("$.type")
                .value("https://github.com/hmcts/wa-task-management-api/problem/forbidden"),
            jsonPath("$.title").value("Forbidden"),
            jsonPath("$.status").value(403),
            jsonPath("$.detail").value(
                "Forbidden: The action could not be completed because the client/user "
                + "had insufficient rights to a resource.")
        );
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

        when(camundaServiceApi.evaluateConfigurationDmnTable(any(), any(), any(), any()))
            .thenReturn(asList(
                new ConfigurationDmnEvaluationResponse(stringValue("caseName"), stringValue("someName")),
                new ConfigurationDmnEvaluationResponse(stringValue("appealType"), stringValue("protection")),
                new ConfigurationDmnEvaluationResponse(stringValue("region"), stringValue("1")),
                new ConfigurationDmnEvaluationResponse(stringValue("location"), stringValue("765324")),
                new ConfigurationDmnEvaluationResponse(stringValue("locationName"), stringValue("Taylor House")),
                new ConfigurationDmnEvaluationResponse(stringValue("workType"), stringValue("decision_making_work")),
                new ConfigurationDmnEvaluationResponse(stringValue("caseManagementCategory"), stringValue("Protection"))
            ));

        when(camundaServiceApi.evaluatePermissionsDmnTable(any(), any(), any(), any()))
            .thenReturn(asList(
                new PermissionsDmnEvaluationResponse(
                    stringValue("tribunal-caseworker"),
                    stringValue("Read,Refer,Own"),
                    stringValue("IA,WA"),
                    null,
                    null,
                    null,
                    stringValue(null)
                ),
                new PermissionsDmnEvaluationResponse(
                    stringValue("senior-tribunal-caseworker"),
                    stringValue("Read,Refer,Own"),
                    null,
                    null,
                    null,
                    null,
                    stringValue(null)
                )
            ));

        when(roleAssignmentServiceApi.queryRoleAssignments(any(), any(), any()))
            .thenReturn(new RoleAssignmentResource(emptyList()));

        ExecutorService executorService = new ScheduledThreadPoolExecutor(2);
        executorService.execute(() -> {
            try {
                ZonedDateTime createdDate = ZonedDateTime.now();
                ZonedDateTime dueDate = createdDate.plusDays(1);
                String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);

                InitiateTaskRequestAttributes req = new InitiateTaskRequestAttributes(INITIATION, asList(
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
                ).andExpectAll(
                    status().isCreated(),
                    content().contentType(APPLICATION_JSON_VALUE)
                );
            } catch (Exception e) {
                fail();
            }
        });

        TimeUnit.SECONDS.sleep(1); // to ensure second call does not start before first call above

        ZonedDateTime createdDate = ZonedDateTime.now();
        ZonedDateTime dueDate = createdDate.plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);
        InitiateTaskRequestAttributes someOtherReq = new InitiateTaskRequestAttributes(INITIATION, asList(
            new TaskAttribute(TASK_TYPE, "markCaseAsPaid"),
            new TaskAttribute(TASK_NAME, "soe other task name"),
            new TaskAttribute(TASK_CASE_ID, "some other task case id"),
            new TaskAttribute(TASK_DUE_DATE, formattedDueDate)

        ));

        mockMvc
            .perform(post(ENDPOINT_BEING_TESTED)
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(someOtherReq)))
            .andExpectAll(
                status().isServiceUnavailable(),
                content().contentType(APPLICATION_PROBLEM_JSON_VALUE)
            );

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

        InitiateTaskRequestAttributes req = new InitiateTaskRequestAttributes(INITIATION, asList(
            new TaskAttribute(TASK_TYPE, "followUpOverdueReasonsForAppeal"),
            new TaskAttribute(TASK_NAME, "follow Up Overdue Reasons For Appeal"),
            new TaskAttribute(TASK_CASE_ID, "someCaseId"),
            new TaskAttribute(TASK_DUE_DATE, formattedDueDate)
        ));

        mockMvc
            .perform(post(ENDPOINT_BEING_TESTED)
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(APPLICATION_JSON_VALUE)
                .content(asJsonString(req)))
            .andDo(print())
            .andExpectAll(status().isInternalServerError());

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

        when(camundaServiceApi.evaluateConfigurationDmnTable(any(), any(), any(), any()))
            .thenReturn(asList(
                new ConfigurationDmnEvaluationResponse(stringValue("caseName"), stringValue("someName")),
                new ConfigurationDmnEvaluationResponse(stringValue("appealType"), stringValue("protection")),
                new ConfigurationDmnEvaluationResponse(stringValue("region"), stringValue("1")),
                new ConfigurationDmnEvaluationResponse(stringValue("location"), stringValue("765324")),
                new ConfigurationDmnEvaluationResponse(stringValue("locationName"), stringValue("Taylor House")),
                new ConfigurationDmnEvaluationResponse(stringValue("workType"), stringValue("decision_making_work")),
                new ConfigurationDmnEvaluationResponse(stringValue("caseManagementCategory"), stringValue("Protection"))
            ));

        when(camundaServiceApi.evaluatePermissionsDmnTable(any(), any(), any(), any()))
            .thenReturn(asList(
                new PermissionsDmnEvaluationResponse(
                    stringValue("tribunal-caseworker"),
                    stringValue("Read,Refer,Own"),
                    stringValue("IA,WA"),
                    null,
                    null,
                    stringValue("LEGAL_OPERATIONS"),
                    stringValue(null)
                ),
                new PermissionsDmnEvaluationResponse(
                    stringValue("senior-tribunal-caseworker"),
                    stringValue("Read,Refer,Own"),
                    null,
                    null,
                    null,
                    stringValue("LEGAL_OPERATIONS"),
                    stringValue(null)
                )
            ));

        when(roleAssignmentServiceApi.queryRoleAssignments(any(), any(), any()))
            .thenReturn(new RoleAssignmentResource(emptyList()));

        ZonedDateTime createdDate = ZonedDateTime.now();
        ZonedDateTime dueDate = createdDate.plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);

        InitiateTaskRequestAttributes req = new InitiateTaskRequestAttributes(INITIATION, asList(
            new TaskAttribute(TASK_TYPE, "followUpOverdueReasonsForAppeal"),
            new TaskAttribute(TASK_NAME, "follow Up Overdue Reasons For Appeal"),
            new TaskAttribute(TASK_CASE_ID, "someCaseId"),
            new TaskAttribute(TASK_DUE_DATE, formattedDueDate)
        ));

        mockMvc
            .perform(post(ENDPOINT_BEING_TESTED)
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(req)))
            .andExpectAll(
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
                jsonPath("$.task_role_resources.[0].manage").value(false),
                jsonPath("$.task_role_resources.[0].cancel").value(false),
                jsonPath("$.task_role_resources.[0].auto_assignable").value(false),
                jsonPath("$.task_role_resources.[1].task_id").value(taskId),
                jsonPath("$.task_role_resources.[1].role_name")
                    .value(anyOf(is("tribunal-caseworker"), is("senior-tribunal-caseworker"))),
                jsonPath("$.task_role_resources.[1].read").value(true),
                jsonPath("$.task_role_resources.[1].own").value(true),
                jsonPath("$.task_role_resources.[1].execute").value(false),
                jsonPath("$.task_role_resources.[1].manage").value(false),
                jsonPath("$.task_role_resources.[1].cancel").value(false),
                jsonPath("$.task_role_resources.[1].auto_assignable").value(false)
            );

    }

    @Test
    void should_set_task_assigned_when_permission_is_auto_assigned_with_authorisations_match() throws Exception {

        when(clientAccessControlService.hasExclusiveAccess(SERVICE_AUTHORIZATION_TOKEN))
            .thenReturn(true);

        when(caseDetails.getCaseType()).thenReturn("Asylum");
        when(caseDetails.getJurisdiction()).thenReturn("IA");
        when(caseDetails.getSecurityClassification()).thenReturn(("PUBLIC"));

        when(ccdDataServiceApi.getCase(any(), any(), eq("someCaseId")))
            .thenReturn(caseDetails);

        when(camundaServiceApi.evaluateConfigurationDmnTable(any(), any(), any(), any()))
            .thenReturn(asList(
                new ConfigurationDmnEvaluationResponse(stringValue("caseName"), stringValue("someName")),
                new ConfigurationDmnEvaluationResponse(stringValue("appealType"), stringValue("protection")),
                new ConfigurationDmnEvaluationResponse(stringValue("region"), stringValue("1")),
                new ConfigurationDmnEvaluationResponse(stringValue("location"), stringValue("765324")),
                new ConfigurationDmnEvaluationResponse(stringValue("locationName"), stringValue("Taylor House")),
                new ConfigurationDmnEvaluationResponse(stringValue("workType"), stringValue("decision_making_work")),
                new ConfigurationDmnEvaluationResponse(stringValue("caseManagementCategory"), stringValue("Protection"))
            ));

        when(camundaServiceApi.evaluatePermissionsDmnTable(any(), any(), any(), any()))
            .thenReturn(List.of(
                new PermissionsDmnEvaluationResponse(
                    stringValue("hearing-judge"),
                    stringValue("Read,Refer,Own"),
                    stringValue("IA,WA"),
                    integerValue(1),
                    booleanValue(true),
                    stringValue("LEGAL_OPERATIONS"),
                    stringValue(null)
                ),
                new PermissionsDmnEvaluationResponse(
                    stringValue("judge"),
                    stringValue("Read,Refer,Own"),
                    stringValue("IA,WA"),
                    integerValue(1),
                    booleanValue(false),
                    stringValue("LEGAL_OPERATIONS"),
                    stringValue(null)
                )
            ));


        when(roleAssignmentServiceApi.queryRoleAssignments(any(), any(), any()))
            .thenReturn(new RoleAssignmentResource(
                singletonList(RoleAssignment.builder()
                                  .id("someId")
                                  .actorIdType(ActorIdType.IDAM)
                                  .actorId(IDAM_USER_ID)
                                  .roleName("hearing-judge")
                                  .roleCategory(RoleCategory.LEGAL_OPERATIONS)
                                  .grantType(GrantType.SPECIFIC)
                                  .roleType(RoleType.ORGANISATION)
                                  .classification(Classification.PUBLIC)
                                  .authorisations(List.of("IA"))
                                  .build())));
        ZonedDateTime createdDate = ZonedDateTime.now();
        ZonedDateTime dueDate = createdDate.plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);

        InitiateTaskRequestAttributes req = new InitiateTaskRequestAttributes(INITIATION, asList(
            new TaskAttribute(TASK_TYPE, "followUpOverdueReasonsForAppeal"),
            new TaskAttribute(TASK_NAME, "aTaskName"),
            new TaskAttribute(TASK_CASE_ID, "someCaseId"),
            new TaskAttribute(TASK_DUE_DATE, formattedDueDate)
        ));

        mockMvc
            .perform(post(ENDPOINT_BEING_TESTED)
                         .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                         .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                         .contentType(MediaType.APPLICATION_JSON_VALUE)
                         .content(asJsonString(req)))
            .andDo(MockMvcResultHandlers.print())
            .andExpectAll(
                status().isCreated(),
                content().contentType(APPLICATION_JSON_VALUE),
                jsonPath("$.task_id").value(taskId),
                jsonPath("$.task_name").value("aTaskName"),
                jsonPath("$.task_type").value("followUpOverdueReasonsForAppeal"),
                jsonPath("$.state").value("ASSIGNED"),
                jsonPath("$.assignee").value(IDAM_USER_ID),
                jsonPath("$.task_system").value("SELF"),
                jsonPath("$.security_classification").value("PUBLIC"),
                jsonPath("$.title").value("aTaskName"),
                jsonPath("$.auto_assigned").value(true),
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
                        + "(Typically this will be in CCD.)")
            );
    }

    @Test
    void should_set_task_assigned_when_permission_is_auto_assigned_with_empty_authorisations() throws Exception {

        when(clientAccessControlService.hasExclusiveAccess(SERVICE_AUTHORIZATION_TOKEN))
            .thenReturn(true);

        when(caseDetails.getCaseType()).thenReturn("Asylum");
        when(caseDetails.getJurisdiction()).thenReturn("IA");
        when(caseDetails.getSecurityClassification()).thenReturn(("PUBLIC"));

        when(ccdDataServiceApi.getCase(any(), any(), eq("someCaseId")))
            .thenReturn(caseDetails);

        when(camundaServiceApi.evaluateConfigurationDmnTable(any(), any(), any(), any()))
            .thenReturn(asList(
                new ConfigurationDmnEvaluationResponse(stringValue("caseName"), stringValue("someName")),
                new ConfigurationDmnEvaluationResponse(stringValue("appealType"), stringValue("protection")),
                new ConfigurationDmnEvaluationResponse(stringValue("region"), stringValue("1")),
                new ConfigurationDmnEvaluationResponse(stringValue("location"), stringValue("765324")),
                new ConfigurationDmnEvaluationResponse(stringValue("locationName"), stringValue("Taylor House")),
                new ConfigurationDmnEvaluationResponse(stringValue("workType"), stringValue("decision_making_work")),
                new ConfigurationDmnEvaluationResponse(stringValue("caseManagementCategory"), stringValue("Protection"))
            ));

        when(camundaServiceApi.evaluatePermissionsDmnTable(any(), any(), any(), any()))
            .thenReturn(asList(
                new PermissionsDmnEvaluationResponse(
                    stringValue("case-manager"),
                    stringValue("Read,Refer,Own"),
                    null,
                    integerValue(1),
                    booleanValue(true),
                    stringValue("LEGAL_OPERATIONS"),
                    stringValue(null)
                ),
                new PermissionsDmnEvaluationResponse(
                    stringValue("tribunal-caseworker"),
                    stringValue("Read,Refer,Own"),
                    null,
                    integerValue(1),
                    booleanValue(false),
                    stringValue("LEGAL_OPERATIONS"),
                    stringValue(null)
                )
            ));


        when(roleAssignmentServiceApi.queryRoleAssignments(any(), any(), any()))
            .thenReturn(new RoleAssignmentResource(
                singletonList(RoleAssignment.builder()
                    .id("someId")
                    .actorIdType(ActorIdType.IDAM)
                    .actorId(IDAM_USER_ID)
                    .roleName("case-manager")
                    .roleCategory(RoleCategory.LEGAL_OPERATIONS)
                    .grantType(GrantType.SPECIFIC)
                    .roleType(RoleType.CASE)
                    .classification(Classification.PUBLIC)
                    .authorisations(List.of("IA"))
                    .build())));
        ZonedDateTime createdDate = ZonedDateTime.now();
        ZonedDateTime dueDate = createdDate.plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);

        InitiateTaskRequestAttributes req = new InitiateTaskRequestAttributes(INITIATION, asList(
            new TaskAttribute(TASK_TYPE, "followUpOverdueReasonsForAppeal"),
            new TaskAttribute(TASK_NAME, "aTaskName"),
            new TaskAttribute(TASK_CASE_ID, "someCaseId"),
            new TaskAttribute(TASK_DUE_DATE, formattedDueDate)
        ));

        mockMvc
            .perform(post(ENDPOINT_BEING_TESTED)
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(req)))
            .andDo(MockMvcResultHandlers.print())
            .andExpectAll(
                status().isCreated(),
                content().contentType(APPLICATION_JSON_VALUE),
                jsonPath("$.task_id").value(taskId),
                jsonPath("$.task_name").value("aTaskName"),
                jsonPath("$.task_type").value("followUpOverdueReasonsForAppeal"),
                jsonPath("$.state").value("ASSIGNED"),
                jsonPath("$.assignee").value(IDAM_USER_ID),
                jsonPath("$.task_system").value("SELF"),
                jsonPath("$.security_classification").value("PUBLIC"),
                jsonPath("$.title").value("aTaskName"),
                jsonPath("$.auto_assigned").value(true),
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
                    + "(Typically this will be in CCD.)")
            );
    }

    @Test
    void should_keep_task_assigned_when_user_has_still_valid_permissions_and_auto_assigned_is_true() throws Exception {

        when(clientAccessControlService.hasExclusiveAccess(SERVICE_AUTHORIZATION_TOKEN))
            .thenReturn(true);

        when(caseDetails.getCaseType()).thenReturn("Asylum");
        when(caseDetails.getJurisdiction()).thenReturn("IA");
        when(caseDetails.getSecurityClassification()).thenReturn(("PUBLIC"));

        when(ccdDataServiceApi.getCase(any(), any(), eq("someCaseId")))
            .thenReturn(caseDetails);

        when(camundaServiceApi.evaluateConfigurationDmnTable(any(), any(), any(), any()))
            .thenReturn(asList(
                new ConfigurationDmnEvaluationResponse(stringValue("caseName"), stringValue("someName")),
                new ConfigurationDmnEvaluationResponse(stringValue("appealType"), stringValue("protection")),
                new ConfigurationDmnEvaluationResponse(stringValue("region"), stringValue("1")),
                new ConfigurationDmnEvaluationResponse(stringValue("location"), stringValue("765324")),
                new ConfigurationDmnEvaluationResponse(stringValue("locationName"), stringValue("Taylor House")),
                new ConfigurationDmnEvaluationResponse(stringValue("workType"), stringValue("decision_making_work")),
                new ConfigurationDmnEvaluationResponse(stringValue("caseManagementCategory"), stringValue("Protection"))
            ));

        when(camundaServiceApi.evaluatePermissionsDmnTable(any(), any(), any(), any()))
            .thenReturn(asList(
                new PermissionsDmnEvaluationResponse(
                    stringValue("tribunal-caseworker"),
                    stringValue("Read,Refer,Own"),
                    stringValue("IA,WA"),
                    integerValue(1),
                    booleanValue(true),
                    stringValue("LEGAL_OPERATIONS"),
                    stringValue(null)
                ),
                new PermissionsDmnEvaluationResponse(
                    stringValue("senior-tribunal-caseworker"),
                    stringValue("Read,Refer,Own"),
                    null,
                    null,
                    null,
                    stringValue("LEGAL_OPERATIONS"),
                    stringValue(null)
                )
            ));

        when(roleAssignmentServiceApi.getRolesForUser(any(), any(), any()))
            .thenReturn(new RoleAssignmentResource(
                List.of(RoleAssignment.builder()
                    .id("someId")
                    .actorIdType(ActorIdType.IDAM)
                    .actorId("someAssignee")
                    .roleName("tribunal-caseworker")
                    .roleCategory(RoleCategory.LEGAL_OPERATIONS)
                    .grantType(GrantType.SPECIFIC)
                    .roleType(RoleType.ORGANISATION)
                    .classification(Classification.PUBLIC)
                    .authorisations(List.of("IA"))
                    .build())));
        ZonedDateTime createdDate = ZonedDateTime.now();
        ZonedDateTime dueDate = createdDate.plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);

        InitiateTaskRequestAttributes req = new InitiateTaskRequestAttributes(INITIATION, asList(
            new TaskAttribute(TASK_TYPE, "followUpOverdueReasonsForAppeal"),
            new TaskAttribute(TASK_ASSIGNEE, "someAssignee"),
            new TaskAttribute(TASK_STATE, "UNCONFIGURED"),
            new TaskAttribute(TASK_NAME, "aTaskName"),
            new TaskAttribute(TASK_CASE_ID, "someCaseId"),
            new TaskAttribute(TASK_DUE_DATE, formattedDueDate)
        ));

        mockMvc
            .perform(post(ENDPOINT_BEING_TESTED)
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(req)))
            .andDo(MockMvcResultHandlers.print())
            .andExpectAll(
                status().isCreated(),
                content().contentType(APPLICATION_JSON_VALUE),
                jsonPath("$.task_id").value(taskId),
                jsonPath("$.task_name").value("aTaskName"),
                jsonPath("$.task_type").value("followUpOverdueReasonsForAppeal"),
                jsonPath("$.state").value("ASSIGNED"),
                jsonPath("$.assignee").value("someAssignee"),
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
                jsonPath("$.execution_type_code.execution_name")
                    .value("Case Management Task"),
                jsonPath("$.execution_type_code.description").value(
                    "The task requires a case management event to be executed by the user. "
                    + "(Typically this will be in CCD.)"),
                jsonPath("$.execution_type_code.execution_code").value("CASE_EVENT")
            );
    }

    @Test
    void should_assign_task_to_another_user_when_assigned_user_permissions_not_valid_and_auto_assigned_is_true()
        throws Exception {

        when(clientAccessControlService.hasExclusiveAccess(SERVICE_AUTHORIZATION_TOKEN))
            .thenReturn(true);

        when(caseDetails.getCaseType()).thenReturn("Asylum");
        when(caseDetails.getJurisdiction()).thenReturn("IA");
        when(caseDetails.getSecurityClassification()).thenReturn(("PUBLIC"));

        when(ccdDataServiceApi.getCase(any(), any(), eq("someCaseId")))
            .thenReturn(caseDetails);

        when(camundaServiceApi.evaluateConfigurationDmnTable(any(), any(), any(), any()))
            .thenReturn(asList(
                new ConfigurationDmnEvaluationResponse(stringValue("caseName"), stringValue("someName")),
                new ConfigurationDmnEvaluationResponse(stringValue("appealType"), stringValue("protection")),
                new ConfigurationDmnEvaluationResponse(stringValue("region"), stringValue("1")),
                new ConfigurationDmnEvaluationResponse(stringValue("location"), stringValue("765324")),
                new ConfigurationDmnEvaluationResponse(stringValue("locationName"), stringValue("Taylor House")),
                new ConfigurationDmnEvaluationResponse(stringValue("workType"), stringValue("decision_making_work")),
                new ConfigurationDmnEvaluationResponse(stringValue("caseManagementCategory"), stringValue("Protection"))
            ));

        when(camundaServiceApi.evaluatePermissionsDmnTable(any(), any(), any(), any()))
            .thenReturn(asList(
                new PermissionsDmnEvaluationResponse(
                    stringValue("tribunal-caseworker"),
                    stringValue("Read,Refer,Own"),
                    stringValue("IA,WA"),
                    integerValue(1),
                    booleanValue(true),
                    stringValue("LEGAL_OPERATIONS"),
                    stringValue(null)
                ),
                new PermissionsDmnEvaluationResponse(
                    stringValue("senior-tribunal-caseworker"),
                    stringValue("Read,Refer,Own"),
                    null,
                    null,
                    null,
                    stringValue("LEGAL_OPERATIONS"),
                    stringValue(null)
                )
            ));


        when(roleAssignmentServiceApi.getRolesForUser(any(), any(), any()))
            .thenReturn(new RoleAssignmentResource(emptyList()));
        when(roleAssignmentServiceApi.queryRoleAssignments(any(), any(), any()))
            .thenReturn(new RoleAssignmentResource(
                singletonList(RoleAssignment.builder()
                    .id("someId")
                    .actorIdType(ActorIdType.IDAM)
                    .actorId("anotherAssignee")
                    .roleName("tribunal-caseworker")
                    .roleCategory(RoleCategory.LEGAL_OPERATIONS)
                    .grantType(GrantType.SPECIFIC)
                    .roleType(RoleType.ORGANISATION)
                    .classification(Classification.PUBLIC)
                    .authorisations(List.of("IA"))
                    .build())));
        ZonedDateTime createdDate = ZonedDateTime.now();
        ZonedDateTime dueDate = createdDate.plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);

        InitiateTaskRequestAttributes req = new InitiateTaskRequestAttributes(INITIATION, asList(
            new TaskAttribute(TASK_TYPE, "followUpOverdueReasonsForAppeal"),
            new TaskAttribute(TASK_ASSIGNEE, "someAssignee"),
            new TaskAttribute(TASK_STATE, "UNCONFIGURED"),
            new TaskAttribute(TASK_NAME, "aTaskName"),
            new TaskAttribute(TASK_CASE_ID, "someCaseId"),
            new TaskAttribute(TASK_DUE_DATE, formattedDueDate)
        ));

        mockMvc
            .perform(post(ENDPOINT_BEING_TESTED)
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(req)))
            .andDo(MockMvcResultHandlers.print())
            .andExpectAll(
                status().isCreated(),
                content().contentType(APPLICATION_JSON_VALUE),
                jsonPath("$.task_id").value(taskId),
                jsonPath("$.task_name").value("aTaskName"),
                jsonPath("$.task_type").value("followUpOverdueReasonsForAppeal"),
                jsonPath("$.state").value("ASSIGNED"),
                jsonPath("$.task_system").value("SELF"),
                jsonPath("$.security_classification").value("PUBLIC"),
                jsonPath("$.title").value("aTaskName"),
                jsonPath("$.auto_assigned").value(true),
                jsonPath("$.has_warnings").value(false),
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
                    + "(Typically this will be in CCD.)")
            );
    }

    @Test
    void should_set_task_unassigned_when_permissions_is_auto_assigned_with_authorisations_mismatch() throws Exception {

        when(clientAccessControlService.hasExclusiveAccess(SERVICE_AUTHORIZATION_TOKEN))
            .thenReturn(true);

        when(caseDetails.getCaseType()).thenReturn("Asylum");
        when(caseDetails.getJurisdiction()).thenReturn("IA");
        when(caseDetails.getSecurityClassification()).thenReturn(("PUBLIC"));

        when(ccdDataServiceApi.getCase(any(), any(), eq("someCaseId")))
            .thenReturn(caseDetails);

        when(camundaServiceApi.evaluateConfigurationDmnTable(any(), any(), any(), any()))
            .thenReturn(asList(
                new ConfigurationDmnEvaluationResponse(stringValue("caseName"), stringValue("someName")),
                new ConfigurationDmnEvaluationResponse(stringValue("appealType"), stringValue("protection")),
                new ConfigurationDmnEvaluationResponse(stringValue("region"), stringValue("1")),
                new ConfigurationDmnEvaluationResponse(stringValue("location"), stringValue("765324")),
                new ConfigurationDmnEvaluationResponse(stringValue("locationName"), stringValue("Taylor House")),
                new ConfigurationDmnEvaluationResponse(stringValue("workType"), stringValue("decision_making_work")),
                new ConfigurationDmnEvaluationResponse(stringValue("caseManagementCategory"), stringValue("Protection"))
            ));

        when(camundaServiceApi.evaluatePermissionsDmnTable(any(), any(), any(), any()))
            .thenReturn(asList(
                new PermissionsDmnEvaluationResponse(
                    stringValue("tribunal-caseworker"),
                    stringValue("Read,Refer,Own"),
                    stringValue("WA"),
                    integerValue(1),
                    booleanValue(true),
                    stringValue("LEGAL_OPERATIONS"),
                    stringValue(null)
                ),
                new PermissionsDmnEvaluationResponse(
                    stringValue("senior-tribunal-caseworker"),
                    stringValue("Read,Refer,Own"),
                    null,
                    null,
                    null,
                    stringValue("LEGAL_OPERATIONS"),
                    stringValue(null)
                )
            ));


        when(roleAssignmentServiceApi.getRolesForUser(any(), any(), any()))
            .thenReturn(new RoleAssignmentResource(emptyList()));
        when(roleAssignmentServiceApi.queryRoleAssignments(any(), any(), any()))
            .thenReturn(new RoleAssignmentResource(
                singletonList(RoleAssignment.builder()
                                  .id("someId")
                                  .actorIdType(ActorIdType.IDAM)
                                  .actorId("anotherAssignee")
                                  .roleName("tribunal-caseworker")
                                  .roleCategory(RoleCategory.LEGAL_OPERATIONS)
                                  .grantType(GrantType.SPECIFIC)
                                  .roleType(RoleType.ORGANISATION)
                                  .classification(Classification.PUBLIC)
                                  .authorisations(List.of("IA"))
                                  .build())));
        ZonedDateTime createdDate = ZonedDateTime.now();
        ZonedDateTime dueDate = createdDate.plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);

        InitiateTaskRequestAttributes req = new InitiateTaskRequestAttributes(INITIATION, asList(
            new TaskAttribute(TASK_TYPE, "followUpOverdueReasonsForAppeal"),
            new TaskAttribute(TASK_STATE, "UNCONFIGURED"),
            new TaskAttribute(TASK_NAME, "aTaskName"),
            new TaskAttribute(TASK_CASE_ID, "someCaseId"),
            new TaskAttribute(TASK_DUE_DATE, formattedDueDate)
        ));

        mockMvc
            .perform(post(ENDPOINT_BEING_TESTED)
                         .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                         .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                         .contentType(MediaType.APPLICATION_JSON_VALUE)
                         .content(asJsonString(req)))
            .andDo(MockMvcResultHandlers.print())
            .andExpectAll(
                status().isCreated(),
                content().contentType(APPLICATION_JSON_VALUE),
                jsonPath("$.task_id").value(taskId),
                jsonPath("$.task_name").value("aTaskName"),
                jsonPath("$.task_type").value("followUpOverdueReasonsForAppeal"),
                jsonPath("$.state").value("UNASSIGNED"),
                jsonPath("$.task_system").value("SELF"),
                jsonPath("$.security_classification").value("PUBLIC"),
                jsonPath("$.title").value("aTaskName"),
                jsonPath("$.auto_assigned").value(false),
                jsonPath("$.has_warnings").value(false),
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
                jsonPath("$.task_role_resources.[0].manage").value(false),
                jsonPath("$.task_role_resources.[0].cancel").value(false),
                jsonPath("$.task_role_resources.[1].task_id").value(taskId),
                jsonPath("$.task_role_resources.[1].role_name")
                    .value(anyOf(is("tribunal-caseworker"), is("senior-tribunal-caseworker"))),
                jsonPath("$.task_role_resources.[1].read").value(true),
                jsonPath("$.task_role_resources.[1].own").value(true),
                jsonPath("$.task_role_resources.[1].execute").value(false),
                jsonPath("$.task_role_resources.[1].manage").value(false),
                jsonPath("$.task_role_resources.[1].cancel").value(false)
            );
    }

    @Test
    void should_set_task_unassigned_when_assigned_user_has_invalid_permissions_and_auto_assign_is_false()
        throws Exception {

        when(clientAccessControlService.hasExclusiveAccess(SERVICE_AUTHORIZATION_TOKEN))
            .thenReturn(true);

        when(caseDetails.getCaseType()).thenReturn("Asylum");
        when(caseDetails.getJurisdiction()).thenReturn("IA");
        when(caseDetails.getSecurityClassification()).thenReturn(("PUBLIC"));

        when(ccdDataServiceApi.getCase(any(), any(), eq("someCaseId")))
            .thenReturn(caseDetails);

        when(camundaServiceApi.evaluateConfigurationDmnTable(any(), any(), any(), any()))
            .thenReturn(asList(
                new ConfigurationDmnEvaluationResponse(stringValue("caseName"), stringValue("someName")),
                new ConfigurationDmnEvaluationResponse(stringValue("appealType"), stringValue("protection")),
                new ConfigurationDmnEvaluationResponse(stringValue("region"), stringValue("1")),
                new ConfigurationDmnEvaluationResponse(stringValue("location"), stringValue("765324")),
                new ConfigurationDmnEvaluationResponse(stringValue("locationName"), stringValue("Taylor House")),
                new ConfigurationDmnEvaluationResponse(stringValue("workType"), stringValue("decision_making_work")),
                new ConfigurationDmnEvaluationResponse(stringValue("caseManagementCategory"), stringValue("Protection"))
            ));

        when(camundaServiceApi.evaluatePermissionsDmnTable(any(), any(), any(), any()))
            .thenReturn(asList(
                new PermissionsDmnEvaluationResponse(
                    stringValue("tribunal-caseworker"),
                    stringValue("Read,Refer,Own"),
                    stringValue("IA,WA"),
                    null,
                    null,
                    stringValue("LEGAL_OPERATIONS"),
                    stringValue(null)
                ),
                new PermissionsDmnEvaluationResponse(
                    stringValue("senior-tribunal-caseworker"),
                    stringValue("Read,Refer,Own"),
                    null,
                    null,
                    null,
                    stringValue("LEGAL_OPERATIONS"),
                    stringValue(null)
                )
            ));


        when(roleAssignmentServiceApi.getRolesForUser(eq("someAssignee"), any(), any()))
            .thenReturn(new RoleAssignmentResource(emptyList()));
        when(roleAssignmentServiceApi.queryRoleAssignments(any(), any(), any()))
            .thenReturn(new RoleAssignmentResource(emptyList()));
        ZonedDateTime createdDate = ZonedDateTime.now();
        ZonedDateTime dueDate = createdDate.plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);

        InitiateTaskRequestAttributes req = new InitiateTaskRequestAttributes(INITIATION, asList(
            new TaskAttribute(TASK_TYPE, "followUpOverdueReasonsForAppeal"),
            new TaskAttribute(TASK_ASSIGNEE, "someAssignee"),
            new TaskAttribute(TASK_STATE, "UNCONFIGURED"),
            new TaskAttribute(TASK_NAME, "aTaskName"),
            new TaskAttribute(TASK_CASE_ID, "someCaseId"),
            new TaskAttribute(TASK_DUE_DATE, formattedDueDate)
        ));

        mockMvc
            .perform(post(ENDPOINT_BEING_TESTED)
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(req)))
            .andDo(MockMvcResultHandlers.print())
            .andExpectAll(
                status().isCreated(),
                content().contentType(APPLICATION_JSON_VALUE),
                jsonPath("$.task_id").value(taskId),
                jsonPath("$.task_name").value("aTaskName"),
                jsonPath("$.task_type").value("followUpOverdueReasonsForAppeal"),
                jsonPath("$.state").value("UNASSIGNED"),
                jsonPath("$.assignee").doesNotExist(),
                jsonPath("$.task_system").value("SELF"),
                jsonPath("$.security_classification").value("PUBLIC"),
                jsonPath("$.title").value("aTaskName"),
                jsonPath("$.auto_assigned").value(false),
                jsonPath("$.has_warnings").value(false),
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
                jsonPath("$.task_role_resources.[0].manage").value(false),
                jsonPath("$.task_role_resources.[0].cancel").value(false),
                jsonPath("$.task_role_resources.[0].auto_assignable").value(false),
                jsonPath("$.task_role_resources.[1].task_id").value(taskId),
                jsonPath("$.task_role_resources.[1].role_name")
                    .value(anyOf(is("tribunal-caseworker"), is("senior-tribunal-caseworker"))),
                jsonPath("$.task_role_resources.[1].read").value(true),
                jsonPath("$.task_role_resources.[1].own").value(true),
                jsonPath("$.task_role_resources.[1].execute").value(false),
                jsonPath("$.task_role_resources.[1].manage").value(false),
                jsonPath("$.task_role_resources.[1].cancel").value(false),
                jsonPath("$.task_role_resources.[1].auto_assignable").value(false)
            );
    }

    @Test
    void should_return_503_when_task_is_already_initiated() throws Exception {
        TaskRoleResource taskRoleResource = new TaskRoleResource(
            TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleName(),
            false, true, true, false, false, false,
            new String[]{}, 1, false,
            TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleCategory().name()
        );
        insertDummyTaskInDb("IA", "Asylum", taskId, taskRoleResource);

        when(clientAccessControlService.hasExclusiveAccess(SERVICE_AUTHORIZATION_TOKEN))
            .thenReturn(true);

        when(caseDetails.getCaseType()).thenReturn("Asylum");
        when(caseDetails.getJurisdiction()).thenReturn("IA");
        when(caseDetails.getSecurityClassification()).thenReturn(("PUBLIC"));

        when(ccdDataServiceApi.getCase(any(), any(), eq("someCaseId")))
            .thenReturn(caseDetails);

        when(camundaServiceApi.evaluateConfigurationDmnTable(any(), any(), any(), any()))
            .thenReturn(asList(
                new ConfigurationDmnEvaluationResponse(stringValue("caseName"), stringValue("someName")),
                new ConfigurationDmnEvaluationResponse(stringValue("appealType"), stringValue("protection")),
                new ConfigurationDmnEvaluationResponse(stringValue("region"), stringValue("1")),
                new ConfigurationDmnEvaluationResponse(stringValue("location"), stringValue("765324")),
                new ConfigurationDmnEvaluationResponse(stringValue("locationName"), stringValue("Taylor House")),
                new ConfigurationDmnEvaluationResponse(stringValue("workType"), stringValue("decision_making_work")),
                new ConfigurationDmnEvaluationResponse(stringValue("caseManagementCategory"), stringValue("Protection"))
            ));

        when(camundaServiceApi.evaluatePermissionsDmnTable(any(), any(), any(), any()))
            .thenReturn(List.of(
                new PermissionsDmnEvaluationResponse(
                    stringValue("hearing-judge"),
                    stringValue("Read,Refer,Own"),
                    stringValue("IA,WA"),
                    integerValue(1),
                    booleanValue(true),
                    stringValue("LEGAL_OPERATIONS"),
                    stringValue(null)
                ),
                new PermissionsDmnEvaluationResponse(
                    stringValue("judge"),
                    stringValue("Read,Refer,Own"),
                    stringValue("IA,WA"),
                    integerValue(1),
                    booleanValue(false),
                    stringValue("LEGAL_OPERATIONS"),
                    stringValue(null)
                )
            ));


        when(roleAssignmentServiceApi.queryRoleAssignments(any(), any(), any()))
            .thenReturn(new RoleAssignmentResource(
                singletonList(RoleAssignment.builder()
                                  .id("someId")
                                  .actorIdType(ActorIdType.IDAM)
                                  .actorId(IDAM_USER_ID)
                                  .roleName("hearing-judge")
                                  .roleCategory(RoleCategory.LEGAL_OPERATIONS)
                                  .grantType(GrantType.SPECIFIC)
                                  .roleType(RoleType.ORGANISATION)
                                  .classification(Classification.PUBLIC)
                                  .authorisations(List.of("IA"))
                                  .build())));
        ZonedDateTime createdDate = ZonedDateTime.now();
        ZonedDateTime dueDate = createdDate.plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);

        InitiateTaskRequestAttributes req = new InitiateTaskRequestAttributes(INITIATION, asList(
            new TaskAttribute(TASK_TYPE, "followUpOverdueReasonsForAppeal"),
            new TaskAttribute(TASK_NAME, "aTaskName"),
            new TaskAttribute(TASK_CASE_ID, "someCaseId"),
            new TaskAttribute(TASK_DUE_DATE, formattedDueDate)
        ));

        mockMvc
            .perform(post(ENDPOINT_BEING_TESTED)
                         .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                         .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                         .contentType(MediaType.APPLICATION_JSON_VALUE)
                         .content(asJsonString(req)))
            .andDo(MockMvcResultHandlers.print())
            .andExpectAll(
                status().isServiceUnavailable(),
                content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
                jsonPath("$.type")
                    .value("https://github.com/hmcts/wa-task-management-api/problem/database-conflict"),
                jsonPath("$.title").value("Database Conflict Error"),
                jsonPath("$.status").value(503),
                jsonPath("$.detail").value(
                    "Database Conflict Error: The action could not be completed because "
                        + "there was a conflict in the database.")
            );
    }

    private void insertDummyTaskInDb(String jurisdiction, String caseType, String taskId,
                                     TaskRoleResource taskRoleResource) {
        TaskResource taskResource = new TaskResource(
            taskId,
            "someTaskName",
            "someTaskType",
            UNASSIGNED
        );
        taskResource.setCreated(OffsetDateTime.now());
        taskResource.setDueDateTime(OffsetDateTime.now());
        taskResource.setJurisdiction(jurisdiction);
        taskResource.setCaseTypeId(caseType);
        taskResource.setSecurityClassification(SecurityClassification.PUBLIC);
        taskResource.setLocation("765324");
        taskResource.setLocationName("Taylor House");
        taskResource.setRegion("TestRegion");
        taskResource.setCaseId("claimCaseId1");

        taskRoleResource.setTaskId(taskId);
        Set<TaskRoleResource> taskRoleResourceSet = Set.of(taskRoleResource);
        taskResource.setTaskRoleResources(taskRoleResourceSet);
        cftTaskDatabaseService.saveTask(taskResource);
    }

}

