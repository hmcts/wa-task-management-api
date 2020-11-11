package uk.gov.hmcts.reform.wataskmanagementapi.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.http.Header;
import io.restassured.http.Headers;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

public class RestApiActions {

    private final String baseUri;
    private final PropertyNamingStrategy propertyNamingStrategy;
    RequestSpecBuilder requestSpecBuilder = new RequestSpecBuilder();
    RequestSpecification specification;

    public RestApiActions(final String baseUri, final PropertyNamingStrategy propertyNamingStrategy) {
        this.baseUri = baseUri;
        this.propertyNamingStrategy = propertyNamingStrategy;
    }

    public RestApiActions setUp() {
        requestSpecBuilder.setBaseUri(baseUri);
        specification = requestSpecBuilder.build();
        specification.relaxedHTTPSValidation();
        return this;
    }

    protected RequestSpecification given() {
        return RestAssured.given()
            .spec(specification)
            .config(RestAssured.config()
                .objectMapperConfig(new ObjectMapperConfig().jackson2ObjectMapperFactory(
                    (type, s) -> {
                        ObjectMapper objectMapper = new ObjectMapper();
                        objectMapper.setPropertyNamingStrategy(propertyNamingStrategy);
                        objectMapper.registerModule(new Jdk8Module());
                        objectMapper.registerModule(new JavaTimeModule());
                        return objectMapper;
                    }
                ))
            );
    }

    public Response get(String path, Header header) {
        return this.get(path, null, APPLICATION_JSON_VALUE, APPLICATION_JSON_VALUE, new Headers(header));
    }

    public Response get(String path, String resourceId, Headers headers) {
        return this.get(path, resourceId, APPLICATION_JSON_VALUE, APPLICATION_JSON_VALUE, headers);
    }

    public Response get(String path, String resourceId, String accept, Headers headers) {
        return this.get(path, resourceId, APPLICATION_JSON_VALUE, accept, headers);
    }

    public Response get(String path, String resourceId, String contentType, String accept, Headers headers) {
        return (resourceId != null)
            ? given()
            .contentType(contentType)
            .accept(accept)
            .headers(headers)
            .when()
            .get(path, resourceId)
            : given()
            .contentType(contentType)
            .accept(accept)
            .headers(headers)
            .when()
            .get(path);
    }

    public Response post(String path, String resourceId, Headers headers) {
        return post(path, resourceId, null, APPLICATION_JSON_VALUE, APPLICATION_JSON_VALUE, headers);
    }

    public Response post(String path, String resourceId, Object body, Header header) {
        return post(path, resourceId, body, APPLICATION_JSON_VALUE, APPLICATION_JSON_VALUE, new Headers(header));
    }

    public Response post(String path, Object body, Header header) {
        return post(path, null, body, APPLICATION_JSON_VALUE, APPLICATION_JSON_VALUE, new Headers(header));
    }

    public Response post(String path, Object body, Headers headers) {
        return post(path, null, body, APPLICATION_JSON_VALUE, APPLICATION_JSON_VALUE, headers);
    }

    public Response post(String path,
                         String resourceId,
                         Object body,
                         String contentType,
                         String accept,
                         Headers headers) {
        return (body != null)
            ? postWithBody(path, resourceId, body, contentType, accept, headers)
            : postWithoutBody(path, resourceId, contentType, accept, headers);
    }

    private Response postWithBody(String path,
                                  String resourceId,
                                  Object body,
                                  String contentType,
                                  String accept,
                                  Headers headers) {
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

    private Response postWithoutBody(String path,
                                     String resourceId,
                                     String contentType,
                                     String accept,
                                     Headers headers) {
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
