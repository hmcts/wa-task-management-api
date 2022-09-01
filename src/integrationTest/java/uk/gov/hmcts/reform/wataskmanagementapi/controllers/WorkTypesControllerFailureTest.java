package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import org.hibernate.exception.JDBCConnectionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultMatcher;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootIntegrationBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAttributeDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.response.RoleAssignmentResource;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.RoleAssignmentServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTWorkTypeDatabaseService;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.zalando.problem.Status.SERVICE_UNAVAILABLE;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.SERVICE_AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.IDAM_AUTHORIZATION_TOKEN;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.IDAM_USER_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.SERVICE_AUTHORIZATION_TOKEN;

class WorkTypesControllerFailureTest extends SpringBootIntegrationBaseTest {
    private static final String ENDPOINT_PATH = "/work-types";

    @MockBean
    private IdamWebApi idamWebApi;
    @MockBean
    private CamundaServiceApi camundaServiceApi;
    @MockBean
    private RoleAssignmentServiceApi roleAssignmentServiceApi;
    @MockBean
    private ServiceAuthorisationApi serviceAuthorisationApi;
    @MockBean
    private CFTWorkTypeDatabaseService cftWorkTypeDatabaseService;
    @MockBean
    private AuthTokenGenerator serviceAuthTokenGenerator;
    private ServiceMocks mockServices;
    private UserInfo mockedUserInfo;

    @BeforeEach
    void setUp() {
        mockedUserInfo = UserInfo.builder().uid(IDAM_USER_ID).name("someUser").build();
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
    void should_return_500_when_database_connection_not_available_without_filter() throws Exception {
        final List<String> roleNames = singletonList("tribunal-caseworker");
        List<RoleAssignment> allTestRoles = mockServices.createTestRoleAssignments(roleNames);
        when(roleAssignmentServiceApi.getRolesForUser(any(), anyString(), anyString()))
            .thenReturn(new RoleAssignmentResource(allTestRoles));
        when(cftWorkTypeDatabaseService.getAllWorkTypes()).thenThrow(JDBCConnectionException.class);

        mockMvc.perform(
            get(ENDPOINT_PATH)
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
        ).andExpect(
            ResultMatcher.matchAll(
                content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
                status().isServiceUnavailable(),
                jsonPath("$.type").value("https://github.com/hmcts/wa-task-management-api/problem/service-unavailable"),
                jsonPath("$.title").value("Service Unavailable"),
                jsonPath("$.status").value(SERVICE_UNAVAILABLE.getStatusCode()),
                jsonPath("$.detail").value("Database is unavailable.")
            ));
    }

    @Test
    void should_return_500_when_database_connection_not_available_with_filter() throws Exception {

        when(cftWorkTypeDatabaseService.getWorkTypes(anySet())).thenThrow(JDBCConnectionException.class);

        final List<String> roleNames = singletonList("tribunal-caseworker");

        // Role attribute is IA
        Map<String, String> roleAttributes = new HashMap<>();
        roleAttributes.put(RoleAttributeDefinition.JURISDICTION.value(), "IA");
        roleAttributes.put(RoleAttributeDefinition.WORK_TYPES.value(), "hearing_work,upper_tribunal");

        List<RoleAssignment> allTestRoles = new ArrayList<>();
        roleNames.forEach(roleName -> asList(RoleType.ORGANISATION, RoleType.CASE)
            .forEach(roleType -> {
                RoleAssignment roleAssignment = mockServices.createBaseAssignment(
                    UUID.randomUUID().toString(), "tribunal-caseworker",
                    roleType,
                    Classification.PUBLIC,
                    roleAttributes
                );
                allTestRoles.add(roleAssignment);
            }));
        when(roleAssignmentServiceApi.getRolesForUser(any(), anyString(), anyString()))
            .thenReturn(new RoleAssignmentResource(allTestRoles));

        mockMvc.perform(
            get(ENDPOINT_PATH + "?filter-by-user=true")
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
        ).andExpect(
            ResultMatcher.matchAll(
                content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
                status().isServiceUnavailable(),
                jsonPath("$.type").value("https://github.com/hmcts/wa-task-management-api/problem/service-unavailable"),
                jsonPath("$.title").value("Service Unavailable"),
                jsonPath("$.status").value(SERVICE_UNAVAILABLE.getStatusCode()),
                jsonPath("$.detail").value("Database is unavailable.")
            ));
    }

    @Test
    void should_return_403_when_role_assignment_is_empty() throws Exception {
        when(cftWorkTypeDatabaseService.findById(anyString())).thenCallRealMethod();
        when(roleAssignmentServiceApi.getRolesForUser(any(), anyString(), anyString()))
            .thenReturn(new RoleAssignmentResource(List.of()));
        when(cftWorkTypeDatabaseService.getAllWorkTypes()).thenCallRealMethod();
        mockMvc.perform(
            get(ENDPOINT_PATH)
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        ).andExpect(
            ResultMatcher.matchAll(
                status().isForbidden(),
                jsonPath("$.message").isNotEmpty(),
                jsonPath("$.message").value("User did not have sufficient permissions to perform this action")
            ));
    }
}

