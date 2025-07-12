package uk.gov.hmcts.reform.wataskmanagementapi.config.db;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;

@Profile("replica | preview")
@Configuration
public class FlywayReplicaMigrationConfiguration {
    @Autowired
    private DataSource dataSource;

    @Autowired
    @Qualifier("replicaDataSource")
    private DataSource replicaDataSource;

    @Value("${environment}")
    private String environment;


    @Bean
    public FlywayMigrationStrategy multiDBMigrateStrategy() {
        return new FlywayMigrationStrategy() {
            @Override
            public void migrate(Flyway flyway) {
                Flyway flywayBase;
                if (isNonProdEnvironment()) {
                    flywayBase = modifyConfigForDataSource(
                        flyway, dataSource, "db/migration", "dbcleanup/migration");
                } else {
                    flywayBase = modifyConfigForDataSource(flyway, dataSource, "db/migration");
                }
                flywayBase.migrate();

                Flyway flywayReplica;
                if (isNonProdEnvironment()) {
                    flywayReplica = modifyConfigForDataSource(
                        flyway, replicaDataSource, "dbreplica/migration", "dbcleanupreplica/migration");
                } else {
                    flywayReplica = modifyConfigForDataSource(flyway, replicaDataSource, "dbreplica/migration");
                }

                flywayReplica.migrate();

            }
        };
    }

    private Flyway modifyConfigForDataSource(Flyway flyway, DataSource dataSource, String location) {
        return Flyway.configure()
            .configuration(flyway.getConfiguration())
            .dataSource(dataSource)
            .locations(location).load();
    }

    private Flyway modifyConfigForDataSource(Flyway flyway, DataSource dataSource, String... locations) {
        return Flyway.configure()
            .configuration(flyway.getConfiguration())
            .dataSource(dataSource)
            .locations(locations)
            .load();
    }

    private boolean isNonProdEnvironment() {
        return !"prod".equalsIgnoreCase(environment);
    }
}
