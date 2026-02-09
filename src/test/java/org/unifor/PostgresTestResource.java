package org.unifor;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

/**
 * Test resource that starts a PostgreSQL container for integration tests (Phase 6, PRD).
 * Ensures isolated DB per test run; Flyway runs migrations on startup.
 */
public class PostgresTestResource implements QuarkusTestResourceLifecycleManager {

    private static final String IMAGE = "postgres:16-alpine";
    private static final String DB_NAME = "unifor_manager";
    private static final String USER = "unifor";
    private static final String PASSWORD = "unifor";

    static PostgreSQLContainer<?> postgres;

    @Override
    public Map<String, String> start() {
        postgres = new PostgreSQLContainer<>(DockerImageName.parse(IMAGE))
                .withDatabaseName(DB_NAME)
                .withUsername(USER)
                .withPassword(PASSWORD);
        postgres.start();
        return Map.of(
                "quarkus.datasource.jdbc.url", postgres.getJdbcUrl(),
                "quarkus.datasource.username", postgres.getUsername(),
                "quarkus.datasource.password", postgres.getPassword()
        );
    }

    @Override
    public void stop() {
        if (postgres != null) {
            postgres.stop();
        }
    }
}
