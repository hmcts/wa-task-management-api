package uk.gov.hmcts.reform.wataskmanagementapi.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.opentest4j.AssertionFailedError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootIntegrationBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.IdamTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.Token;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserIdamTokenGeneratorInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.restrict.ClientAccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.RoleAssignmentService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.ActorIdType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleCategory;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.response.RoleAssignmentResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.CftQueryService;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CcdDataServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.RoleAssignmentServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.config.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.InitiateTaskRequestMap;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.TaskOperationRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.ExecuteReconfigureTaskFilter;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.MarkTaskToReconfigureTaskFilter;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskFilter;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskOperation;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskFilterOperator;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskOperationType;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.PermissionsDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.SecurityClassification;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.ccd.CaseDetails;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.enums.TestRolesWithGrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskRoleResource;
import uk.gov.hmcts.reform.wataskmanagementapi.enums.TaskAction;
import uk.gov.hmcts.reform.wataskmanagementapi.services.operation.TaskReconfigurationTransactionHandler;
import uk.gov.hmcts.reform.wataskmanagementapi.services.utils.TaskMandatoryFieldsValidator;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.role.RoleAssignmentService.TOTAL_RECORDS;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.SERVICE_AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.InitiateTaskOperation.INITIATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskOperationType.EXECUTE_RECONFIGURE;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskOperationType.MARK_TO_RECONFIGURE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaTime.CAMUNDA_DATA_TIME_FORMATTER;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaValue.booleanValue;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaValue.integerValue;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaValue.stringValue;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.DUE_DATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.TASK_NAME;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.TASK_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.IDAM_AUTHORIZATION_TOKEN;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.SERVICE_AUTHORIZATION_TOKEN;

@SuppressWarnings("checkstyle:LineLength")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DmnEvaluationServiceIntegrationTest extends SpringBootIntegrationBaseTest {

    public static final String SYSTEM_USER_1 = "system_user1";
    public static final String ASSIGNEE_USER = "assigneeUser";
    private static final String ENDPOINT_BEING_TESTED = "/task/operation";
    @MockBean(name = "systemUserIdamInfo")
    UserIdamTokenGeneratorInfo systemUserIdamInfo;
    @MockBean
    private ClientAccessControlService clientAccessControlService;
    @MockBean
    private CftQueryService cftQueryService;
    @MockBean
    private CcdDataService ccdDataService;
    @MockBean
    private CamundaServiceApi camundaServiceApi;
    @SpyBean
    private CFTTaskDatabaseService cftTaskDatabaseService;
    @MockBean
    private RoleAssignmentService roleAssignmentService;
    @MockBean
    private IdamWebApi idamWebApi;
    @MockBean
    private LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider;

    @MockBean
    private CaseDetails caseDetails;

    @MockBean
    private CcdDataServiceApi ccdDataServiceApi;

    @MockBean
    private RoleAssignmentServiceApi roleAssignmentServiceApi;

    @Autowired
    private IdamTokenGenerator systemUserIdamToken;

    @SpyBean
    TaskReconfigurationTransactionHandler taskReconfigurationTransactionHandler;
    @SpyBean
    TaskMandatoryFieldsValidator taskMandatoryFieldsValidator;

    private String taskId;
    private String bearerAccessToken1;

    private static final String INITIATION_ENDPOINT_PATH = "/task/%s/initiation";
    private static String INITIATION_ENDPOINT_BEING_TESTED;



    @BeforeEach
    void setUp() {
        taskId = UUID.randomUUID().toString();

        INITIATION_ENDPOINT_BEING_TESTED = String.format(INITIATION_ENDPOINT_PATH, taskId);

        when(clientAccessControlService.hasExclusiveAccess(SERVICE_AUTHORIZATION_TOKEN))
            .thenReturn(true);


        bearerAccessToken1 = "Token" + UUID.randomUUID();
        when(idamWebApi.token(any())).thenReturn(new Token(bearerAccessToken1, "Scope"));
        when(idamWebApi.userInfo(any())).thenReturn(UserInfo.builder().uid(SYSTEM_USER_1).build());
        CaseDetails caseDetails = new CaseDetails(
            "IA",
            "Asylum",
            SecurityClassification.PUBLIC.getSecurityClassification(),
            Map.of("caseAccessCategory", "categoryA,categoryC")
        );
        lenient().when(ccdDataService.getCaseData(anyString())).thenReturn(caseDetails);
        RoleAssignment roleAssignmentResource = buildRoleAssignment(
            ASSIGNEE_USER,
            "tribunalCaseworker",
            singletonList("IA")
        );
        List<RoleAssignment> roleAssignmentForAssignee = List.of(roleAssignmentResource);
        when(roleAssignmentService.getRolesByUserId(any())).thenReturn(roleAssignmentForAssignee);
        when(roleAssignmentService.queryRolesForAutoAssignmentByCaseId(any())).thenReturn(roleAssignmentForAssignee);
    }

    @ParameterizedTest
    @CsvSource({
        ",true,title",
        "'',true,title",
        "updatedTitle,true,updatedTitle"
    })
    void should_not_update_task_title_from_dmn_when_dmn_evaluates_title_as_null_or_empty_on_task_reconfig(
        String dmnEvaluatedTitleValue, boolean canReconfigure, String expectedTitleValue) throws Exception {
        String caseIdToday = "caseId5-" + OffsetDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
        OffsetDateTime dueDateTime = OffsetDateTime.now();
        createTaskAndRoleAssignments(CFTTaskState.ASSIGNED, ASSIGNEE_USER, caseIdToday, dueDateTime);
        doNothing().when(taskMandatoryFieldsValidator).validate(any(TaskResource.class));

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskOperationRequest(MARK_TO_RECONFIGURE, markTaskFilters(caseIdToday))))
        ).andExpectAll(
            status().is(HttpStatus.OK.value())
        );

        List<TaskResource> taskResourcesBefore = cftTaskDatabaseService.findByCaseIdOnly(caseIdToday);
        taskResourcesBefore.forEach(task -> {
            assertEquals(ASSIGNEE_USER, task.getAssignee());
            assertEquals(CFTTaskState.ASSIGNED, task.getState());
            assertEquals("title", task.getTitle());
            assertNotNull(task.getReconfigureRequestTime());
        });
        when(camundaServiceApi.evaluateConfigurationDmnTable(any(), any(), any(), any()))
            .thenReturn(new ArrayList<>(List.of(
                new ConfigurationDmnEvaluationResponse(stringValue("caseName"), stringValue("updatedCaseName"),
                                                       booleanValue(true)),
                new ConfigurationDmnEvaluationResponse(stringValue("title"), dmnEvaluatedTitleValue == null ? null
                    : stringValue(dmnEvaluatedTitleValue), booleanValue(canReconfigure))

            )));



        when(camundaServiceApi.evaluatePermissionsDmnTable(
            anyString(),
            anyString(),
            anyString(),
            any())).thenReturn(permissionsResponse());

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskOperationRequest(
                    EXECUTE_RECONFIGURE,
                    executeTaskFilters(OffsetDateTime.now().minusSeconds(30L))
                )))
        ).andExpectAll(
            status().is(HttpStatus.OK.value())
        );
        await().ignoreException(AssertionFailedError.class)
            .pollInterval(1, SECONDS)
            .atMost(10, SECONDS)
            .untilAsserted(() -> {
                List<TaskResource> taskResourcesAfter = cftTaskDatabaseService.findByCaseIdOnly(caseIdToday);

                taskResourcesAfter
                    .forEach(task -> {
                        assertNotNull(task.getLastReconfigurationTime());
                        assertNull(task.getReconfigureRequestTime());
                        assertTrue(LocalDateTime.now().isAfter(task.getLastReconfigurationTime().toLocalDateTime()));
                        assertEquals(expectedTitleValue, task.getTitle());
                        assertEquals("updatedCaseName", task.getCaseName());
                        assertEquals(TaskAction.CONFIGURE.getValue(), task.getLastUpdatedAction());
                    });
            });


    }

    @ParameterizedTest
    @CsvSource({
        ",follow Up Overdue Reasons For Appeal",
        "'',follow Up Overdue Reasons For Appeal",
        "updatedTitle,updatedTitle"
    })
    void should_not_update_task_title_from_dmn_when_dmn_evaluates_title_as_null_or_empty_during_task_initiation(
        String dmnEvaluatedTitleValue, String expectedValue)
        throws Exception {
        when(clientAccessControlService.hasExclusiveAccess(SERVICE_AUTHORIZATION_TOKEN))
            .thenReturn(true);

        when(caseDetails.getCaseType()).thenReturn("");
        when(caseDetails.getJurisdiction()).thenReturn("IA");
        when(caseDetails.getSecurityClassification()).thenReturn(("PUBLIC"));

        when(ccdDataServiceApi.getCase(any(), any(), eq("someCaseId")))
            .thenReturn(caseDetails);

        when(camundaServiceApi.evaluateConfigurationDmnTable(any(), any(), any(), any()))
            .thenReturn(new ArrayList<>(List.of(
                new ConfigurationDmnEvaluationResponse(stringValue("caseName"), stringValue("someName1, someName2")),
                new ConfigurationDmnEvaluationResponse(stringValue("title"),
                                                       dmnEvaluatedTitleValue == null ? null
                                                           : stringValue(dmnEvaluatedTitleValue))
            )));

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
                )
            ));

        when(roleAssignmentServiceApi.queryRoleAssignments(any(), any(), any(), any(), any()))
            .thenReturn(ResponseEntity.ok()
                            .header(TOTAL_RECORDS, "0")
                            .body(new RoleAssignmentResource(emptyList())));

        ZonedDateTime createdDate = ZonedDateTime.now();
        ZonedDateTime dueDate = createdDate.plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);

        Map<String, Object> taskAttributes = Map.of(
            TASK_TYPE.value(), "followUpOverdueReasonsForAppeal",
            TASK_NAME.value(), "follow Up Overdue Reasons For Appeal",
            CASE_ID.value(), "someCaseId",
            DUE_DATE.value(), formattedDueDate
        );

        InitiateTaskRequestMap req = new InitiateTaskRequestMap(INITIATION, taskAttributes);

        mockMvc
            .perform(post(INITIATION_ENDPOINT_BEING_TESTED)
                         .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                         .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                         .contentType(MediaType.APPLICATION_JSON_VALUE)
                         .content(asJsonString(req)))
            .andExpectAll(status().isCreated(),
                          content().contentType(APPLICATION_JSON_VALUE),
                          jsonPath("$.task_id").value(taskId),
                          jsonPath("$.task_name").value("follow Up Overdue Reasons For Appeal"),
                          jsonPath("$.task_type").value("followUpOverdueReasonsForAppeal"),
                          jsonPath("$.title").value(expectedValue)

            );
    }


    private TaskOperationRequest taskOperationRequest(TaskOperationType operationName, List<TaskFilter<?>> taskFilters) {
        TaskOperation operation = TaskOperation
            .builder()
            .type(operationName)
            .runId(UUID.randomUUID().toString())
            .maxTimeLimit(60)
            .retryWindowHours(0)
            .build();
        return new TaskOperationRequest(operation, taskFilters);
    }

    private List<TaskFilter<?>> executeTaskFilters(OffsetDateTime reconfigureRequestTime) {
        TaskFilter<?> filter = new ExecuteReconfigureTaskFilter("reconfigure_request_time",
                                                                reconfigureRequestTime, TaskFilterOperator.AFTER
        );
        return List.of(filter);
    }

    private List<TaskFilter<?>> markTaskFilters(String caseId) {
        TaskFilter<?> filter = new MarkTaskToReconfigureTaskFilter(
            "case_id", List.of(caseId), TaskFilterOperator.IN
        );
        return List.of(filter);
    }

    private void insertDummyTaskInDb(String jurisdiction,
                                     String caseType,
                                     String caseId,
                                     String taskId,
                                     CFTTaskState cftTaskState,
                                     String assignee,
                                     OffsetDateTime dueDateTime,
                                     TaskRoleResource taskRoleResource) {
        TaskResource taskResource = new TaskResource(
            taskId,
            "someTaskName",
            "someTaskType",
            cftTaskState
        );
        taskResource.setCreated(OffsetDateTime.now());
        taskResource.setDueDateTime(dueDateTime);
        taskResource.setJurisdiction(jurisdiction);
        taskResource.setCaseTypeId(caseType);
        taskResource.setSecurityClassification(SecurityClassification.PUBLIC);
        taskResource.setLocation("765324");
        taskResource.setLocationName("Taylor House");
        taskResource.setRegion("TestRegion");
        taskResource.setCaseId(caseId);
        taskResource.setAssignee(assignee);
        taskResource.setTitle("title");
        taskRoleResource.setTaskId(taskId);
        Set<TaskRoleResource> taskRoleResourceSet = Set.of(taskRoleResource);
        taskResource.setTaskRoleResources(taskRoleResourceSet);
        cftTaskDatabaseService.saveTask(taskResource);
    }

    private void createTaskAndRoleAssignments(CFTTaskState cftTaskState, String assignee, String caseId,
                                              OffsetDateTime dueDateTime) {

        //assigner permission : manage, own, cancel
        TaskRoleResource assignerTaskRoleResource = new TaskRoleResource(
            TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleName(),
            false, true, true, true, true, false,
            new String[]{"IA"}, 1, true,
            TestRolesWithGrantType.SPECIFIC_TRIBUNAL_CASE_WORKER.getRoleCategory().name()
        );
        String jurisdiction = "IA";
        String caseType = "Asylum";
        insertDummyTaskInDb(jurisdiction, caseType, caseId, taskId, cftTaskState, assignee, dueDateTime,
                            assignerTaskRoleResource
        );

        List<RoleAssignment> assignerRoles = new ArrayList<>();
        RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.SPECIFIC_HEARING_PANEL_JUDGE)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(jurisdiction)
                    .caseType(caseType)
                    .caseId(caseId)
                    .build()
            )
            .build();

        createRoleAssignment(assignerRoles, roleAssignmentRequest);
    }

    private List<PermissionsDmnEvaluationResponse> permissionsResponse() {
        return asList(
            new PermissionsDmnEvaluationResponse(
                stringValue("tribunalCaseworker"),
                stringValue("Read,Refer,Own,Execute,Manage,Cancel"),
                stringValue("IA"),
                integerValue(1),
                booleanValue(true),
                stringValue("LEGAL_OPERATIONS"),
                stringValue("categoryA,categoryC")
            ),
            new PermissionsDmnEvaluationResponse(
                stringValue("seniorTribunalCaseworker"),
                stringValue("Read,Refer,Own,Execute,Manage,Cancel"),
                stringValue("IA"),
                integerValue(2),
                booleanValue(true),
                stringValue("LEGAL_OPERATIONS"),
                stringValue("categoryB,categoryD")
            )
        );
    }



    private RoleAssignment buildRoleAssignment(String actorId, String roleName, List<String> authorisations) {
        return RoleAssignment.builder()
            .id(UUID.randomUUID().toString())
            .actorIdType(ActorIdType.IDAM)
            .actorId(actorId)
            .roleName(roleName)
            .roleCategory(RoleCategory.LEGAL_OPERATIONS)
            .roleType(RoleType.ORGANISATION)
            .classification(Classification.PUBLIC)
            .authorisations(authorisations)
            .grantType(GrantType.STANDARD)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();
    }

}

