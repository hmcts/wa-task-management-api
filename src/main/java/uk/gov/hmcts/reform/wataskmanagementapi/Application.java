package uk.gov.hmcts.reform.wataskmanagementapi;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecuritySchemes;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@SuppressWarnings("HideUtilityClassConstructor") // Spring needs a constructor, its not a utility class
@EnableFeignClients(basePackages =
    {
        "uk.gov.hmcts.reform.authorisation",
        "uk.gov.hmcts.reform.wataskmanagementapi",
        "uk.gov.hmcts.reform.ccd.client",
        "uk.gov.hmcts.reform.wataskmanagementapi.clients"
    })
@EnableCaching
@EnableRetry
@SecuritySchemes({
    @SecurityScheme(type = SecuritySchemeType.APIKEY, name = "ServiceAuthorization",
        in = SecuritySchemeIn.HEADER, scheme = "bearer", paramName = "ServiceAuthorization",
        description = "Keyword `Bearer` followed by a service-to-service token for a whitelisted micro-service"),
    @SecurityScheme(name = "Authorization", type = SecuritySchemeType.HTTP, scheme = "bearer",
        in = SecuritySchemeIn.HEADER, paramName = "Authorization",
        description = "Keyword `Bearer` followed by user authorization token")
})
@OpenAPIDefinition(info = @Info(title = "WA Task Management API"),
    security = { @SecurityRequirement(name = "ServiceAuthorization") })
public class Application {

    public static void main(final String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
