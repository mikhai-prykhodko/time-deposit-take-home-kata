# Time Deposit Refactoring Kata - Take-Home Assignment

> **Solution:** Java 17 / Spring Boot in [`java/`](java). Jump to the [solution notes](#solution-notes-java) for how to run it, call the two endpoints from Swagger UI, and the assumptions made. The original assignment follows unchanged.

## XA Bank Time Deposit

### Context
A junior developer implemented domain logic for a time deposit system but did not complete the API functionality. Your task is to refactor the existing codebase to implement all required functionalities based on the provided business requirements, ensuring no breaking changes occur.

### Requirements

1. **API Endpoints**:
    - Create a RESTful API endpoint to update the balances of all time deposits in the database.
    - Create a RESTful API endpoint to retrieve all time deposits.
        - The GET endpoint should return a list of all time deposits with the following schema:
            - `id`
            - `planType`
            - `balance`
            - `days`
            - `withdrawals`

2. **Database Setup**:
    - Store all time deposit plans in a database.
    - Define the following tables:
        - `timeDeposits`:
            - `id`: Integer (primary key)
            - `planType`: String (required)
            - `days`: Integer (required)
            - `balance`: Decimal (required)
        - `withdrawals`:
            - `id`: Integer (primary key)
            - `timeDepositId`: Integer (foreign key, required)
            - `amount`: Decimal (required)
            - `date`: Date (required)

3. **Interest Calculation**:
    - Implement logic to calculate monthly interest based on the plan type:
        - **Basic Plan**: 1% interest
        - **Student Plan**: 3% interest (no interest after 1 year)
        - **Premium Plan**: 5% interest (interest starts after 45 days)
    - No interest is applied for the first 30 days for any existing plans.

4. **Refactoring Constraints**:
    - Do not introduce breaking changes to the shared `TimeDeposit` class or modify the `updateBalance` method signature.
    - Ensure the design is extensible to accommodate future complexities in interest calculations.

5. **Code Quality**:
    - Adhere to SOLID principles, design patterns, and clean code practices where applicable.

6. **AI-Assisted Development**:
    - Set up an AI harness or agent workflow and use it throughout the development for this take-home exercise.
    - Briefly document the tools and setup used (e.g., LLMs, coding assistants, agentic frameworks, configuration).
    - Ensure your AI setup is practical and reproducible.
    - Include any custom rules, system prompts, or agent configurations used.
    - Include a brief summary of which parts of the solution were AI-assisted and why.

### Important Guidelines
- The existing `TimeDepositCalculator.updateBalance` method is functioning correctly. Ensure its behavior remains unchanged after refactoring.
- The final solution must include **exactly two API endpoints**. Do not develop additional endpoints.
- **Do not** create a pull request or a new branch in the ikigai-digital repository. Instead, fork the repository into your own GitHub repository and develop the solution there.
- Handling invalid input or exceptions is not required.
- Use any tools, frameworks, or libraries you find suitable.
- In case of ambiguity, make logical assumptions and justify them in code comments.

### Preferred Stack
- Use an OpenAPI Swagger contract.
- Embrace Hexagonal Architecture.
- Follow atomic commit practices.
- Utilize testcontainers.
- Leverage AI-assisted development tools for code generation, testing, and refactoring.

### Submission Instructions
- Provide clear instructions on how to trigger the endpoints using the Swagger contract.
- Email the link to your public GitHub repository.

---

# Solution notes (Java)

## What was built
- `GET /time-deposits` returns every time deposit as `id, planType, balance, days, withdrawals`.
- `POST /time-deposits/balance-updates` applies one month of interest to every time deposit (`204 No Content`).
- Exactly these two endpoints exist. Swagger UI and `/v3/api-docs` only document them; there is no actuator or any other endpoint.
- The interest rules are the original ones, now one strategy class per plan. `TimeDeposit` and the signature and behaviour of `TimeDepositCalculator.updateBalance` are unchanged; 26 characterization tests pin that behaviour.

## Run it
Requirements: JDK 17, Maven 3.9, Docker.

**Option A: no local database (Testcontainers starts Postgres for you)**
```bash
cd java
mvn spring-boot:test-run
```

**Option B: docker compose**
```bash
cd java
docker compose up -d
mvn spring-boot:run
# when done: docker compose down
```
Both load a few demo deposits (`java/src/main/resources/db/seed`).

## Trigger the endpoints with Swagger
1. Start the app (above) and open <http://localhost:8080/swagger-ui.html>.
2. Expand **GET /time-deposits**, click **Try it out**, then **Execute**. Note the balances.
3. Expand **POST /time-deposits/balance-updates**, click **Try it out**, then **Execute**. Expect `204`.
4. Run the GET again: balances changed according to each plan.

The contract itself is [`java/src/main/resources/openapi.yaml`](java/src/main/resources/openapi.yaml) (also served at <http://localhost:8080/v3/api-docs>). With curl:
```bash
curl localhost:8080/time-deposits
curl -X POST localhost:8080/time-deposits/balance-updates -i
```

## Tests
```bash
cd java
mvn test                                       # fast unit tests, no Docker
mvn verify                                     # adds Testcontainers tests (*IT), needs Docker
mvn org.pitest:pitest-maven:mutationCoverage   # mutation testing of the interest logic
```
| Level | What it proves |
|---|---|
| Characterization tests (`TimeDepositCalculatorTest`) | The original interest behaviour, including day boundaries 30/31, 45/46, 365/366 and half-cent rounding |
| Domain tests | Strategy lookup; a new plan needs only a new class |
| Use case tests | Orchestration against an in-memory fake of the repository port |
| Controller slice test | JSON shape, `204`, and that the app exposes exactly two endpoints |
| Persistence test (`*IT`) | JPA adapter on a real Postgres, including the `Double` to `NUMERIC(19,2)` round trip |
| End-to-end test (`TimeDepositApiIT`) | Full app over HTTP; every request and response validated against `openapi.yaml` |
| PIT | 21/21 mutants killed in the interest logic |

CI (`.github/workflows/ci.yml`) runs `mvn verify` and PIT on every push.

## Architecture (hexagonal)
```
adapter.in.web           controller generated from openapi.yaml (contract first)
application              use cases and the ports the core needs (TimeDepositRepository)
domain.interest          InterestStrategy per plan + registry (no framework code)
adapter.out.persistence  JPA entities and the adapter behind the repository port
org.ikigaidigital        TimeDeposit, TimeDepositCalculator (unchanged public API)
```
More background for non-developers: [`docs/walkthrough.md`](docs/walkthrough.md).

## Assumptions
- **Table names:** snake_case (`time_deposits`, `plan_type`, `time_deposit_id`) because Postgres lowercases unquoted names; the API keeps the camelCase names from the assignment.
- **`withdrawals` in the response:** `TimeDeposit` has no such field and must not break, so withdrawals travel in a separate read model (`TimeDepositDetails`) mapped in the adapter.
- **Update semantics:** each POST applies one month of interest, so it is not idempotent, which is why it is a POST. `days` is not changed (the original code never did).
- **Unknown plan type:** earns no interest, as in the original code.
- **Rounding:** interest is rounded half-up on the exact `double` value, exactly like the original. For example 1218 on the basic plan gives 1.015, stored as 1.01499..., which rounds to 1.01; a test pins this.
- **Money in JSON:** plain numbers (`1000.5`), not strings.
- **Out of scope:** validation and error handling, authentication, concurrency control between simultaneous POSTs.
- **Logging:** ECS JSON on the console, plus one line per balance-update run with the number of deposits and the duration.

## AI-assisted development
See [`docs/ai-assistance.md`](docs/ai-assistance.md) for the tools, the rules file ([`CLAUDE.md`](CLAUDE.md)), what was AI-assisted and why, and the AI mistakes that were caught.
