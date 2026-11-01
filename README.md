# Time Deposit Refactoring Kata - Take-Home Assignment

> **Two solutions, one contract:** Java 17 / Spring Boot in [`java/`](java) ([solution notes](#solution-notes-java)) and TypeScript / Express in [`typescript/`](typescript) ([solution notes](#solution-notes-typescript)). Each exposes exactly the same two endpoints from the same OpenAPI contract and the same database schema. The original assignment follows unchanged.

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
Requirements: JDK 17, Maven 3.9, Docker. The complete guide (generated classes, database access, Postman, troubleshooting) is in [`docs/guide.md`](docs/guide.md).

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
adapter.in.web           controller implementing the interface generated from openapi.yaml (contract first)
application              use cases and the ports the core needs (TimeDepositRepository)
domain.interest          InterestStrategy per plan + registry (no framework code)
adapter.out.persistence  JPA entities and the adapter behind the repository port
org.ikigaidigital        TimeDeposit, TimeDepositCalculator (unchanged public API)
```
The API classes under `target/generated-sources` are generated from `openapi.yaml` on every Maven build (nothing to do after a clone beyond `mvn compile`); how that works is explained in [`docs/guide.md`](docs/guide.md#1-the-generated-classes). More background for non-developers: [`docs/walkthrough.md`](docs/walkthrough.md).

## Assumptions
The full list with the reasoning behind each (interest rules, API, data, structure, tooling, out of scope) is in [`docs/assumptions.md`](docs/assumptions.md); the main ones are:

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

---

# Solution notes (TypeScript)

The same kata in TypeScript, built the same way: characterization tests first, then the strategy refactor, then a Postgres adapter, a contract-first API, end-to-end tests, ECS logging and CI. It serves **the same two endpoints** from an identical copy of the contract (`typescript/openapi/openapi.yaml`) on port **3000**.

## Run it
Requirements: Node 24 (22+), Yarn 1, Docker.
```bash
cd typescript
yarn install
yarn dev:containers          # app + throwaway Postgres (Testcontainers), demo data loaded
# or: docker compose up -d && yarn start   (Postgres on port 5433)
```
Then open <http://localhost:3000/swagger-ui> and follow the same Swagger steps as for Java: GET, POST (`204`), GET again. With curl:
```bash
curl localhost:3000/time-deposits
curl -X POST localhost:3000/time-deposits/balance-updates -i
```

## Tests
```bash
cd typescript
yarn typecheck      # tsc --noEmit (Babel-based Jest does not type-check)
yarn test           # unit tests, no Docker
yarn test:it        # Testcontainers adapter and end-to-end tests, needs Docker
yarn mutation       # Stryker mutation testing of the interest logic
```
| Level | What it proves |
|---|---|
| Characterization tests (`src/tests/TimeDepositCalculator.test.ts`) | The original **TypeScript** behaviour, including day boundaries and its own rounding |
| Domain tests | Strategy lookup; a new plan needs only a new class |
| Use case tests | Orchestration against an in-memory fake of the repository and transaction ports |
| HTTP tests (supertest) | JSON shape, `204`, and that the app registers exactly two API routes |
| Postgres tests (`*.it.test.ts`) | Adapter, transaction commit/rollback and migrations on a real Postgres |
| End-to-end test | Full app over HTTP; every response validated against `openapi.yaml` (jest-openapi) |
| Stryker | 38/38 mutants killed in the interest logic |

## How it maps to the Java solution
| Java | TypeScript |
|---|---|
| Spring MVC controller implementing a generated interface | Express routes typed with types generated by `openapi-typescript` (`yarn generate`, run automatically) |
| Spring Data JPA adapter | node-postgres adapter with explicit SQL |
| Flyway | the same SQL files, applied by a small Flyway-style `SqlMigrator` |
| `@Transactional` | a `TransactionRunner` port (`BEGIN`/`COMMIT`/`ROLLBACK`) |
| Spring wiring | manual composition root (`src/compositionRoot.ts`) |
| PIT, REST Assured, swagger-request-validator | Stryker, supertest, jest-openapi |

## TypeScript-specific assumptions
- **Rounding differs from Java.** The TypeScript original rounds with `Math.round((interest + Number.EPSILON) * 100) / 100`. For a basic deposit of 1218 that gives **1.02**, where the Java original gives 1.01. The assignment calls each original correct, so each solution keeps its own; the tests pin both.
- **`src/index.ts`** in the starter did not compile (it created a `TimeDeposit` without an `id`). It was a demo script, not shared API, so it became the server start-up.
- `TimeDeposit.ts` and `TimeDepositCalculator.ts` stay at their paths: moving them would break imports, which counts as a breaking change.
- node-postgres returns `NUMERIC` as a string; conversion to and from numbers happens only in the adapter.

All assumptions, for both solutions, are in [`docs/assumptions.md`](docs/assumptions.md); the TypeScript how-to is in [`docs/guide.md`](docs/guide.md#11-typescript-solution).

