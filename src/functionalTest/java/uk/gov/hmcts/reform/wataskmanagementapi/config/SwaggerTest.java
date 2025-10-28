package uk.gov.hmcts.reform.wataskmanagementapi.config;

import io.restassured.http.Header;
import io.restassured.response.Response;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestsApiUtils;

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
