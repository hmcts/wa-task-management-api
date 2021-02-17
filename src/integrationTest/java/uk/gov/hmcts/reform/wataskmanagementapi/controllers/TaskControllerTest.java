package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootIntegrationBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.SearchEventAndCase;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.RoleAssignmentServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.services.AuthorizationHeadersProvider;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TaskControllerTest extends SpringBootIntegrationBaseTest {

    @Autowired
    protected AuthorizationHeadersProvider authorizationHeadersProvider;
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private IdamWebApi idamWebApi;
    @MockBean
    private CamundaServiceApi camundaServiceApi;
    @MockBean
    private AuthTokenGenerator authTokenGenerator;
    @MockBean
    private RoleAssignmentServiceApi roleAssignmentServiceApi;

    private ServiceMocks mockServices;

    @BeforeEach
    public void setUp() {
        mockServices = new ServiceMocks(idamWebApi,
            camundaServiceApi,
            roleAssignmentServiceApi);
    }

    @Nested
    @DisplayName("cancelTask()")
    class CancelTask {

        @DisplayName("Invalid escalation code")
        @Test
        void should_return_a_500_when_esclation_code_is_invalid() throws Exception {
            final var taskId = UUID.randomUUID().toString();

            final var errorMessage = "There was a problem cancelling "
                                     + "the task with id: " + taskId;

            mockServices.mockServiceAPIs();

            FeignException mockFeignException = mock(FeignException.class);

            when(mockFeignException.contentUTF8())
                .thenReturn(mockServices.createCamundaTestException(
                    "aCamundaErrorType", String.format(
                        "There was a problem cancelling the task with id: %s",
                        taskId)));
            doThrow(mockFeignException).when(camundaServiceApi).bpmnEscalation(any(), any(), any());

            mockMvc.perform(
                post("/task/" + taskId + "/cancel")
                    .header(
                        "Authorization",
                        authorizationHeadersProvider.getTribunalCaseworkerAAuthorization()
                    )
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            ).andExpect(status().is5xxServerError())
                .andExpect(result -> assertEquals(
                    errorMessage,
                    result.getResolvedException().getMessage()
                ));

            verify(camundaServiceApi, times(1))
                .bpmnEscalation(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("getTask()")
    class GetTask {
        @Test
        void should_return_a_500_when_id_invalid() throws Exception {
            final var taskId = UUID.randomUUID().toString();


            mockServices.mockServiceAPIs();

            FeignException mockFeignException = mock(FeignException.FeignServerException.class);

            when(mockFeignException.contentUTF8())
                .thenReturn(mockServices.createCamundaTestException(
                    "aCamundaErrorType", String.format(
                        "There was a problem fetching the task with id: %s",
                        taskId
                    )));
            doThrow(mockFeignException).when(roleAssignmentServiceApi).createRoleAssignment(any(), any(), any());

            mockMvc.perform(
                get("/task/" + taskId)
                    .header(
                        "Authorization",
                        authorizationHeadersProvider.getTribunalCaseworkerAAuthorization()
                    )
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            ).andExpect(status().is5xxServerError());

            verify(camundaServiceApi, times(1))
                .getTask(any(), any());
        }
    }

    @Nested
    @DisplayName("auto-complete()")
    class AutoCompleteTask {

        @DisplayName("Invalid DMN table")
        @Test
        void should_return_a_500_when_dmn_table_is_invalid() throws Exception {
            final var taskId = UUID.randomUUID().toString();

            final var errorMessage = "There was a problem cancelling "
                                     + "the task with id: " + taskId;

            SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
                "some-caseId",
                "some-eventId",
                "some-caseJurisdiction",
                "some-caseType"
            );
            ObjectMapper mapper = new ObjectMapper();
            final var searchContent = mapper.writeValueAsString(searchEventAndCase);

            mockServices.mockServiceAPIs();

            FeignException mockFeignException = mock(FeignException.class);

            when(mockFeignException.contentUTF8())
                .thenReturn(mockServices.createCamundaTestException(
                    "aCamundaErrorType", "There was a problem evaluating DMN"));
            doThrow(mockFeignException).when(camundaServiceApi).evaluateDMN(any(), any(), any());

            mockMvc.perform(
                post("/task/searchForCompletable")
                    .header(
                        "Authorization",
                        authorizationHeadersProvider.getTribunalCaseworkerAAuthorization()
                    )
                    .content(searchContent)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            ).andExpect(status().is5xxServerError())
                .andExpect(result -> assertEquals(
                    "There was a problem evaluating DMN",
                    result.getResolvedException().getMessage()
                ));

            verify(camundaServiceApi, times(1))
                .evaluateDMN(any(), any(), any());
        }
    }
}
