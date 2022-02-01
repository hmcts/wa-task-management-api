package uk.gov.hmcts.reform.wataskmanagementapi.wataskconfigurationapi;

import com.github.benmanes.caffeine.cache.Ticker;
import com.google.common.testing.FakeTicker;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.Token;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.auth.idam.IdamTokenGenerator;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = "IA_IDAM_REDIRECT_URI=http://localhost:3002/oauth2/callback")
@ActiveProfiles("integration")
public class IdamTokenGeneratorCacheTest {

    @MockBean
    private IdamWebApi idamWebApi;

    @Autowired
    private IdamTokenGenerator systemUserIdamToken;


    @TestConfiguration
    public static class OverrideBean {

        static FakeTicker FAKE_TICKER = new FakeTicker();

        @Bean
        public Ticker ticker() {
            return FAKE_TICKER::read;
        }
    }

    @Nested
    @DisplayName("getUserInfo()")
    class GetUserInfo {
        private String bearerAccessToken1;
        private String bearerAccessToken2;

        @BeforeEach
        public void setUp() {
            bearerAccessToken1 = generateBearerToken();
            bearerAccessToken2 = generateBearerToken();
        }

        @Test
        void given_repeated_calls_it_should_cache_user_info() {
            when(idamWebApi.userInfo(bearerAccessToken1)).thenReturn(UserInfo.builder().uid("user1").build());
            when(idamWebApi.userInfo(bearerAccessToken2)).thenReturn(UserInfo.builder().uid("user2").build());

            IntStream.range(0, 3).forEach(i -> systemUserIdamToken.getUserInfo(bearerAccessToken1));
            IntStream.range(0, 3).forEach(i -> systemUserIdamToken.getUserInfo(bearerAccessToken2));

            verify(idamWebApi, times(1)).userInfo(bearerAccessToken1);
            verify(idamWebApi, times(1)).userInfo(bearerAccessToken2);
        }

        @Test
        void given_repeated_calls_when_first_call_fails_it_should_return_cached_user_info() {
            when(idamWebApi.userInfo(bearerAccessToken1))
                .thenThrow(FeignException.FeignServerException.class) // 1st call
                .thenReturn(UserInfo.builder().uid("user1").build()) // 2nd call
                .thenReturn(UserInfo.builder().uid("user1").build()); // 3rd call

            try {
                IntStream.range(0, 3).forEach(i -> systemUserIdamToken.getUserInfo(bearerAccessToken1));
            } catch (Exception e) {
                //Do nothing
            }
            verify(idamWebApi, times(1)).userInfo(bearerAccessToken1);
        }

        @Test
        void given_repeated_calls_when_second_call_fails_it_should_return_cached_user_info() {
            when(idamWebApi.userInfo(bearerAccessToken1))
                .thenReturn(UserInfo.builder().uid("user1").build()) // 1st call
                .thenThrow(FeignException.FeignServerException.class) // 2nd call
                .thenReturn(UserInfo.builder().uid("user1").build()); // 3rd call

            IntStream.range(0, 3).forEach(i -> systemUserIdamToken.getUserInfo(bearerAccessToken1));

            verify(idamWebApi, times(1)).userInfo(bearerAccessToken1);
        }

        @Test
        void given_repeated_calls_when_third_call_fails_it_should_return_cached_user_info() {
            when(idamWebApi.userInfo(bearerAccessToken1))
                .thenReturn(UserInfo.builder().uid("user1").build()) // 1st call
                .thenReturn(UserInfo.builder().uid("user1").build()) // 2nd call
                .thenThrow(FeignException.FeignServerException.class); // 3rd call

            IntStream.range(0, 3).forEach(i -> systemUserIdamToken.getUserInfo(bearerAccessToken1));

            verify(idamWebApi, times(1)).userInfo(bearerAccessToken1);
        }

        @Test
        void reload_user_info_after_cache_is_expired() {
            final String userid3 = "userid3";
            final String userid4 = "userid4";
            when(idamWebApi.userInfo(bearerAccessToken1)).thenReturn(UserInfo.builder().uid(userid3).build());
            when(idamWebApi.userInfo(bearerAccessToken2)).thenReturn(UserInfo.builder().uid(userid4).build());

            assertEquals(userid3, systemUserIdamToken.getUserInfo(bearerAccessToken1).getUid());
            assertEquals(userid4, systemUserIdamToken.getUserInfo(bearerAccessToken2).getUid());

            verify(idamWebApi, times(1)).userInfo(bearerAccessToken1);
            verify(idamWebApi, times(1)).userInfo(bearerAccessToken2);

            //Cache is configured to 30 minutes
            //Advance ticker 29 minutes
            OverrideBean.FAKE_TICKER.advance(29, TimeUnit.MINUTES);
            // Data should still be cached
            assertEquals(userid3, systemUserIdamToken.getUserInfo(bearerAccessToken1).getUid());
            assertEquals(userid4, systemUserIdamToken.getUserInfo(bearerAccessToken2).getUid());

            verify(idamWebApi, times(1)).userInfo(bearerAccessToken1);
            verify(idamWebApi, times(1)).userInfo(bearerAccessToken2);

            //Advance ticker 1 more minute
            OverrideBean.FAKE_TICKER.advance(1, TimeUnit.MINUTES);
            //Data should have expired
            assertEquals(userid3, systemUserIdamToken.getUserInfo(bearerAccessToken1).getUid());
            assertEquals(userid4, systemUserIdamToken.getUserInfo(bearerAccessToken2).getUid());

            verify(idamWebApi, times(2)).userInfo(bearerAccessToken1);
            verify(idamWebApi, times(2)).userInfo(bearerAccessToken2);
        }

        /**
         * Helper method to generate random values for bearer tokens to guarantee uniqueness between tests.
         *
         * @return a random bearer token value as a string
         */
        private String generateBearerToken() {
            return "Bearer Token" + UUID.randomUUID();
        }
    }

    @Nested
    @DisplayName("getUserBearerToken()")
    class GetUserBearerToken {

        @Test
        void given_repeated_calls_it_should_cache_bearer_tokens() {

            String username = UUID.randomUUID().toString();
            String pass = UUID.randomUUID().toString();
            MultiValueMap<String, String> request = buildRequestForUser(username, pass);

            when(idamWebApi.token(request)).thenReturn(new Token("Bearer Token", "Scope"));

            IntStream.range(0, 4).forEach(i -> systemUserIdamToken.getUserBearerToken(username, pass));

            verify(idamWebApi, times(1)).token(request);
        }


        @Test
        void given_lots_repeated_calls_it_should_cache_bearer_tokens() {

            String username = UUID.randomUUID().toString();
            String pass = UUID.randomUUID().toString();
            MultiValueMap<String, String> request = buildRequestForUser(username, pass);

            when(idamWebApi.token(request)).thenReturn(new Token("Bearer Token", "Scope"));

            IntStream.range(0, 500).forEach(i -> systemUserIdamToken.getUserBearerToken(username, pass));

            verify(idamWebApi, times(1)).token(request);
        }


        @Test
        void given_repeated_calls_when_first_call_fails_it_should_return_cached_bearer_token() {

            String username = UUID.randomUUID().toString();
            String pass = UUID.randomUUID().toString();
            MultiValueMap<String, String> request = buildRequestForUser(username, pass);

            when(idamWebApi.token(request))
                .thenThrow(FeignException.FeignServerException.class)
                .thenReturn(new Token("Bearer Token", "Scope"))
                .thenReturn(new Token("Bearer Token", "Scope"));

            try {
                IntStream.range(0, 3).forEach(i -> systemUserIdamToken.getUserBearerToken(username, pass));
            } catch (Exception e) {
                //Do nothing
            }

            verify(idamWebApi, times(1)).token(request);
        }

        @Test
        void given_repeated_calls__and_different_users_it_should_cache_bearer_tokens_based_on_username() {

            String username1 = UUID.randomUUID().toString();
            String pass1 = UUID.randomUUID().toString();
            String username2 = UUID.randomUUID().toString();
            String pass2 = UUID.randomUUID().toString();
            MultiValueMap<String, String> request1 = buildRequestForUser(username1, pass1);
            MultiValueMap<String, String> request2 = buildRequestForUser(username2, pass2);

            when(idamWebApi.token(request1)).thenReturn(new Token("Bearer Token", "Scope"));
            when(idamWebApi.token(request2)).thenReturn(new Token("Bearer Token", "Scope"));

            IntStream.range(0, 4).forEach(i -> systemUserIdamToken.getUserBearerToken(username1, pass1));
            IntStream.range(0, 4).forEach(i -> systemUserIdamToken.getUserBearerToken(username2, pass2));

            verify(idamWebApi, times(1)).token(request1);
            verify(idamWebApi, times(1)).token(request2);
        }

        @Test
        void should_reload_bearer_token_when_cache_expires() {

            String username = UUID.randomUUID().toString();
            String pass = UUID.randomUUID().toString();
            MultiValueMap<String, String> request = buildRequestForUser(username, pass);

            when(idamWebApi.token(request)).thenReturn(new Token("Bearer Token", "Scope"));

            systemUserIdamToken.getUserBearerToken(username, pass);
            verify(idamWebApi, times(1)).token(request);

            //Cache is configured to 30 minutes
            //Advance ticker 29 minutes
            OverrideBean.FAKE_TICKER.advance(29, TimeUnit.MINUTES);
            // Data should still be cached
            systemUserIdamToken.getUserBearerToken(username, pass);
            verify(idamWebApi, times(1)).token(request);

            //Advance ticker 1 more minute
            OverrideBean.FAKE_TICKER.advance(1, TimeUnit.MINUTES);
            // Data should still be expired
            systemUserIdamToken.getUserBearerToken(username, pass);
            verify(idamWebApi, times(2)).token(request);
        }

        private MultiValueMap<String, String> buildRequestForUser(String user, String pass) {
            MultiValueMap<String, String> request = new LinkedMultiValueMap<>();
            request.add("grant_type", "password");
            request.add("redirect_uri", "http://localhost:3002/oauth2/callback");
            request.add("client_id", "wa");
            request.add("client_secret", "something");
            request.add("username", user);
            request.add("password", pass);
            request.add("scope", "openid profile roles");
            return request;
        }
    }

}
