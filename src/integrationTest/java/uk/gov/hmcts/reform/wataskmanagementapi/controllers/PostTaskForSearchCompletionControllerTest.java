package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import feign.FeignException;
import org.hibernate.query.criteria.internal.CriteriaBuilderImpl;
import org.hibernate.query.criteria.internal.predicate.BooleanAssertionPredicate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootIntegrationBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.SearchEventAndCase;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.Token;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.response.RoleAssignmentResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.ExecutionTypeResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.WorkTypeResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.BusinessContext;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.ExecutionType;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.TaskSystem;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.RoleAssignmentServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.config.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTaskCount;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariable;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableInstance;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.SecurityClassification;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.SERVICE_AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.IDAM_AUTHORIZATION_TOKEN;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.SERVICE_AUTHORIZATION_TOKEN;

class PostTaskForSearchCompletionControllerTest extends SpringBootIntegrationBaseTest {

    @MockBean
    private IdamWebApi idamWebApi;
    @MockBean
    private CamundaServiceApi camundaServiceApi;
    @MockBean
    private AuthTokenGenerator authTokenGenerator;
    @MockBean
    private RoleAssignmentServiceApi roleAssignmentServiceApi;
    @MockBean
    private ServiceAuthorisationApi serviceAuthorisationApi;
    @MockBean
    private EntityManager entityManager;
    @MockBean
    private LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider;
    @Mock
    private CriteriaQuery<TaskResource> criteriaQuery;
    @Mock
    private Root<TaskResource> root;
    @Mock
    private Root<TaskResource> subRoot;
    @Mock
    private CriteriaQuery<Long> countCriteriaQuery;
    @Mock
    private Subquery<TaskResource> subQuery;
    @Mock
    private TypedQuery<TaskResource> query;
    @Mock
    private TypedQuery<Long> countQuery;
    @Mock
    private Predicate predicate;
    @Mock
    private CriteriaBuilder.In<Object> inObject;
    @Mock
    private CriteriaBuilder.In<Object> values;
    @Mock
    private Expression<Long> selection;
    @Mock
    private Path<Object> authorizations;
    @Mock
    private Join<Object, Object> taskRoleResources;
    @Mock
    private Path<Object> path;
    @Mock(extraInterfaces = Serializable.class)
    private CriteriaBuilderImpl builder;

    private ServiceMocks mockServices;
    private String taskId;

    private SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
        "some-caseId",
        "decideAnApplication",
        "ia",
        "asylum"
    );

    @BeforeEach
    void setUp() {
        mockServices = new ServiceMocks(
            idamWebApi,
            serviceAuthorisationApi,
            camundaServiceApi,
            roleAssignmentServiceApi
        );

        taskId = UUID.randomUUID().toString();

        lenient().when(entityManager.getCriteriaBuilder()).thenReturn(builder);
        lenient().when(builder.createQuery(TaskResource.class)).thenReturn(criteriaQuery);
        lenient().when(criteriaQuery.distinct(true)).thenReturn(criteriaQuery);
        lenient().when(criteriaQuery.from(TaskResource.class)).thenReturn(root);
        lenient().when(builder.equal(any(), anyString())).thenReturn(predicate);
        lenient().when(entityManager.createQuery(criteriaQuery)).thenReturn(query);
        lenient().when(query.setFirstResult(0)).thenReturn(query);
        lenient().when(query.setFirstResult(1)).thenReturn(query);
        lenient().when(query.setMaxResults(0)).thenReturn(query);
        lenient().when(query.setMaxResults(10)).thenReturn(query);
        lenient().when(builder.in(any())).thenReturn(inObject);
        lenient().when(inObject.value(any())).thenReturn(values);
        lenient().when(builder.or(any(), any())).thenReturn(inObject);
        lenient().when(builder.or(any())).thenReturn(inObject);
        lenient().when(builder.and(any(), any())).thenReturn(inObject);
        lenient().when(builder.and(any(), any(), any(), any(), any(), any(), any())).thenReturn(inObject);
        BooleanAssertionPredicate booleanAssertionPredicate = new BooleanAssertionPredicate(
            builder,
            null,
            Boolean.TRUE
        );
        lenient().when(builder.conjunction()).thenReturn(booleanAssertionPredicate);
        lenient().when(builder.equal(any(), any())).thenReturn(predicate);
        lenient().when(inObject.value(any())).thenReturn(values);

        lenient().when(taskRoleResources.get(anyString())).thenReturn(authorizations);

        lenient().when(authorizations.isNull()).thenReturn(predicate);
        lenient().when(root.join(anyString())).thenReturn(taskRoleResources);
        lenient().when(root.get(anyString())).thenReturn(path);
        lenient().when(root.get(anyString()).get(anyString())).thenReturn(path);

    }

    @DisplayName("Invalid DMN table")
    @Test
    void should_return_a_500_when_dmn_table_is_invalid() throws Exception {
        searchEventAndCase = new SearchEventAndCase(
            "some-caseId",
            "some-eventId",
            "ia",
            "asylum"
        );
        mockServices.mockServiceAPIs();

        FeignException mockFeignException = mock(FeignException.class);
        when(launchDarklyFeatureFlagProvider.getBooleanValue(any(), any(), any())).thenReturn(false);
        when(mockFeignException.contentUTF8())
            .thenReturn(mockServices.createCamundaTestException(
                "aCamundaErrorType", "There was a problem evaluating DMN"));
        doThrow(mockFeignException).when(camundaServiceApi).evaluateDMN(any(), any(), any(), any());

        mockMvc.perform(
                post("/task/search-for-completable")
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .content(asJsonString(searchEventAndCase))
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            ).andExpect(status().is5xxServerError())
            .andExpect(result -> assertEquals(
                "There was a problem evaluating DMN",
                result.getResolvedException().getMessage()
            ));

        verify(camundaServiceApi, times(1))
            .evaluateDMN(any(), any(), any(), any());
    }

    @Test
    void should_return_a_200_when_dmn_table_is_valid() throws Exception {
        mockServices.mockUserInfo();
        mockServices.mockServiceAPIs();

        List<CamundaTask> camundaTasks = List.of(
            mockServices.getCamundaTask("processInstanceId", taskId)
        );

        when(camundaServiceApi.getTaskCount(any(), any()))
            .thenReturn(new CamundaTaskCount(1));

        when(camundaServiceApi.searchWithCriteriaAndNoPagination(any(), any()))
            .thenReturn(camundaTasks);

        List<Map<String, CamundaVariable>> mockedResponse = asList(Map.of(
            "taskType", new CamundaVariable("reviewTheAppeal", "String"),
            "completionMode", new CamundaVariable("Auto", "String"),
            "workType", new CamundaVariable("decision_making_work", "String")
        ));
        when(camundaServiceApi.evaluateDMN(any(), any(), any(), anyMap()))
            .thenReturn(mockedResponse);

        when(camundaServiceApi.getAllVariables(any(), any()))
            .thenReturn(mockedAllVariables("processInstanceId", "IA", taskId));

        mockMvc.perform(
                post("/task/search-for-completable")
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .content(asJsonString(searchEventAndCase))
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("tasks.size()").value(1));

        verify(camundaServiceApi, times(1))
            .evaluateDMN(any(), any(), any(), any());
    }

    @Test
    void should_return_a_200_and_empty_list_when_jurisdiction_not_IA_and_case_type_not_asylum() throws Exception {
        mockServices.mockUserInfo();
        mockServices.mockServiceAPIs();

        SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
            "some-caseId",
            "decideAnApplication",
            "SSCS",
            "aCaseType"
        );

        List<CamundaTask> camundaTasks = List.of(
            mockServices.getCamundaTask("processInstanceId", taskId)
        );

        when(camundaServiceApi.getTaskCount(any(), any())).thenReturn(new CamundaTaskCount(1));

        when(camundaServiceApi.searchWithCriteriaAndNoPagination(any(), any()))
            .thenReturn(camundaTasks);

        when(camundaServiceApi.getAllVariables(any(), any()))
            .thenReturn(mockedAllVariables("processInstanceId", "SSCS", taskId));

        mockMvc.perform(
                post("/task/search-for-completable")
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .content(asJsonString(searchEventAndCase))
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("tasks.size()").value(0));
    }

    @Test
    void should_return_a_200_and_empty_list_when_idam_user_id_different_from_task_assignee() throws Exception {
        mockServices.mockUserInfo();
        mockServices.mockServiceAPIs();

        List<CamundaTask> camundaTasksForSomeUser = List.of(
            mockServices.getCamundaTaskForSomeUser("processInstanceId", taskId)
        );

        when(camundaServiceApi.getTaskCount(any(), any())).thenReturn(new CamundaTaskCount(1));

        when(camundaServiceApi.searchWithCriteriaAndNoPagination(any(), any()))
            .thenReturn(camundaTasksForSomeUser);


        when(camundaServiceApi.getAllVariables(any(), any()))
            .thenReturn(mockedAllVariables("processInstanceId", "IA", taskId));

        mockMvc.perform(
                post("/task/search-for-completable")
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .content(asJsonString(searchEventAndCase))
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("tasks.size()").value(0));
    }

    @Test
    void should_return_a_200_and_task_list_when_idam_user_id_same_with_task_assignee() throws Exception {
        mockServices.mockUserInfo();
        mockServices.mockServiceAPIs();

        List<CamundaTask> camundaTasks = List.of(
            mockServices.getCamundaTask("processInstanceId", taskId)
        );

        when(camundaServiceApi.getTaskCount(any(), any()))
            .thenReturn(new CamundaTaskCount(1));

        when(camundaServiceApi.searchWithCriteriaAndNoPagination(any(), any()))
            .thenReturn(camundaTasks);

        List<Map<String, CamundaVariable>> mockedResponse = asList(Map.of(
            "taskType", new CamundaVariable("reviewTheAppeal", "String"),
            "completionMode", new CamundaVariable("Auto", "String"),
            "workType", new CamundaVariable("decision_making_work", "String"),
            "description", new CamundaVariable("aDescription", "String")
        ));
        when(camundaServiceApi.evaluateDMN(any(), any(), any(), anyMap()))
            .thenReturn(mockedResponse);

        when(camundaServiceApi.getAllVariables(any(), any()))
            .thenReturn(mockedAllVariables("processInstanceId", "IA", taskId));

        mockMvc.perform(
                post("/task/search-for-completable")
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .content(asJsonString(searchEventAndCase))
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("tasks.size()").value(1))
            .andExpect(jsonPath("tasks[0].assignee").value("IDAM_USER_ID"))
            .andExpect(jsonPath("tasks[0].description").value("aDescription"));
    }

    @Test
    void should_return_a_200_with_empty_list_when_the_user_did_not_have_any_roles() throws Exception {

        UserInfo userInfo = mockServices.mockUserInfo();

        when(roleAssignmentServiceApi.getRolesForUser(
            any(), any(), any()
        )).thenReturn(new RoleAssignmentResource(emptyList()));

        when(idamWebApi.token(any())).thenReturn(new Token(IDAM_AUTHORIZATION_TOKEN, "scope"));
        when(idamWebApi.userInfo(any())).thenReturn(userInfo);

        mockMvc.perform(
                post("/task/search-for-completable")
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .content(asJsonString(searchEventAndCase))
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("tasks.size()").value(0));

    }

    private List<CamundaVariableInstance> mockedAllVariables(String processInstanceId,
                                                             String jurisdiction,
                                                             String taskId) {

        return asList(
            new CamundaVariableInstance(
                jurisdiction,
                "String",
                "jurisdiction",
                processInstanceId,
                taskId
            ),
            new CamundaVariableInstance(
                "PUBLIC",
                "String",
                "securityClassification",
                processInstanceId,
                taskId
            ),
            new CamundaVariableInstance(
                "Read,Refer,Own,Manager,Cancel",
                "String",
                "tribunal-caseworker",
                processInstanceId,
                taskId
            ),
            new CamundaVariableInstance(
                "caseId1",
                "String",
                "caseId",
                processInstanceId,
                taskId
            ),
            new CamundaVariableInstance(
                "aDescription",
                "String",
                "description",
                processInstanceId,
                taskId
            )
        );

    }

    private List<Map<String, CamundaVariable>> mockTaskCompletionDMNResponse() {
        List<Map<String, CamundaVariable>> dmnResult = new ArrayList<>();
        Map<String, CamundaVariable> response = Map.of(
            "completionMode", new CamundaVariable("Auto", "String"),
            "taskType", new CamundaVariable("reviewTheAppeal", "String")
        );
        dmnResult.add(response);
        return dmnResult;
    }

    private TaskResource createTaskResource() {
        return new TaskResource(
            "taskId",
            "aTaskName",
            "startAppeal",
            OffsetDateTime.parse("2022-05-09T20:15:45.345875+01:00"),
            CFTTaskState.COMPLETED,
            TaskSystem.SELF,
            SecurityClassification.PUBLIC,
            "title",
            "a description",
            null,
            0,
            0,
            "someAssignee",
            false,
            new ExecutionTypeResource(ExecutionType.MANUAL, "Manual", "Manual Description"),
            new WorkTypeResource("routine_work", "Routine work"),
            "JUDICIAL",
            false,
            OffsetDateTime.parse("2022-05-09T20:15:45.345875+01:00"),
            "1623278362430412",
            "Asylum",
            "TestCase",
            "IA",
            "1",
            "TestRegion",
            "765324",
            "Taylor House",
            BusinessContext.CFT_TASK,
            "Some termination reason",
            OffsetDateTime.parse("2021-05-09T20:15:45.345875+01:00"),
            Collections.emptySet(),
            "caseCategory",
            ADDITIONAL_PROPERTIES,
                "nextHearingId",
            OffsetDateTime.parse("2021-05-09T20:15:45.345875+01:00"),
            OffsetDateTime.parse("2021-05-09T20:15:45.345875+01:00")
        );
    }

}
