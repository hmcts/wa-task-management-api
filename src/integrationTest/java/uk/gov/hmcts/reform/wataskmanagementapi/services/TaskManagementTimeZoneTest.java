package uk.gov.hmcts.reform.wataskmanagementapi.services;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.opentest4j.AssertionFailedError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.Token;
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
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.PermissionsDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.SecurityClassification;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.ccd.CaseDetails;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.ReportableTaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskHistoryResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.services.utils.TaskMandatoryFieldsValidator;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.IntegrationTestUtils;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskTestUtils;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.ASSIGNMENT_EXPIRY;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.CREATED;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.DUE_DATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.NEXT_HEARING_DATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.TASK_NAME;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.TASK_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.TITLE;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.IDAM_AUTHORIZATION_TOKEN;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.IDAM_USER_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.SERVICE_AUTHORIZATION_TOKEN;

@ActiveProfiles("replica")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Slf4j
class TaskManagementTimeZoneTest extends ReplicaBaseTest {

    @MockitoBean
    private ClientAccessControlService clientAccessControlService;

    @Mock
    private CaseDetails caseDetails;

    @MockitoBean
    private CcdDataServiceApi ccdDataServiceApi;

    @MockitoBean
    private CamundaServiceApi camundaServiceApi;

    @MockitoBean
    private RoleAssignmentServiceApi roleAssignmentServiceApi;

    @MockitoBean
    private AuthTokenGenerator authTokenGenerator;

    private ServiceMocks mockServices;

    @MockitoBean
    private IdamWebApi idamWebApi;
    @MockitoBean
    private ServiceAuthorisationApi serviceAuthorisationApi;

    @MockitoBean
    private LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider;

    @MockitoSpyBean
    private CFTTaskDatabaseService cftTaskDatabaseService;

    @MockitoSpyBean
    TaskMandatoryFieldsValidator taskMandatoryFieldsValidator;

    @MockitoBean
    private DmnEvaluationService dmnEvaluationService;

    @MockitoBean
    private CftQueryService cftQueryService;

    @MockitoBean
    private CcdDataService ccdDataService;

    @MockitoBean
    private RoleAssignmentService roleAssignmentService;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    IntegrationTestUtils integrationTestUtils;

    TaskTestUtils taskTestUtils;

    private String bearerAccessToken1;

    public static final String SYSTEM_USER_1 = "system_user1";
    public static final String ASSIGNEE_USER = "assigneeUser";

    @BeforeAll
    void init() {
        taskTestUtils = new TaskTestUtils(cftTaskDatabaseService,"primary");
    }

    @ParameterizedTest
    @ValueSource(strings = {"UTC", "BST"})
    void when_timezone_changes_all_timestamp_attributes_should_behave_consistently(String timeZone) throws Exception {

        AtomicReference<TaskResource> taskResource = new AtomicReference<>();

        AtomicReference<TaskHistoryResource> taskHistoryResource = new AtomicReference<>();

        AtomicReference<ReportableTaskResource> reportableTaskResource = new AtomicReference<>();

        String taskId;

        log.info("TimeZone ({})", timeZone);

        if ("UTC".equals(timeZone)) {
            taskId = initiateTask(OffsetDateTime.of(2024, 10, 27, 02,
                00, 00, 0, ZoneOffset.UTC).toZonedDateTime());
        } else {
            taskId = initiateTask(OffsetDateTime.of(2024, 03, 31,
                01, 00, 00, 0,
                ZoneOffset.of("+01:00")).toZonedDateTime());
        }

        await().ignoreException(AssertionFailedError.class)
            .pollInterval(1, SECONDS)
            .atMost(10, SECONDS)
            .until(
                () -> {
                    Optional<TaskResource> optionalTaskResource = taskResourceRepository.getByTaskId(taskId);
                    assertTrue(optionalTaskResource.isPresent());

                    taskResource.set(optionalTaskResource.get());

                    List<TaskHistoryResource> taskHistoryResourceList
                        = miReportingServiceForTest.findByTaskId(taskResource.get().getTaskId());

                    assertFalse(taskHistoryResourceList.isEmpty());

                    taskHistoryResource.set(taskHistoryResourceList.get(0));

                    List<ReportableTaskResource> reportableTaskList
                        = miReportingServiceForTest.findByReportingTaskId(taskResource.get().getTaskId());

                    assertFalse(reportableTaskList.isEmpty());
                    assertEquals(1, reportableTaskList.size());

                    reportableTaskResource.set(reportableTaskList.get(0));

                    return true;
                });

        assertTrue(taskResource.get().getLastUpdatedTimestamp().truncatedTo(ChronoUnit.SECONDS)
                       .isEqual(taskHistoryResource.get().getUpdated()
                                    .truncatedTo(ChronoUnit.SECONDS)));
        assertTrue(taskResource.get().getCreated().truncatedTo(ChronoUnit.SECONDS)
                       .isEqual(taskHistoryResource.get().getCreated()
                                    .truncatedTo(ChronoUnit.SECONDS)));
        assertTrue(taskResource.get().getPriorityDate().truncatedTo(ChronoUnit.SECONDS)
                       .isEqual(taskHistoryResource.get().getPriorityDate()
                                    .truncatedTo(ChronoUnit.SECONDS)));
        assertTrue(taskResource.get().getNextHearingDate().truncatedTo(ChronoUnit.SECONDS)
                       .isEqual(taskHistoryResource.get().getNextHearingDate()
                                    .truncatedTo(ChronoUnit.SECONDS)));
        assertTrue(taskResource.get().getAssignmentExpiry().truncatedTo(ChronoUnit.SECONDS)
                       .isEqual(taskHistoryResource.get().getAssignmentExpiry()
                                    .truncatedTo(ChronoUnit.SECONDS)));
        assertTrue(taskResource.get().getDueDateTime().truncatedTo(ChronoUnit.SECONDS)
                       .isEqual(taskHistoryResource.get().getDueDateTime()
                                    .truncatedTo(ChronoUnit.SECONDS)));

        assertTrue(taskResource.get().getLastUpdatedTimestamp().truncatedTo(ChronoUnit.SECONDS)
                       .isEqual(reportableTaskResource.get().getUpdated().truncatedTo(ChronoUnit.SECONDS)));
        assertTrue(taskResource.get().getCreated().truncatedTo(ChronoUnit.SECONDS)
                       .isEqual(reportableTaskResource.get().getCreated().truncatedTo(ChronoUnit.SECONDS)));
        assertTrue(taskResource.get().getPriorityDate().truncatedTo(ChronoUnit.SECONDS)
                       .isEqual(reportableTaskResource.get().getPriorityDate()
                                    .truncatedTo(ChronoUnit.SECONDS)));
        assertTrue(taskResource.get().getNextHearingDate().truncatedTo(ChronoUnit.SECONDS)
                       .isEqual(reportableTaskResource.get().getNextHearingDate()
                                    .truncatedTo(ChronoUnit.SECONDS)));
        assertTrue(taskResource.get().getAssignmentExpiry().truncatedTo(ChronoUnit.SECONDS)
                       .isEqual(reportableTaskResource.get().getAssignmentExpiry()
                                    .truncatedTo(ChronoUnit.SECONDS)));
        assertTrue(taskResource.get().getDueDateTime().truncatedTo(ChronoUnit.SECONDS)
                       .isEqual(reportableTaskResource.get().getDueDateTime()
                                    .truncatedTo(ChronoUnit.SECONDS)));

    }

    @Test
    void when_timezone_changes_reconfig_attributes_should_behave_consistently() throws Exception {

        when(clientAccessControlService.hasExclusiveAccess(SERVICE_AUTHORIZATION_TOKEN))
            .thenReturn(true);

        lenient().when(dmnEvaluationService.evaluateTaskConfigurationDmn(
            anyString(),
            anyString(),
            anyString(),
            anyString()
        )).thenReturn(List.of(
            new ConfigurationDmnEvaluationResponse(
                CamundaValue.stringValue("caseName"),
                CamundaValue.stringValue("Value"),
                CamundaValue.booleanValue(true)
            )
        ));
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
        RoleAssignment roleAssignmentResource = taskTestUtils.buildRoleAssignment(
            ASSIGNEE_USER,
            "tribunalCaseworker",
            singletonList("IA")
        );
        List<RoleAssignment> roleAssignmentForAssignee = List.of(roleAssignmentResource);
        when(roleAssignmentService.getRolesByUserId(any())).thenReturn(roleAssignmentForAssignee);
        when(roleAssignmentService.queryRolesForAutoAssignmentByCaseId(any())).thenReturn(roleAssignmentForAssignee);

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        String caseIdToday = UUID.randomUUID().toString();
        OffsetDateTime dueDateTime = OffsetDateTime.now(ZoneOffset.UTC);
        String reconfigTaskId = taskTestUtils.createTaskAndRoleAssignments(CFTTaskState.ASSIGNED, caseIdToday,
                                                                           dueDateTime, ASSIGNEE_USER);
        doNothing().when(taskMandatoryFieldsValidator).validate(any(TaskResource.class));

        mockMvc.perform(
            post("/task/operation")
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(integrationTestUtils.asJsonString(taskTestUtils.taskOperationRequest(
                    MARK_TO_RECONFIGURE, taskTestUtils.markTaskFilters(caseIdToday))))
        ).andExpectAll(
            status().is(HttpStatus.OK.value())
        );

        Optional<TaskResource> optionalMarkReconfigTaskResource = taskResourceRepository.getByTaskId(reconfigTaskId);

        optionalMarkReconfigTaskResource.ifPresentOrElse(
            task -> {
                assertNotNull(task.getReconfigureRequestTime());

                List<TaskHistoryResource> taskHistoryResourceList
                    = miReportingServiceForTest.findByTaskId(task.getTaskId());

                assertFalse(taskHistoryResourceList.isEmpty());

                taskHistoryResourceList = taskHistoryResourceList.stream()
                    .sorted(Comparator.comparing(TaskHistoryResource::getUpdateId))
                    .toList().reversed();

                TaskHistoryResource taskHistoryResource = null;

                assertNotNull(taskHistoryResourceList.get(0).getReconfigureRequestTime());
                taskHistoryResource = taskHistoryResourceList.get(0);


                Assertions.assertThat(task.getReconfigureRequestTime())
                    .isCloseTo(now, Assertions.within(10, ChronoUnit.SECONDS));
                Assertions.assertThat(taskHistoryResource.getReconfigureRequestTime())
                    .isCloseTo(now, Assertions.within(10, ChronoUnit.SECONDS));
            },
            () -> {
                throw new AssertionFailedError("Task not found");
            }
        );

        List<TaskResource> taskResourcesBefore = cftTaskDatabaseService.findByCaseIdOnly(caseIdToday);

        when(dmnEvaluationService.evaluateTaskConfigurationDmn(
            anyString(),
            anyString(),
            anyString(),
            anyString())).thenReturn(taskTestUtils.configurationDmnResponse(true));
        when(dmnEvaluationService.evaluateTaskPermissionsDmn(
            anyString(),
            anyString(),
            anyString(),
            anyString())).thenReturn(taskTestUtils.permissionsResponse());
        when(cftQueryService.getTask(any(), any(), anyList())).thenReturn(Optional.of(taskResourcesBefore.get(0)));

        mockMvc.perform(
            post("/task/operation")
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(integrationTestUtils.asJsonString(taskTestUtils.taskOperationRequest(
                    EXECUTE_RECONFIGURE,
                    taskTestUtils.executeTaskFilters(OffsetDateTime.now().minusSeconds(30L))
                )))
        ).andExpectAll(
            status().is(HttpStatus.OK.value())
        );

        await().ignoreException(AssertionFailedError.class)
            .pollInterval(1, SECONDS)
            .atMost(10, SECONDS)
            .until(
                () -> {
                    Optional<TaskResource> optionalTaskResource = taskResourceRepository.getByTaskId(reconfigTaskId);

                    optionalTaskResource.ifPresentOrElse(
                        task -> {
                            assertNotNull(task.getLastReconfigurationTime());

                            List<TaskHistoryResource> taskHistoryResourceList
                                = miReportingServiceForTest.findByTaskId(task.getTaskId());

                            assertFalse(taskHistoryResourceList.isEmpty());

                            taskHistoryResourceList = taskHistoryResourceList.stream()
                                .sorted(Comparator.comparing(TaskHistoryResource::getUpdateId))
                                .toList().reversed();

                            assertNotNull(taskHistoryResourceList.get(0).getLastReconfigurationTime());
                            TaskHistoryResource taskHistoryResource = taskHistoryResourceList.get(0);

                            Assertions.assertThat(task.getLastReconfigurationTime())
                                .isCloseTo(now, Assertions.within(10, ChronoUnit.SECONDS));
                            Assertions.assertThat(taskHistoryResource.getLastReconfigurationTime())
                                .isCloseTo(now, Assertions.within(10, ChronoUnit.SECONDS));
                        },
                        () -> {
                            throw new AssertionFailedError("Task not found");
                        }
                    );

                    return true;
                });

    }


    private String initiateTask(ZonedDateTime createdDate) throws Exception {

        mockServices = new ServiceMocks(
            idamWebApi,
            serviceAuthorisationApi,
            camundaServiceApi,
            roleAssignmentServiceApi
        );

        mockServices.mockServiceAPIs();
        when(idamWebApi.userInfo(any())).thenReturn(UserInfo.builder().uid("system_user1").build());

        String taskId = UUID.randomUUID().toString();

        String initiationEndPoint = String.format("/task/%s/initiation", taskId);

        when(clientAccessControlService.hasExclusiveAccess(SERVICE_AUTHORIZATION_TOKEN))
            .thenReturn(true);

        when(caseDetails.getCaseType()).thenReturn("Asylum");
        when(caseDetails.getJurisdiction()).thenReturn("IA");
        when(caseDetails.getSecurityClassification()).thenReturn(("PUBLIC"));

        when(ccdDataServiceApi.getCase(any(), any(), any()))
            .thenReturn(caseDetails);

        CaseDetails caseDetails = new CaseDetails(
            "IA",
            "Asylum",
            SecurityClassification.PUBLIC.getSecurityClassification(),
            Map.of("caseAccessCategory", "categoryA,categoryC")
        );
        lenient().when(ccdDataService.getCaseData(anyString())).thenReturn(caseDetails);

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

        when(launchDarklyFeatureFlagProvider.getJsonValue(any(), any())).thenReturn(null);


        when(roleAssignmentServiceApi.queryRoleAssignments(any(), any(), any(), any(), any()))
            .thenReturn(ResponseEntity.ok()
                .header(TOTAL_RECORDS, "1")
                .body(new RoleAssignmentResource(
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
                        .build()))));
        ZonedDateTime dueDate = createdDate.plusDays(1);
        ZonedDateTime assignmentExpery = createdDate.plusDays(7);
        ZonedDateTime nextHearingDate = createdDate.plusDays(30);
        String formattedCreatedDate = CAMUNDA_DATA_TIME_FORMATTER.format(createdDate);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);
        String formattedAssignmentExpiry = CAMUNDA_DATA_TIME_FORMATTER.format(assignmentExpery);
        String formattedNextHearingDate = CAMUNDA_DATA_TIME_FORMATTER.format(nextHearingDate);


        Map<String, Object> taskAttributes = Map.of(
            TASK_TYPE.value(), "followUpOverdueReasonsForAppeal",
            TASK_NAME.value(), "aTaskName",
            TITLE.value(), "A test task",
            CASE_ID.value(), "someCaseId",
            DUE_DATE.value(), formattedDueDate,
            CREATED.value(), formattedCreatedDate,
            NEXT_HEARING_DATE.value(),formattedNextHearingDate,
            ASSIGNMENT_EXPIRY.value(), formattedAssignmentExpiry
        );

        InitiateTaskRequestMap req = new InitiateTaskRequestMap(INITIATION, taskAttributes);

        mockMvc
            .perform(post(initiationEndPoint)
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(APPLICATION_JSON_VALUE)
                .content(integrationTestUtils.asJsonString(req)))
            .andDo(MockMvcResultHandlers.print())
            .andExpectAll(
                status().isCreated(),
                content().contentType(APPLICATION_JSON_VALUE)
            );
        return taskId;
    }

}
