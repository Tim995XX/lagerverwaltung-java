package de.hilpert.lager;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Startet für Datenbanktests ein echtes PostgreSQL in Docker.
 * {@code @ServiceConnection} trägt URL und Zugangsdaten automatisch in Spring ein.
 */
@TestConfiguration(proxyBeanMethods = false)
public class PostgresTestKonfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
        // gleiche Version wie in docker-compose.yml
        return new PostgreSQLContainer("postgres:17-alpine");
    }
}
