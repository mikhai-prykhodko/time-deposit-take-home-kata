package org.ikigaidigital;

import com.atlassian.oai.validator.restassured.OpenApiValidationFilter;
import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;

/**
 * Whole application over real HTTP with a real Postgres. Every request and response is also checked
 * against openapi.yaml, so the contract and the running service cannot drift apart.
 *
 * Expected balances are computed by hand with exact decimals, not with the application's own code:
 *   basic   1234567.00 * 1% / 12 = 1028.81   -> 1235595.81, next month 1029.66 -> 1236625.47
 *   student    2000.00 * 3% / 12 = 5.00      -> 2005.00,    next month 5.01    -> 2010.01
 *   premium    5000.00 * 5% / 12 = 20.83     -> 5020.83,    next month 20.92   -> 5041.75
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
// Schema only: the demo seed is not loaded, so the test controls its own data.
@org.springframework.test.context.TestPropertySource(properties = "spring.flyway.locations=classpath:db/migration")
class TimeDepositApiIT {

    private static final OpenApiValidationFilter CONTRACT = new OpenApiValidationFilter("openapi.yaml");

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @LocalServerPort
    int port;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void insertTestData() {
        jdbc.update("DELETE FROM withdrawals");
        jdbc.update("DELETE FROM time_deposits");
        jdbc.update("INSERT INTO time_deposits (id, plan_type, days, balance) VALUES (1, 'basic', 45, 1234567.00)");
        jdbc.update("INSERT INTO time_deposits (id, plan_type, days, balance) VALUES (2, 'student', 100, 2000.00)");
        jdbc.update("INSERT INTO time_deposits (id, plan_type, days, balance) VALUES (3, 'premium', 100, 5000.00)");
        jdbc.update("INSERT INTO time_deposits (id, plan_type, days, balance) VALUES (4, 'basic', 20, 1500.00)");
        jdbc.update("INSERT INTO time_deposits (id, plan_type, days, balance) VALUES (5, 'student', 400, 3000.00)");
        jdbc.update("INSERT INTO time_deposits (id, plan_type, days, balance) VALUES (6, 'premium', 40, 8000.00)");
        jdbc.update("INSERT INTO withdrawals (id, time_deposit_id, amount, date) VALUES (1, 1, 100.00, '2024-01-15')");
        jdbc.update("INSERT INTO withdrawals (id, time_deposit_id, amount, date) VALUES (2, 1, 50.25, '2024-02-20')");
    }

    @Test
    void getReturnsAllDepositsWithTheirWithdrawals() {
        api().get("/time-deposits")
            .then().statusCode(200)
            .body("$", hasSize(6))
            .body("id", contains(1, 2, 3, 4, 5, 6))
            .body("[0].planType", org.hamcrest.Matchers.equalTo("basic"))
            .body("[0].balance", org.hamcrest.Matchers.equalTo(1234567.0f))
            .body("[0].days", org.hamcrest.Matchers.equalTo(45))
            .body("[0].withdrawals.id", contains(1, 2))
            .body("[0].withdrawals.amount", contains(100.0f, 50.25f))
            .body("[0].withdrawals.date", contains("2024-01-15", "2024-02-20"))
            .body("[1].withdrawals", empty());
    }

    @Test
    void postAppliesOneMonthOfInterestAndGetShowsTheNewBalances() {
        api().post("/time-deposits/balance-updates").then().statusCode(204);

        api().get("/time-deposits")
            .then().statusCode(200)
            .body("balance", contains(1235595.81f, 2005.0f, 5020.83f, 1500.0f, 3000.0f, 8000.0f))
            // only balances move: plan, age and withdrawals stay as they were
            .body("planType", contains("basic", "student", "premium", "basic", "student", "premium"))
            .body("days", contains(45, 100, 100, 20, 400, 40))
            .body("[0].withdrawals", hasSize(2));
    }

    @Test
    void eachPostAppliesAnotherMonth() {
        api().post("/time-deposits/balance-updates").then().statusCode(204);
        api().post("/time-deposits/balance-updates").then().statusCode(204);

        api().get("/time-deposits")
            .then().statusCode(200)
            .body("balance", contains(1236625.47f, 2010.01f, 5041.75f, 1500.0f, 3000.0f, 8000.0f));
    }

    @Test
    void postOnAnEmptyDatabaseSucceedsAndGetReturnsAnEmptyList() {
        jdbc.update("DELETE FROM withdrawals");
        jdbc.update("DELETE FROM time_deposits");

        api().post("/time-deposits/balance-updates").then().statusCode(204);
        api().get("/time-deposits").then().statusCode(200).body("$", empty());
    }

    private RequestSpecification api() {
        return given().port(port).filter(CONTRACT);
    }
}
