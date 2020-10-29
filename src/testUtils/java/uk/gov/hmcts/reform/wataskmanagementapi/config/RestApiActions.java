package uk.gov.hmcts.reform.wataskmanagementapi.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import io.restassured.RestAssured;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.http.Headers;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

public class RestApiActions {

    private final String baseUri;

    public RestApiActions(final String baseUri) {
        this.baseUri = baseUri;
    }

    public RestApiActions setUp() {
        RestAssured.baseURI = baseUri;
        RestAssured.useRelaxedHTTPSValidation();
        return this;
    }

    protected RequestSpecification given() {
        return RestAssured.given()
            .config(RestAssured.config()
                        .objectMapperConfig(new ObjectMapperConfig().jackson2ObjectMapperFactory(
                            (type, s) -> {
                                ObjectMapper objectMapper = new ObjectMapper();
                                objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
                                return objectMapper;
                            }
                        )));
    }

    public Response get(String path, String resourceId, Headers headers) {
        return this.get(path, resourceId, APPLICATION_JSON_VALUE, APPLICATION_JSON_VALUE, headers);
    }

    public Response get(String path, String resourceId, String accept, Headers headers) {
        return this.get(path, resourceId, APPLICATION_JSON_VALUE, accept, headers);
    }

    public Response get(String path, String resourceId, String contentType, String accept, Headers headers) {
        return given()
            .contentType(contentType)
            .accept(accept)
            .headers(headers)
            .when()
            .get(path, resourceId);
    }

    public Response post(String path, String resourceId, Headers headers) {
        return post(path, resourceId, null, APPLICATION_JSON_VALUE, APPLICATION_JSON_VALUE, headers);
    }

    public Response post(String path, Object body, Headers headers) {
        return post(path, null, body, APPLICATION_JSON_VALUE, APPLICATION_JSON_VALUE, headers);
    }

    public Response post(String path, String resourceId, Object body, String contentType, String accept, Headers headers) {
        return (body != null)
            ? postWithBody(path, resourceId, body, contentType, accept, headers)
            : postWithoutBody(path, resourceId, contentType, accept, headers);
    }

    private Response postWithBody(String path, String resourceId, Object body, String contentType, String accept, Headers headers) {
        return (resourceId != null)
            ? given()
            .contentType(contentType)
            .accept(accept)
            .headers(headers)
            .body(body)
            .when()
            .post(path, resourceId)
            : given()
            .contentType(contentType)
            .accept(accept)
            .headers(headers)
            .body(body)
            .when()
            .post(path);
    }

    private Response postWithoutBody(String path, String resourceId, String contentType, String accept, Headers headers) {
        return (resourceId != null)
            ? given()
            .contentType(contentType)
            .accept(accept)
            .headers(headers)
            .when()
            .post(path, resourceId)
            : given()
            .contentType(contentType)
            .accept(accept)
            .headers(headers)
            .when()
            .post(path);
    }

}
