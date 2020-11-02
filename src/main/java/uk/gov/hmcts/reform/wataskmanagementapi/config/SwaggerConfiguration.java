package uk.gov.hmcts.reform.wataskmanagementapi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ParameterBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.schema.ModelRef;
import springfox.documentation.service.Parameter;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;
import uk.gov.hmcts.reform.wataskmanagementapi.Application;

import java.util.Arrays;

@Configuration
@EnableSwagger2
public class SwaggerConfiguration {
    private static final String VALUE = "string";
    private static final String HEADER = "header";
    private final boolean enableSwagger;

    public SwaggerConfiguration(@Value("${config.enableSwagger}") boolean enableSwagger) {
        this.enableSwagger = enableSwagger;
    }

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
            .useDefaultResponseMessages(false)
            .enable(enableSwagger)
            .select()
            .apis(RequestHandlerSelectors.basePackage(Application.class.getPackage().getName() + ".controllers"))
            .paths(PathSelectors.any())
            .build()
            .globalOperationParameters(Arrays.asList(
                headerServiceAuthorization(),
                headerAuthorization()

            ));
    }

    private Parameter headerServiceAuthorization() {
        return new ParameterBuilder()
            .name("ServiceAuthorization")
            .description("Keyword `Bearer` followed by a service-to-service token for a whitelisted micro-service")
            .modelRef(new ModelRef(VALUE))
            .parameterType(HEADER)
            .required(true)
            .build();
    }

    private Parameter headerAuthorization() {
        return new ParameterBuilder()
            .name("Authorization")
            .description("Keyword `Bearer` followed by a valid IDAM user token")
            .modelRef(new ModelRef(VALUE))
            .parameterType(HEADER)
            .required(true)
            .build();
    }

}
