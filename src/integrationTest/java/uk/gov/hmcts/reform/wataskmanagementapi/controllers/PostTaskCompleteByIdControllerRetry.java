package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import feign.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.RoleAssignmentHelper;
import uk.gov.hmcts.reform.wataskmanagementapi.RoleAssignmentHelper.RoleAssignmentAttribute;
import uk.gov.hmcts.reform.wataskmanagementapi.RoleAssignmentHelper.RoleAssignmentRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.IdamService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.Token;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.restrict.ClientAccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.response.RoleAssignmentResource;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.RoleAssignmentServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.SecurityClassification;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.enums.TestRolesWithGrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskRoleResource;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskDatabaseService;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.IntegrationTestUtils;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.ASSIGNED;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.SERVICE_AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.IDAM_AUTHORIZATION_TOKEN;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.IDAM_USER_EMAIL;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.IDAM_USER_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.SERVICE_AUTHORIZATION_TOKEN;

@SpringBootTest
@ActiveProfiles({"integration"})
@AutoConfigureMockMvc(addFilters = false)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SuppressWarnings("checkstyle:LineLength")
class PostTaskCompleteByIdControllerRetry {

    private static final String ENDPOINT_PATH = "/task/%s/complete";

    private static final WireMockServer wireMockServer = new WireMockServer(options().dynamicPort());

    static {
        wireMockServer.start();
        System.setProperty("camunda.url", wireMockServer.baseUrl());

        configureFor("localhost", wireMockServer.port());
    }

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("camunda.url", () -> wireMockServer.baseUrl());
    }

    @AfterAll
    static void tearDownAll() {
        wireMockServer.stop();
    }

    @MockitoBean
    private IdamWebApi idamWebApi;
    @MockitoBean
    private AuthTokenGenerator authTokenGenerator;
    @MockitoBean
    private RoleAssignmentServiceApi roleAssignmentServiceApi;
    @MockitoBean
    private ServiceAuthorisationApi serviceAuthorisationApi;
    @MockitoBean
    private IdamService idamService;
    @MockitoBean
    private ClientAccessControlService clientAccessControlService;

    @Autowired
    private CFTTaskDatabaseService cftTaskDatabaseService;
    @Autowired
    MockMvc mockMvc;
    @Autowired
    IntegrationTestUtils integrationTestUtils;

    @Mock
    private UserInfo mockedUserInfo;

    private ServiceMocks mockServices;
    private RoleAssignmentHelper roleAssignmentHelper = new RoleAssignmentHelper();

    private String taskId;
    private String endpointBeingTested;

    @BeforeAll
    void setUp() {
        mockServices = new ServiceMocks(
            idamWebApi,
            serviceAuthorisationApi,
            null,
            roleAssignmentServiceApi
        );
    }

    @BeforeEach
    void beforeEach() {
        taskId = UUID.randomUUID().toString();
        endpointBeingTested = String.format(ENDPOINT_PATH, taskId);

        when(authTokenGenerator.generate()).thenReturn(IDAM_AUTHORIZATION_TOKEN);
        lenient().when(mockedUserInfo.getUid()).thenReturn(IDAM_USER_ID);
        lenient().when(mockedUserInfo.getEmail()).thenReturn(IDAM_USER_EMAIL);

        when(clientAccessControlService.hasPrivilegedAccess(eq(SERVICE_AUTHORIZATION_TOKEN), any()))
            .thenReturn(true);

        wireMockServer.resetAll();
    }

    @Test
    void should_retry_camunda_complete_api_when_fails() throws Exception {
        mockServices.mockUserInfo();

        List<RoleAssignment> roleAssignments = new ArrayList<>();
        RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction("IA")
                    .caseType("Asylum")
                    .caseId("retryCaseId1")
                    .build()
            )
            .build();
        roleAssignmentHelper.createRoleAssignment(roleAssignments, roleAssignmentRequest);
        RoleAssignmentResource accessControlResponse = new RoleAssignmentResource(roleAssignments);

        TaskRoleResource taskRoleResource = new TaskRoleResource(
            TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleName(),
            false, true, true, false, false, false,
            new String[]{}, 1, false,
            TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleCategory().name()
        );
        insertDummyTaskInDb("IA", "Asylum", "retryCaseId1", taskId, taskRoleResource);

        when(idamService.getUserInfo(IDAM_AUTHORIZATION_TOKEN)).thenReturn(mockedUserInfo);
        when(roleAssignmentServiceApi.getRolesForUser(any(), any(), any()))
            .thenReturn(accessControlResponse);

        when(idamWebApi.token(any())).thenReturn(new Token(IDAM_AUTHORIZATION_TOKEN, "scope"));
        when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);
        wireMockServer.stubFor(post(urlPathMatching("/task/.*/localVariables"))
                                   .willReturn(aResponse().withStatus(204)));

        wireMockServer.stubFor(post(urlPathEqualTo("/history/variable-instance"))
                                   .willReturn(aResponse()
                                                   .withStatus(200)
                                                   .withHeader("Content-Type", APPLICATION_JSON_VALUE)
                                                   .withBody("[]")));

        wireMockServer.stubFor(post(urlPathEqualTo("/task/" + taskId + "/complete"))
                                   .inScenario("camunda-complete-retry")
                                   .whenScenarioStateIs(Scenario.STARTED)
                                   .willReturn(aResponse().withStatus(500))
                                   .willSetStateTo("second"));

        wireMockServer.stubFor(post(urlPathEqualTo("/task/" + taskId + "/complete"))
                                   .inScenario("camunda-complete-retry")
                                   .whenScenarioStateIs("second")
                                   .willReturn(aResponse().withStatus(500))
                                   .willSetStateTo("third"));

        wireMockServer.stubFor(post(urlPathEqualTo("/task/" + taskId + "/complete"))
                                   .inScenario("camunda-complete-retry")
                                   .whenScenarioStateIs("third")
                                   .willReturn(aResponse().withStatus(204)));

        mockMvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(endpointBeingTested)
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        ).andExpect(status().is2xxSuccessful());

        wireMockServer.verify(3, postRequestedFor(urlPathEqualTo("/task/" + taskId + "/complete")));
    }

    @Test
    void should_retry_camunda_complete_api_with_new_token_when_fails() throws Exception {
        mockServices.mockUserInfo();

        AtomicInteger tokenCounter = new AtomicInteger(1);
        when(authTokenGenerator.generate()).thenAnswer(invocation -> "S2S_TOKEN_" + tokenCounter.getAndIncrement());

        List<RoleAssignment> roleAssignments = new ArrayList<>();
        RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction("IA")
                    .caseType("Asylum")
                    .caseId("retryCaseId401")
                    .build()
            )
            .build();
        roleAssignmentHelper.createRoleAssignment(roleAssignments, roleAssignmentRequest);
        RoleAssignmentResource accessControlResponse = new RoleAssignmentResource(roleAssignments);

        TaskRoleResource taskRoleResource = new TaskRoleResource(
            TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleName(),
            false, true, true, false, false, false,
            new String[]{}, 1, false,
            TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleCategory().name()
        );
        insertDummyTaskInDb("IA", "Asylum", "retryCaseId401", taskId, taskRoleResource);

        when(idamService.getUserInfo(IDAM_AUTHORIZATION_TOKEN)).thenReturn(mockedUserInfo);
        when(roleAssignmentServiceApi.getRolesForUser(any(), any(), any()))
            .thenReturn(accessControlResponse);

        when(idamWebApi.token(any())).thenReturn(new Token(IDAM_AUTHORIZATION_TOKEN, "scope"));
        AtomicInteger s2sCounter = new AtomicInteger(1);
        when(serviceAuthorisationApi.serviceToken(any()))
            .thenAnswer(invocation -> "S2S_TOKEN_" + s2sCounter.getAndIncrement());

        wireMockServer.stubFor(post(urlPathMatching("/task/.*/localVariables"))
                                   .willReturn(aResponse().withStatus(204)));

        wireMockServer.stubFor(post(urlPathEqualTo("/history/variable-instance"))
                                   .willReturn(aResponse()
                                                   .withStatus(200)
                                                   .withHeader("Content-Type", APPLICATION_JSON_VALUE)
                                                   .withBody("[]")));

        wireMockServer.stubFor(post(urlPathEqualTo("/task/" + taskId + "/complete"))
                                   .inScenario("camunda-complete-retry-401")
                                   .whenScenarioStateIs(Scenario.STARTED)
                                   .willReturn(aResponse().withStatus(401))
                                   .willSetStateTo("second"));

        wireMockServer.stubFor(post(urlPathEqualTo("/task/" + taskId + "/complete"))
                                   .inScenario("camunda-complete-retry-401")
                                   .whenScenarioStateIs("second")
                                   .willReturn(aResponse().withStatus(204)));

        mockMvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(endpointBeingTested)
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        ).andExpect(status().is2xxSuccessful());

        verify(2, postRequestedFor(urlPathEqualTo("/task/" + taskId + "/complete")));

        var completeEvents = wireMockServer.getAllServeEvents().stream()
            .filter(e -> e.getRequest().getUrl().equals("/task/" + taskId + "/complete"))
            .toList();

        assertThat(completeEvents)
            .hasSize(2);

        String firstHeader = completeEvents.get(0).getRequest().getHeader("ServiceAuthorization");
        String secondHeader = completeEvents.get(1).getRequest().getHeader("ServiceAuthorization");

        assertThat(firstHeader)
            .as("ServiceAuthorization header must be present on first /complete attempt")
            .isNotBlank();

        assertThat(secondHeader)
            .as("ServiceAuthorization header must be present on retry /complete attempt")
            .isNotBlank();

        assertThat(secondHeader)
            .as("Retry should use a different ServiceAuthorization token than the first /complete attempt")
            .isNotEqualTo(firstHeader);

        wireMockServer.verify(2, postRequestedFor(urlPathEqualTo("/task/" + taskId + "/complete")));

        org.mockito.Mockito.verify(authTokenGenerator, org.mockito.Mockito.atLeast(2)).generate();
    }

    private void insertDummyTaskInDb(String jurisdiction, String caseType, String caseId, String taskId, TaskRoleResource taskRoleResource) {
        TaskResource taskResource = new TaskResource(
            taskId,
            "someTaskName",
            "someTaskType",
            ASSIGNED
        );
        taskResource.setCreated(OffsetDateTime.now());
        taskResource.setDueDateTime(OffsetDateTime.now());
        taskResource.setJurisdiction(jurisdiction);
        taskResource.setCaseTypeId(caseType);
        taskResource.setSecurityClassification(SecurityClassification.PUBLIC);
        taskResource.setLocation("765324");
        taskResource.setLocationName("Taylor House");
        taskResource.setRegion("TestRegion");
        // use the passed caseId instead of a hard-coded value
        taskResource.setCaseId(caseId);
        taskResource.setAssignee(IDAM_USER_ID);

        taskRoleResource.setTaskId(taskId);
        taskResource.setTaskRoleResources(Set.of(taskRoleResource));

        cftTaskDatabaseService.saveTask(taskResource);
    }

    /**
     * Optional: turn Feign logging up for this test if you want to see the retries in logs.
     * You can delete this if you don't need it.
     */
    @TestConfiguration
    static class FeignLogConfig {
        @Bean
        Logger.Level feignLoggerLevel() {
            return Logger.Level.FULL;
        }
    }
}

