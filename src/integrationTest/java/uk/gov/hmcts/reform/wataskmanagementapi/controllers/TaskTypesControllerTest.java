package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.response.RoleAssignmentResource;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.RoleAssignmentServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.config.IntegrationTest;
import uk.gov.hmcts.reform.wataskmanagementapi.services.DmnEvaluationService;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.ControllerTestStubs;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks;

import java.util.HashMap;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
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
@IntegrationTest
@AutoConfigureMockMvc(addFilters = false)
@TestInstance(PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TaskTypesControllerTest {
    private static final String ENDPOINT_PATH = "/task/task-types";
    private static final String DMN_NAME = "Task Types DMN";

    @Autowired
    private IdamWebApi idamWebApi;

    @MockitoBean
    private CamundaServiceApi camundaServiceApi;

    @MockitoBean
    private RoleAssignmentServiceApi roleAssignmentServiceApi;

    @MockitoBean
    private ServiceAuthorisationApi serviceAuthorisationApi;

    @MockitoBean
    private AuthTokenGenerator serviceAuthTokenGenerator;

    @Autowired
    private DmnEvaluationService dmnEvaluationService;

    @Autowired
    protected MockMvc mockMvc;

    private ServiceMocks mockServices;
    private UserInfo mockedUserInfo;

    @BeforeAll
    void setUp() {
        mockServices = new ServiceMocks(
            idamWebApi,
            serviceAuthorisationApi,
            camundaServiceApi,
            roleAssignmentServiceApi
        );
    }

    @BeforeEach
    void beforeEach() {
        mockedUserInfo = ControllerTestStubs.stubUserInfo(
            idamWebApi,
            serviceAuthTokenGenerator,
            IDAM_AUTHORIZATION_TOKEN,
            SERVICE_AUTHORIZATION_TOKEN,
            ServiceMocks.IDAM_USER_ID,
            "someUser"
        );
    }

    @Test
    void should_return_all_task_types() throws Exception {

        String jurisdiction = "wa";
        final List<String> roleNames = singletonList("tribunal-caseworker");
        List<RoleAssignment> allTestRoles = mockServices.createTestRoleAssignments(roleNames);

        when(roleAssignmentServiceApi.getRolesForUser(any(), anyString(), anyString()))
            .thenReturn(new RoleAssignmentResource(allTestRoles));

        mockMvc.perform(
            get(ENDPOINT_PATH + "?jurisdiction=" + jurisdiction)
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        ).andDo(
            mvcResult -> log.info("task_types: {}", mvcResult.getResponse().getContentAsString())
        ).andExpectAll(
            status().isOk(),
            content().contentType(APPLICATION_JSON_VALUE)
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
            jsonPath("$.type")
                .value("https://github.com/hmcts/wa-task-management-api/problem/constraint-violation"),
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
    void should_return_502_with_application_problem_response_when_camunda_api_throws_an_exception() throws Exception {
        final List<String> roleNames = singletonList("tribunal-caseworker");
        List<RoleAssignment> allTestRoles = mockServices.createTestRoleAssignments(roleNames);

        Request request = Request.create(Request.HttpMethod.POST, "url",
                                         new HashMap<>(), null, new RequestTemplate());

        FeignException exception = new FeignException.BadGateway(
            "Camunda is down.",
            request,
            null,
            null);

        when(roleAssignmentServiceApi.getRolesForUser(any(), anyString(), anyString()))
            .thenReturn(new RoleAssignmentResource(allTestRoles));

        when(camundaServiceApi.getTaskTypesDmnTable(
            anyString(),
            anyString(),
            anyString(),
            anyBoolean()
        )).thenThrow(exception);

        mockMvc.perform(
            get(ENDPOINT_PATH + "?jurisdiction=some_jurisdiction")
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        ).andExpectAll(
            status().isBadGateway(),
            content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
            jsonPath("$.type")
                .value("https://github.com/hmcts/wa-task-management-api/problem/downstream-dependency-error"),
            jsonPath("$.title")
                .value("Downstream Dependency Error"),
            jsonPath("$.status")
                .value(502),
            jsonPath("$.detail")
                .value("Downstream dependency did not respond as expected and the request could not be completed."
                           + " Message from downstream system: Camunda is down.")
        );
    }

    @Test
    void should_return_503_with_application_problem_response_when_camunda_api_is_not_available() throws Exception {
        final List<String> roleNames = singletonList("tribunal-caseworker");
        List<RoleAssignment> allTestRoles = mockServices.createTestRoleAssignments(roleNames);

        when(roleAssignmentServiceApi.getRolesForUser(any(), anyString(), anyString()))
            .thenReturn(new RoleAssignmentResource(allTestRoles));

        when(camundaServiceApi.getTaskTypesDmnTable(
            anyString(),
            anyString(),
            anyString(),
            anyBoolean()
        )).thenThrow(FeignException.ServiceUnavailable.class);

        mockMvc.perform(
            get(ENDPOINT_PATH + "?jurisdiction=sscs")
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        ).andExpectAll(
            status().isServiceUnavailable(),
            content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
            jsonPath("$.type")
                .value("https://github.com/hmcts/wa-task-management-api/problem/service-unavailable"),
            jsonPath("$.title").value("Service Unavailable"),
            jsonPath("$.status").value(503),
            jsonPath("$.detail").value(
                "Service unavailable.")
        );
    }
}
