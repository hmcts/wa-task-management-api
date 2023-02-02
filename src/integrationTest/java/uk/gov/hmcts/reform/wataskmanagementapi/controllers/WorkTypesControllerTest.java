package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import feign.FeignException;
import org.hibernate.exception.JDBCConnectionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootIntegrationBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAttributeDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.response.RoleAssignmentResource;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.RoleAssignmentServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.GetWorkTypesResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.task.WorkType;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTWorkTypeDatabaseService;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.SERVICE_AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.IDAM_AUTHORIZATION_TOKEN;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.SERVICE_AUTHORIZATION_TOKEN;

@ExtendWith(MockitoExtension.class)
class WorkTypesControllerTest extends SpringBootIntegrationBaseTest {
    private static final String ENDPOINT_PATH = "/work-types";

    @MockBean
    private IdamWebApi idamWebApi;
    @MockBean
    private CamundaServiceApi camundaServiceApi;
    @SpyBean
    private CFTWorkTypeDatabaseService cftWorkTypeDatabaseService;
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
    void should_return_all_work_types_when_filter_is_not_provided() throws Exception {
        when(cftWorkTypeDatabaseService.findById(anyString())).thenCallRealMethod();
        final List<String> roleNames = singletonList("tribunal-caseworker");
        List<RoleAssignment> allTestRoles = mockServices.createTestRoleAssignments(roleNames);
        when(roleAssignmentServiceApi.getRolesForUser(any(), anyString(), anyString()))
            .thenReturn(new RoleAssignmentResource(allTestRoles));
        when(cftWorkTypeDatabaseService.getAllWorkTypes()).thenCallRealMethod();

        MvcResult response = mockMvc.perform(
            get(ENDPOINT_PATH)
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
        ).andExpectAll(
            status().isOk(),
            jsonPath("$.work_types").isNotEmpty(),
            jsonPath("$.work_types.length()").value(11)
        ).andReturn();


        runWorkTypeAssertion(getExpectedWorkTypes(), response);
    }

    @Test
    void should_return_all_work_types_when_filter_is_false() throws Exception {
        when(cftWorkTypeDatabaseService.findById(anyString())).thenCallRealMethod();
        final List<String> roleNames = singletonList("tribunal-caseworker");
        List<RoleAssignment> allTestRoles = mockServices.createTestRoleAssignments(roleNames);
        when(roleAssignmentServiceApi.getRolesForUser(any(), anyString(), anyString()))
            .thenReturn(new RoleAssignmentResource(allTestRoles));

        when(cftWorkTypeDatabaseService.getAllWorkTypes()).thenCallRealMethod();
        mockMvc.perform(
            get(ENDPOINT_PATH + "?filter-by-user=false")
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        ).andExpectAll(
            status().isOk(),
            jsonPath("$.work_types").isNotEmpty(),
            jsonPath("$.work_types.length()").value(11)
        );
    }

    @Test
    void should_return_a_valid_work_type_list_when_user_has_work_types() throws Exception {
        final List<String> roleNames = singletonList("tribunal-caseworker");


        // Role attribute is IA
        Map<String, String> roleAttributes = new HashMap<>();
        roleAttributes.put(RoleAttributeDefinition.JURISDICTION.value(), "IA");
        roleAttributes.put(RoleAttributeDefinition.WORK_TYPES.value(), "hearing_work,upper_tribunal");

        List<RoleAssignment> allTestRoles = mockServices
            .createTestRoleAssignmentsWithRoleAttributes(roleNames, roleAttributes);

        when(cftWorkTypeDatabaseService.getWorkTypes(Set.of("hearing_work", "upper_tribunal")))
            .thenReturn(List.of(new WorkType("hearing_work", "Hearing work"),
                new WorkType("upper_tribunal", "Upper Tribunal")));
        when(roleAssignmentServiceApi.getRolesForUser(any(), anyString(), anyString()))
            .thenReturn(new RoleAssignmentResource(allTestRoles));


        MvcResult response = mockMvc.perform(
            get(ENDPOINT_PATH + "?filter-by-user=true")
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
        ).andReturn();

        WorkType expectedWorkType1 = new WorkType("hearing_work", "Hearing work");
        WorkType expectedWorkType2 = new WorkType("upper_tribunal", "Upper Tribunal");

        GetWorkTypesResponse expectedResponse = new GetWorkTypesResponse(asList(expectedWorkType1, expectedWorkType2));

        runWorkTypeAssertion(expectedResponse, response);
    }

    @Test
    void should_return_a_valid_work_type_list_when_user_has_work_types_leading_spaces() throws Exception {
        final List<String> roleNames = singletonList("tribunal-caseworker");


        // Role attribute is IA
        Map<String, String> roleAttributes = new HashMap<>();
        roleAttributes.put(RoleAttributeDefinition.JURISDICTION.value(), "IA");
        roleAttributes.put(RoleAttributeDefinition.WORK_TYPES.value(), "hearing_work, upper_tribunal");

        List<RoleAssignment> allTestRoles = mockServices
            .createTestRoleAssignmentsWithRoleAttributes(roleNames, roleAttributes);

        when(cftWorkTypeDatabaseService.getWorkTypes(Set.of("hearing_work", "upper_tribunal")))
            .thenReturn(List.of(new WorkType("hearing_work", "Hearing work"),
                new WorkType("upper_tribunal", "Upper Tribunal")));
        when(roleAssignmentServiceApi.getRolesForUser(any(), anyString(), anyString()))
            .thenReturn(new RoleAssignmentResource(allTestRoles));

        MvcResult response = mockMvc.perform(
            get(ENDPOINT_PATH + "?filter-by-user=true")
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
        ).andReturn();

        WorkType expectedWorkType1 = new WorkType("hearing_work", "Hearing work");
        WorkType expectedWorkType2 = new WorkType("upper_tribunal", "Upper Tribunal");

        GetWorkTypesResponse expectedResponse = new GetWorkTypesResponse(asList(expectedWorkType1, expectedWorkType2));

        runWorkTypeAssertion(expectedResponse, response);
    }

    @Test
    void should_return_empty_list_when_work_types_are_given() throws Exception {
        final List<String> roleNames = singletonList("tribunal-caseworker");

        List<RoleAssignment> allTestRoles = mockServices.createTestRoleAssignments(roleNames);

        when(roleAssignmentServiceApi.getRolesForUser(any(), anyString(), anyString()))
            .thenReturn(new RoleAssignmentResource(allTestRoles));

        MvcResult response = mockMvc.perform(
            get(ENDPOINT_PATH + "?filter-by-user=true")
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
        ).andReturn();

        runWorkTypeAssertion(new GetWorkTypesResponse(emptyList()), response);
    }

    @DisplayName("Should return 503 when role assignment service is down")
    @Test
    void should_return_503_with_application_problem_response_when_role_assignment_is_down() throws Exception {

        doThrow(FeignException.ServiceUnavailable.class)
            .when(roleAssignmentServiceApi).getRolesForUser(anyString(), anyString(), anyString());

        mockMvc.perform(
                get(ENDPOINT_PATH + "?filter-by-user=true")
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andExpectAll(
                status().isServiceUnavailable(),
                content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
                jsonPath("$.type").value("https://github.com/hmcts/wa-task-management-api/problem/service-unavailable"),
                jsonPath("$.title").value("Service Unavailable"),
                jsonPath("$.status").value(503),
                jsonPath("$.detail").value("Service unavailable.")
            );
    }

    @Test
    void should_return_503_with_application_problem_response_when_db_is_not_available() throws Exception {
        final List<String> roleNames = singletonList("tribunal-caseworker");

        // Role attribute is IA
        Map<String, String> roleAttributes = new HashMap<>();
        roleAttributes.put(RoleAttributeDefinition.JURISDICTION.value(), "IA");
        roleAttributes.put(RoleAttributeDefinition.WORK_TYPES.value(), "hearing_work,upper_tribunal");
        List<RoleAssignment> allTestRoles =
            mockServices.createTestRoleAssignmentsWithRoleAttributes(roleNames, roleAttributes);

        when(roleAssignmentServiceApi.getRolesForUser(any(), anyString(), anyString()))
            .thenReturn(new RoleAssignmentResource(allTestRoles));

        doThrow(JDBCConnectionException.class)
            .when(cftWorkTypeDatabaseService).getWorkTypes(anySet());
        mockMvc.perform(
            get(ENDPOINT_PATH + "?filter-by-user=true")
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

    @Test
    void should_return_502_when_invalid_user_access() throws Exception {

        final List<String> roleNames = singletonList("tribunal-caseworker");

        // Role attribute is IA
        Map<String, String> roleAttributes = new HashMap<>();
        roleAttributes.put(RoleAttributeDefinition.JURISDICTION.value(), "IA");
        roleAttributes.put(RoleAttributeDefinition.WORK_TYPES.value(), "hearing_work,upper_tribunal");
        mockServices.createTestRoleAssignmentsWithRoleAttributes(roleNames, roleAttributes);

        when(roleAssignmentServiceApi.getRolesForUser(any(), anyString(), anyString()))
            .thenThrow(FeignException.Unauthorized.class);

        mockMvc.perform(
            get(ENDPOINT_PATH + "?filter-by-user=true")
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
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
                .value("Downstream dependency did not respond as expected and the request could not be completed.")
        );

    }

    private GetWorkTypesResponse getExpectedWorkTypes() {
        WorkType expectedWorkType1 = new WorkType("hearing_work", "Hearing work");
        WorkType expectedWorkType2 = new WorkType("upper_tribunal", "Upper Tribunal");
        WorkType expectedWorkType3 = new WorkType("routine_work", "Routine work");
        WorkType expectedWorkType4 = new WorkType("decision_making_work", "Decision-making work");
        WorkType expectedWorkType5 = new WorkType("applications", "Applications");
        WorkType expectedWorkType6 = new WorkType("priority", "Priority");
        WorkType expectedWorkType7 = new WorkType("access_requests", "Access requests");
        WorkType expectedWorkType8 = new WorkType("error_management", "Error management");
        WorkType expectedWorkType9 = new WorkType("review_case", "Review Case");
        WorkType expectedWorkType10 = new WorkType("evidence", "Evidence");
        WorkType expectedWorkType11 = new WorkType("follow_up", "Follow Up");

        return new GetWorkTypesResponse(asList(
            expectedWorkType1, expectedWorkType2, expectedWorkType3,
            expectedWorkType4, expectedWorkType5, expectedWorkType6,
            expectedWorkType7, expectedWorkType8, expectedWorkType9,
            expectedWorkType10, expectedWorkType11
        ));
    }

    private void runWorkTypeAssertion(GetWorkTypesResponse expectedResponse, MvcResult response)
        throws JsonProcessingException, UnsupportedEncodingException {

        assertNotNull(expectedResponse);

        assertNotNull(response.getResponse().getContentAsString());

        assertEquals(
            asJsonString(expectedResponse),
            response.getResponse().getContentAsString()
        );
    }

}

