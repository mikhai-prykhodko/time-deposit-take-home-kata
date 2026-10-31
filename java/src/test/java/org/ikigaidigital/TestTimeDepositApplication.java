package org.ikigaidigital;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Runs the real application against a throwaway Postgres (Docker required), with the demo data loaded:
 * mvn spring-boot:test-run
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestTimeDepositApplication {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgres() {
        return new PostgreSQLContainer<>("postgres:16-alpine");
    }

    public static void main(String[] args) {
        SpringApplication.from(TimeDepositApplication::main).with(TestTimeDepositApplication.class).run(args);
    }
}
