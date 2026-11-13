package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import feign.FeignException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;
import uk.gov.hmcts.reform.wataskmanagementapi.config.IntegrationTest;

import java.util.Map;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.AUTHORIZATION;

@IntegrationTest(properties = {
    "search.jurisdictionUsers.probate=probate-search@example.test",
    "search.jurisdictionUsers.ia=ia-search@example.test"
})
@AutoConfigureMockMvc(addFilters = false)
@Transactional
@Sql("/scripts/system_task_search.sql")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SystemTaskSearchControllerTest {

    private static final String ENDPOINT_PATH = "/task/search";
    private static final String TOKEN = "Bearer system-search-token";
    private static final String CASE_ID = "1793986596864362";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private IdamWebApi idamWebApi;

    @BeforeEach
    void setUp() {
        reset(idamWebApi);
        when(idamWebApi.userInfo(TOKEN)).thenReturn(UserInfo.builder()
            .uid("probate-system-user").email("probate-search@example.test").build());
    }

    @Test
    void should_return_200_with_tasks_for_the_case_and_users_jurisdiction() throws Exception {
        mockMvc.perform(get(ENDPOINT_PATH).param("case_id", CASE_ID).header(AUTHORIZATION, TOKEN))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(jsonPath("$.tasks[*].id", containsInAnyOrder(
                "70000000-0000-0000-0000-000000000001", "70000000-0000-0000-0000-000000000002")))
            .andExpect(jsonPath("$.tasks[*].case_id", everyItem(is(CASE_ID))))
            .andExpect(jsonPath("$.tasks[*].jurisdiction", everyItem(is("PROBATE"))))
            .andExpect(jsonPath("$.tasks[*].task_state", containsInAnyOrder("unassigned", "cancelled")));
    }

    @Test
    void should_return_200_with_empty_tasks_when_case_has_no_tasks() throws Exception {
        mockMvc.perform(get(ENDPOINT_PATH).param("case_id", "1793986596864399").header(AUTHORIZATION, TOKEN))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tasks").isEmpty());
    }

    @Test
    void should_return_401_when_idam_rejects_user() throws Exception {
        when(idamWebApi.userInfo(TOKEN)).thenThrow(idamFailure(401));

        mockMvc.perform(get(ENDPOINT_PATH).param("case_id", CASE_ID).header(AUTHORIZATION, TOKEN))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("IDAM user not available"));
    }

    @Test
    void should_return_403_when_user_has_no_jurisdiction_mapping() throws Exception {
        when(idamWebApi.userInfo(TOKEN)).thenReturn(UserInfo.builder()
            .uid("normal-user").email("normal-user@example.test").build());

        mockMvc.perform(get(ENDPOINT_PATH).param("case_id", CASE_ID).header(AUTHORIZATION, TOKEN))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.detail").value(containsString("User does not belong to any jurisdiction")));
    }

    @Test
    void should_return_503_when_idam_is_unavailable() throws Exception {
        when(idamWebApi.userInfo(TOKEN)).thenThrow(idamFailure(503));

        mockMvc.perform(get(ENDPOINT_PATH).param("case_id", CASE_ID).header(AUTHORIZATION, TOKEN))
            .andExpect(status().isServiceUnavailable());
    }

    @Test
    void should_return_400_when_case_id_is_missing() throws Exception {
        mockMvc.perform(get(ENDPOINT_PATH).header(AUTHORIZATION, TOKEN)).andExpect(status().isBadRequest());
    }

    @Test
    void should_return_400_when_case_id_is_blank() throws Exception {
        mockMvc.perform(get(ENDPOINT_PATH).param("case_id", " ").header(AUTHORIZATION, TOKEN))
            .andExpect(status().isBadRequest());
    }

    private static FeignException idamFailure(int status) {
        Request request = Request.create(Request.HttpMethod.GET, "http://idam/o/userinfo", Map.of(),
                                         (byte[]) null, null, null);
        return FeignException.errorStatus("IdamWebApi#userInfo", Response.builder()
            .status(status).reason("IDAM failure").request(request).build());
    }
}
