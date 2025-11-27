package uk.gov.hmcts.reform.wataskmanagementapi;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles({"integration"})
@AutoConfigureMockMvc(addFilters = false)
@TestInstance(PER_CLASS)
@Transactional//@Testcontainers
//@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class GetWelcomeTest {

    @Autowired
    private MockMvc mvc;

    @DisplayName("Should welcome upon root request with 200 response code")
    @Test
    void welcomeRootEndpoint() throws Exception {
        MvcResult response = mvc.perform(get("/")).andExpect(status().isOk()).andReturn();

        assertThat(response.getResponse().getContentAsString()).startsWith("Welcome");
    }
}
