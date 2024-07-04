package uk.gov.hmcts.reform.wataskmanagementapi.db;

import org.jetbrains.annotations.NotNull;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.JdbcDatabaseContainerProvider;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.jdbc.ConnectionUrl;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

public class PostgreExtendedSQLContainerProvider extends JdbcDatabaseContainerProvider {

    private static final String NAME = "postgresql";

    public static final String USER_PARAM = "postgres";

    public static final String PASSWORD_PARAM = "postgres";

    private final DockerImageName hmctsPostgresDockerImage = DockerImageName
        .parse("hmctspublic.azurecr.io/imported/postgres:15-alpine")
        .asCompatibleSubstituteFor("postgres");

    private String databaseName;

    public PostgreExtendedSQLContainerProvider(String databaseName) {
        this.databaseName = databaseName;
    }

    @Override
    public boolean supports(String databaseType) {
        return databaseType.equals(NAME);
    }

    @Override
    public JdbcDatabaseContainer newInstance() {
        return getJdbcDatabaseContainer(newInstance(PostgreSQLContainer.DEFAULT_TAG));
    }

    @Override
    public JdbcDatabaseContainer newInstance(String tag) {
        return getJdbcDatabaseContainer(new PostgreSQLContainer(hmctsPostgresDockerImage.withTag(tag)));
    }

    @Override
    public JdbcDatabaseContainer newInstance(ConnectionUrl connectionUrl) {
        return getJdbcDatabaseContainer(newInstanceFromConnectionUrl(connectionUrl, USER_PARAM, PASSWORD_PARAM));
    }

    private JdbcDatabaseContainer getJdbcDatabaseContainer(JdbcDatabaseContainer jdbcDatabaseContainer) {
        return (JdbcDatabaseContainer) jdbcDatabaseContainer
            .withDatabaseName(databaseName)
            .withUsername(USER_PARAM)
            .withPassword(PASSWORD_PARAM)
            .withInitScript(getInitScript(databaseName))
            .withCopyFileToContainer(MountableFile.forClasspathResource("pg_hba.conf"), "/postgresql/conf/conf.d/")
            .withCommand(
                "postgres -c max_connections=500 -c wal_level=logical -c hba_file=/postgresql/conf/conf.d/pg_hba.conf "
                + "-c log_statement=all "
                + "-c log_destination=stderr "
                + "-c log_min_messages=info "
                + "-c client_min_messages=info");
    }

    @NotNull
    private String getInitScript(String databaseName) {
        if (databaseName.equals("cft_task_db_replica")) {
            return "initDbReplica.sql";
        } else {
            return "initDb.sql";
        }
    }

}
