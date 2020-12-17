package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

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
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamServiceApi;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TaskControllerTest extends SpringBootIntegrationBaseTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    protected AuthorizationHeadersProvider authorizationHeadersProvider;
    @MockBean
    private IdamServiceApi idamServiceApi;
    @MockBean
    private CamundaServiceApi camundaServiceApi;
    @MockBean
    private AuthTokenGenerator authTokenGenerator;
    @MockBean
    private RoleAssignmentServiceApi roleAssignmentServiceApi;

    private ServiceMocks mockServices;

    @BeforeEach
    public void setUp() {
        mockServices = new ServiceMocks(idamServiceApi,
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
}
