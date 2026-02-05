package uk.gov.hmcts.reform.wataskmanagementapi.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;

@Component
public class ServiceAuthFeignInterceptor implements RequestInterceptor {

    //private final ServiceAuthTokenHolder tokenHolder;
    private final AuthTokenGenerator authTokenGenerator;

    @Value("${camunda.url}")
    private String camundaUrl;


    public ServiceAuthFeignInterceptor(AuthTokenGenerator authTokenGenerator) {
        //this.tokenHolder = tokenHolder;
        this.authTokenGenerator = authTokenGenerator;
    }

    @Override
    public void apply(RequestTemplate template) {
        String url = template.url();
        if (template.feignTarget().url().contains(camundaUrl) &&
            url.matches(".*/task/[^/]+/complete$")) {
            String value = authTokenGenerator.generate();
            if (value != null && !value.isBlank()) {
                template.header("ServiceAuthorization");
                template.header("ServiceAuthorization", value);
            }
        }
    }
}
