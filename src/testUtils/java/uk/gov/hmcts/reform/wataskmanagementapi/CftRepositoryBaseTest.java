package uk.gov.hmcts.reform.wataskmanagementapi;

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@ActiveProfiles("integration")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = CftRepositoryBaseTest.DockerPostgreDataSourceInitializer.class)
@Testcontainers
public abstract class CftRepositoryBaseTest {

    public static PostgreSQLContainer<?> postgreDBContainer =
        new PostgreSQLContainer<>("postgres:12")
        .withDatabaseName("cft-tests-db")
        .withUsername("sa")
        .withPassword("sa");

    static {
        postgreDBContainer.start();
    }

    public static class DockerPostgreDataSourceInitializer implements
        ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {

            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                applicationContext,
                "spring.datasource.url=" + postgreDBContainer.getJdbcUrl(),
                "spring.datasource.username=" + postgreDBContainer.getUsername(),
                "spring.datasource.password=" + postgreDBContainer.getPassword()
            );
        }

    }
}
