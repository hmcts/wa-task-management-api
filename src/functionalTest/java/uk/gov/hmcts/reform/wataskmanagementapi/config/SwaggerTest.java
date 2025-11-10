package uk.gov.hmcts.reform.wataskmanagementapi.config;

import io.restassured.http.Header;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestsApiUtils;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestsApiUtils;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestsApiUtils;

@RunWith(SpringIntegrationSerenityRunner.class)
@SpringBootTest
@ActiveProfiles("functional")
@Slf4j
public class SwaggerTest {

    @Autowired
    TaskFunctionalTestsApiUtils taskFunctionalTestsApiUtils;

    private static final String SWAGGER_URL = "/swagger-ui/index.html";

    @Test
    public void swagger_ui_should_be_accessible_with_no_auth_long_url() {

        Response result = taskFunctionalTestsApiUtils.getRestApiActions().get(
            SWAGGER_URL,
            new Header("Content-type", "application/json")
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value());

    }

}
