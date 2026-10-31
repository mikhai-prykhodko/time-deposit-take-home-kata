# Time Deposit Kata (XA Bank) – Project Guide

Take-home refactoring kata for a QE/SDET-focused role. The assignment text is in `README.md`; this file holds the working rules. Read the README requirements before changing scope.

## Scope decisions
- **Language/stack:** Java 17 + Maven in `java/` only. Do not touch `c#/`, `kotlin/`, `python/`, `typescript/`.
- **Framework:** Spring Boot 3.x (web + Spring Data JPA), Flyway for schema, Postgres via Testcontainers, springdoc for Swagger UI.
- **Architecture:** Hexagonal (ports and adapters). Keep `domain` free of Spring, JPA and Jackson imports.
- **Packages:** `TimeDeposit` and `TimeDepositCalculator` stay in `org.ikigaidigital` (moving them is a breaking change). New code goes in `org.ikigaidigital.domain.interest`, `.application`, `.adapter.in.web`, `.adapter.out.persistence`.
- **Exactly two endpoints** (README rule):
  - `GET /time-deposits`: list all deposits with `id, planType, balance, days, withdrawals`.
  - `POST /time-deposits/balance-updates`: apply one month's interest to all deposits. It is not idempotent, which is why it is a POST. `days` is not incremented, matching the legacy code.
  - No health, actuator or extra endpoints. Swagger UI and `/v3/api-docs` are documentation, not API.
- **Contract-first:** `src/main/resources/openapi.yaml` is the source of truth. Generate interfaces with `openapi-generator-maven-plugin` (`interfaceOnly`).
- **DB:** snake_case tables (`time_deposits`, `withdrawals.time_deposit_id`), because Postgres lowercases unquoted identifiers; justify this in the migration. Money is `NUMERIC(19,2)`; convert to and from `Double` with `BigDecimal.valueOf` in the adapter only.
- **Ops extras:** ECS JSON logging (`logging.structured.format.console=ecs`), GitHub Actions running `mvn verify` + PIT. Gatling is out of scope.
- Invalid input handling is out of scope. Do not add validation or error-handling layers.

## Hard constraints (do not break)
- `TimeDeposit` (`org.ikigaidigital.TimeDeposit`) must not get breaking changes. Additive only; no removed or renamed members, no constructor changes.
- `TimeDepositCalculator.updateBalance(List<TimeDeposit>)` signature and behaviour are frozen. It is correct as is; refactor internals only, with the characterization tests green and unedited.
- Interest rules (verified against current code, not just the README):
  - No interest when `days <= 30`.
  - `student`: 3%/12, only while `days < 366`.
  - `premium`: 5%/12, only when `days > 45`.
  - `basic`: 1%/12.
  - Unknown plan type: no interest.
  - Interest rounded to 2 decimals `HALF_UP` (via `new BigDecimal(double)`), then added to balance.
- The `withdrawals` field in the GET response has no counterpart in `TimeDeposit`; map it in the adapter/application layer, not in the shared class. Record this assumption in a code comment.

## Working order (test-first)
1. Characterization tests against the **unmodified** calculator (the existing test asserts nothing: `assertThat(1).isEqualTo(1)`).
2. Refactor domain to per-plan interest strategies; characterization tests must stay green without edits.
3. Ports + persistence (Flyway schema, JPA adapter, Testcontainers test).
4. OpenAPI contract first, then controllers + use cases.
5. End-to-end API tests on Testcontainers.
6. README: Swagger usage, AI setup, what was AI-assisted.

## Testing rules
- JUnit 5 + AssertJ (already in the pom). Prefer parameterized tests for plan/boundary matrices.
- Always cover boundaries: days 30/31, 45/46, 365/366; rounding at the half-cent; unknown plan.
- No mocking of the database in persistence/API tests; use Testcontainers (needs Docker running).
- Unit tests are `*Test` (Surefire, `mvn test`); Docker-backed tests are `*IT` (Failsafe, `mvn verify`).
- `TimeDepositCalculatorTest` holds the characterization tests. After the commit that adds them, it is frozen: never edit it to make a refactor pass.
- PIT mutation testing on the domain: the score must not drop below the recorded baseline.
- E2E tests validate responses against `openapi.yaml` (swagger-request-validator).
- Test names state behaviour (`basicPlan_after30Days_earnsOnePercentMonthly`), not method names.
- A new behaviour or bug fix needs a test in the same commit.

## Commands
```bash
cd java
mvn test                      # unit tests
mvn verify                    # includes integration tests (Docker required)
mvn -q -Dtest=TimeDepositCalculatorTest test
mvn org.pitest:pitest-maven:mutationCoverage   # mutation testing
mvn spring-boot:test-run      # run the app with a Testcontainers Postgres
```

- Every commit compiles and passes `mvn verify`. Atomic means one logical change, not micro-commits.

## Git conventions
- Atomic commits: one logical change each, tests with the code they cover. Imperative subject lines (`Add characterization tests for premium plan`).
- Never commit `target/`, IDE files or secrets (already in `.gitignore`; `.idea/` is ignored).
- Final submission is a **fork** pushed to the candidate's own public GitHub. Do not push branches or open PRs against `ikigai-digital`.
- Commit `CLAUDE.md` early; the README requires AI-assisted development to be documented and reproducible.

## AI-assistance log (keep updated for the README requirement)
Track in `docs/ai-assistance.md` as work proceeds: tool used, which commits it assisted, what was hand-written or reviewed, and any prompts/rules (this file is the rule set). Note anything the AI got wrong and how it was caught.

## Style
- Match existing code (package `org.ikigaidigital`, 4-space indent, plain Java). Prefer clear names over comments; comment only assumptions and non-obvious rules.
- Use `BigDecimal` internally only if it keeps `updateBalance` results identical; otherwise preserve the existing `double` arithmetic exactly.
- Extensibility goal: adding a new plan = adding one strategy class + registering it, with no edits to existing strategies (open/closed).
