package uk.gov.hmcts.reform.wataskmanagementapi;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

class SmokeTest {

    private final String targetInstance =
        StringUtils.defaultIfBlank(
            System.getenv("TEST_URL"),
            "http://localhost:8087"
        );

    @Test
    void should_check_service_and_return_welcome_message() {

        RequestSpecification requestSpecification = new RequestSpecBuilder()
            .setBaseUri(targetInstance)
            .setRelaxedHTTPSValidation()
            .build();

        given(requestSpecification)
            .when()
            .get("/")
            .then()
            .assertThat()
            .statusCode(HttpStatus.OK.value())
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(containsString("Welcome to wa-task-management-api"));
    }
}
