package uk.gov.hmcts.reform.wataskmanagementapi.config.db;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class FlywayReplicaMigrationConfiguration {
    @Autowired
    private DataSource dataSource;

    @Autowired
    @Qualifier("replicaDataSource")
    private DataSource replicaDataSource;

    private static final String SCHEMA_NAME = "cft_task_db";

    @Bean
    public FlywayMigrationStrategy multiDBMigrateStrategy() {
        return new FlywayMigrationStrategy() {
            @Override
            public void migrate(Flyway flyway) {
                Flyway flywayBase = Flyway.configure()
                    .dataSource(dataSource)
                    .schemas(SCHEMA_NAME)
                    .defaultSchema(SCHEMA_NAME)
                    .locations("db/migration")
                    .baselineOnMigrate(true)
                    .target(MigrationVersion.LATEST).load();

                flywayBase.migrate();

                Flyway flywayReplica = Flyway.configure()
                    .dataSource(replicaDataSource)
                    .schemas(SCHEMA_NAME)
                    .defaultSchema(SCHEMA_NAME)
                    .locations("dbreplica/migration")
                    .baselineOnMigrate(true)
                    .target(MigrationVersion.LATEST).load();

                flywayReplica.migrate();

            }
        };
    }
}
