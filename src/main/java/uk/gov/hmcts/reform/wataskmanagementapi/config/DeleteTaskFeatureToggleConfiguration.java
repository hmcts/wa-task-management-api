package uk.gov.hmcts.reform.wataskmanagementapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class DeleteTaskFeatureToggleConfiguration implements WebMvcConfigurer {
    @Override
    public void addInterceptors(final InterceptorRegistry registry) {
        registry.addInterceptor(deleteTaskFeatureToggleInterceptor()).addPathPatterns("/task/delete/**");
    }

    @Bean
    public DeleteTaskFeatureToggleInterceptor deleteTaskFeatureToggleInterceptor() {
        return new DeleteTaskFeatureToggleInterceptor();
    }
}
