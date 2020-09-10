package uk.gov.hmcts.reform.wataskmanagementapi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;
import uk.gov.hmcts.reform.wataskmanagementapi.Application;

@Configuration
@EnableSwagger2
public class SwaggerConfiguration {
    private final boolean enableSwagger;

    public SwaggerConfiguration(@Value("${enableSwagger}") boolean enableSwagger) {
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
            .build();
    }
}
