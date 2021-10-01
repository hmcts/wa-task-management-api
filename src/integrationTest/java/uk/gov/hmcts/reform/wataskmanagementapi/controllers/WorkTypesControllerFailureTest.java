package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import org.hibernate.exception.JDBCConnectionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.ResultMatcher;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootIntegrationBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.AccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAttributeDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.repository.WorkTypeResourceRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.RoleAssignmentServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.SERVICE_AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.IDAM_AUTHORIZATION_TOKEN;
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
    private AccessControlService  accessControlService;
    @MockBean
    private ServiceAuthorisationApi serviceAuthorisationApi;
    @MockBean
    private WorkTypeResourceRepository workTypeResourceRepository;

    private ServiceMocks mockServices;

    @BeforeEach
    void setUp() {
        mockServices = new ServiceMocks(
            idamWebApi,
            serviceAuthorisationApi,
            camundaServiceApi,
            roleAssignmentServiceApi
        );
    }

    @Test
    void should_return_500_when_database_connection_not_available_without_filter() throws Exception {

        when(workTypeResourceRepository.findAll()).thenThrow(JDBCConnectionException.class);

        mockMvc.perform(
            get(ENDPOINT_PATH)
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
        ).andExpect(
            ResultMatcher.matchAll(
                status().is5xxServerError()
            ));

    }

    @Test
    void should_return_500_when_database_connection_not_available_with_filter() throws Exception {

        when(workTypeResourceRepository.findById(anyString())).thenThrow(JDBCConnectionException.class);

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
        final UserInfo userInfo = UserInfo.builder().uid(ServiceMocks.IDAM_USER_ID).build();
        AccessControlResponse accessControlResponse = new AccessControlResponse(userInfo, allTestRoles);

        when(accessControlService.getRoles(any()))
            .thenReturn(accessControlResponse);

        mockMvc.perform(
            get(ENDPOINT_PATH + "?filter-by-user=true")
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
        ).andExpect(
            ResultMatcher.matchAll(
                status().is5xxServerError()
            ));

    }
}

