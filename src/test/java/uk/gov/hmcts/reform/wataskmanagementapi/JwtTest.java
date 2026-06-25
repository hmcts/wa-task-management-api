package uk.gov.hmcts.reform.wataskmanagementapi;

import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.util.List;
import java.util.Set;

public class JwtTest {

    private static final String DEFAULT_JWK_SET_URI = "https://idam-web-public.aat.platform.hmcts.net/o/jwks";

    private List<String> allowedIssuers = List.of(
        "https://idam-web-public.aat.platform.hmcts.net/o",
        "https://hmcts-access.aat.platform.hmcts.net/o",
        "https://forgerock-am.service.core-compute-idam-aat2.internal:8443/openam/oauth2/realms/root/realms/hmcts"
    );

    public static void main(String[] args) {
//        String token = "eyJ0eXAiOiJKV1QiLCJraWQiOiI3SndwS29NZDBZZ2UvZ3ZMbFdoL1U0QVN2WXc9IiwiYWxnIjoiUlMyNTYifQ.eyJzdWIiOiJkaXNwb3Nlci1pZGFtLXVzZXIiLCJjdHMiOiJPQVVUSDJfU1RBVEVMRVNTX0dSQU5UIiwiYXVkaXRUcmFja2luZ0lkIjoiMzM0YjM4MGEtNmQ2ZC00YzY0LWJjNTQtYWFiZDU2MTM5MzIxLTYwMDI4NDUiLCJzdWJuYW1lIjoiZGlzcG9zZXItaWRhbS11c2VyIiwiaXNzIjoiaHR0cHM6Ly9mb3JnZXJvY2stYW0uc2VydmljZS5jb3JlLWNvbXB1dGUtaWRhbS1hYXQyLmludGVybmFsOjg0NDMvb3BlbmFtL29hdXRoMi9yZWFsbXMvcm9vdC9yZWFsbXMvaG1jdHMiLCJ0b2tlbk5hbWUiOiJhY2Nlc3NfdG9rZW4iLCJ0b2tlbl90eXBlIjoiQmVhcmVyIiwiYXV0aEdyYW50SWQiOiJjTjVXY05pNDdqLVFBZXhndE5nTno2bnoteXciLCJjbGllbnRfaWQiOiJkaXNwb3Nlci1pZGFtLXVzZXIiLCJhdWQiOiJkaXNwb3Nlci1pZGFtLXVzZXIiLCJuYmYiOjE3ODIzMDU3MzcsImdyYW50X3R5cGUiOiJjbGllbnRfY3JlZGVudGlhbHMiLCJzY29wZSI6WyJkZWxldGUtYXJjaGl2ZWQtdXNlciIsImFyY2hpdmUtdXNlciIsInZpZXctYXJjaGl2ZWQtdXNlciJdLCJhdXRoX3RpbWUiOjE3ODIzMDU3MzcsInJlYWxtIjoiL2htY3RzIiwiZXhwIjoxNzgyMzM0NTM3LCJpYXQiOjE3ODIzMDU3MzcsImV4cGlyZXNfaW4iOjI4ODAwLCJqdGkiOiIyWm5OS1NfUEdzcGVJNVJNQVQyRmFIeWFXZTAifQ.rQJOCCmjEYuyc09oizPQi8s7RpGbyX2cDCefPFw4alkp9S4eU7vSoYJ2xOM5VvIq73l1bCUTRgauv9bTmVmqRqaOkk7M-QU_vndWMPKYzeiEm-y1vymigjp3sReks0XCkRlDsIUW8OWjVGA6LI_50AHMBPYasKWG7uNqhYib-H61_S-rsfI8t_PiIVtpIpN6NTTEwzGCki39B9GyefAlm-g7kTzFCNJ6D7aCDcW2sHPBjkmCrmD63cAKAuwIt7FNelzmY8efVl7YMDMYzbe7M5Vsd3mZ82c2xwOl14CpD_4zUOd77r8Mrif4ByVkzZzaaGUUizSvwCVHJpUvkSLlJQ";
        String token = "eyJ0eXAiOiJKV1QiLCJraWQiOiI3SndwS29NZDBZZ2UvZ3ZMbFdoL1U0QVN2WXc9IiwiYWxnIjoiUlMyNTYifQ.eyJzdWIiOiJkaXNwb3Nlci1pZGFtLXVzZXIiLCJjdHMiOiJPQVVUSDJfU1RBVEVMRVNTX0dSQU5UIiwiYXVkaXRUcmFja2luZ0lkIjoiMzFmYjNiNGEtNmRhNS00OWY2LTgxMmUtNjc4MjM3OTI0MzcyLTE5MzQxODgiLCJzdWJuYW1lIjoiZGlzcG9zZXItaWRhbS11c2VyIiwiaXNzIjoiaHR0cHM6Ly9mb3JnZXJvY2stYW0uc2VydmljZS5jb3JlLWNvbXB1dGUtaWRhbS1hYXQyLmludGVybmFsOjg0NDMvb3BlbmFtL29hdXRoMi9yZWFsbXMvcm9vdC9yZWFsbXMvaG1jdHMiLCJ0b2tlbk5hbWUiOiJhY2Nlc3NfdG9rZW4iLCJ0b2tlbl90eXBlIjoiQmVhcmVyIiwiYXV0aEdyYW50SWQiOiI5YkRzRGswQjhYS2tpZENHcmYtdkxxT1hCNlUiLCJjbGllbnRfaWQiOiJkaXNwb3Nlci1pZGFtLXVzZXIiLCJhdWQiOiJkaXNwb3Nlci1pZGFtLXVzZXIiLCJuYmYiOjE3ODIzOTQ0NzMsImdyYW50X3R5cGUiOiJjbGllbnRfY3JlZGVudGlhbHMiLCJzY29wZSI6WyJkZWxldGUtYXJjaGl2ZWQtdXNlciIsImFyY2hpdmUtdXNlciIsInZpZXctYXJjaGl2ZWQtdXNlciJdLCJhdXRoX3RpbWUiOjE3ODIzOTQ0NzMsInJlYWxtIjoiL2htY3RzIiwiZXhwIjoxNzgyNDIzMjczLCJpYXQiOjE3ODIzOTQ0NzMsImV4cGlyZXNfaW4iOjI4ODAwLCJqdGkiOiJqbnU0WHZMMUJRaFA4NEk1bUNUT1pUUXBia0EifQ.CTct5LB13a0bdmgVUr6k7VcLUTpW7an4ChMEG5NwkGyd_W2F-WhTwotmEHXIId4x6Xi4oze3W0VODYtOlCSKQ_s9URB73qmCReeUGvyBKYB4akc1rQoc9HxemTOaH06kT5cgBF6SYlHpOkE1kWSYhvgGELJiTcRcXqN0Q3A2BQRNkjAtGTHtXS4bWeaovyO2xaDh0yXwUtm9jH00urXS9WkhIwlujWqThQthl5Qk3TzWp7ACEBpsoV1t3XBH-Qy0uHtsyIWwqspEHMc2hC5xsKg3bZ2igAljesVqnER6X_1DRMwotQRTpnJrOuHA_UsDMEzh5geoD3RL2m6cunb9YA";
        String jwkSetUri = DEFAULT_JWK_SET_URI;

        OAuth2ResourceServerProperties resourceServerProperties = new OAuth2ResourceServerProperties();
        resourceServerProperties.getJwt().setJwkSetUri(jwkSetUri);

        Jwt jwt = new JwtTest()
            .jwtDecoder(resourceServerProperties)
            .decode(token);

        System.out.println("Token is valid");
        System.out.println("Issuer: " + jwt.getIssuer());
        System.out.println("Subject: " + jwt.getSubject());
        System.out.println("Expires at: " + jwt.getExpiresAt());
    }

    public JwtDecoder jwtDecoder(
        OAuth2ResourceServerProperties resourceServerProperties) {
        NimbusJwtDecoder jwtDecoder =
            NimbusJwtDecoder.withJwkSetUri(resourceServerProperties.getJwt().getJwkSetUri()).build();
        jwtDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
            JwtValidators.createDefault(),
            allowedIssuersValidator(allowedIssuers)
        ));
        return jwtDecoder;
    }

    OAuth2TokenValidator<Jwt> allowedIssuersValidator(List<String> allowedIssuers) {
        Set<String> allowedIssuerSet = Set.copyOf(allowedIssuers);
        return new JwtClaimValidator<>("iss", allowedIssuerSet::contains);
    }
}
