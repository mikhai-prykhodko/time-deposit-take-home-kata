# Assumptions and decisions

The assignment says: *"In case of ambiguity, make logical assumptions and justify them in code comments."* This page lists every place where the assignment was silent, ambiguous or open, what was decided, why, and where it shows up in the repository (a code comment, a test, or both).

**Legend**
- **Assumption**: the assignment did not say, so a reading was chosen.
- **Decision**: a free choice (tooling, design) where several options were fine.

Contents: [A. Interest rules](#a-interest-rules) · [B. API](#b-api) · [C. Data](#c-data) · [D. Code structure](#d-code-structure) · [E. Tooling and process](#e-tooling-and-process) · [F. Out of scope](#f-out-of-scope) · [G. TypeScript solution](#g-typescript-solution)

---

## A. Interest rules

The assignment gives the rules in words and also says the existing `TimeDepositCalculator.updateBalance` is correct and must behave the same after refactoring. So wherever the words are ambiguous, **the original code is the tie-breaker**, and the 26 characterization tests pin it.

| # | Ambiguity | Choice | Why | Where |
|---|---|---|---|---|
| A1 | "Basic plan: 1% interest", "monthly interest": is 1% a year or 1% a month? | **Assumption:** the rates are annual and one update applies a twelfth (`balance x rate / 12`) | The original code does exactly this, and the assignment calls it correct | `InterestStrategy` javadoc; tests: 1200 on basic at day 31 gains 1.00 |
| A2 | "No interest for the first 30 days": when does interest start? | **Assumption:** the first interest is earned at **day 31** (`days > 30`) | Same as the original code | `InterestStrategy` (`GRACE_PERIOD_DAYS`); tests at day 30 for every plan and at day 31 for basic and student |
| A3 | Student: "no interest after 1 year": does day 365 or day 366 stop it? | **Assumption:** day 365 still earns, **day 366 and later earn nothing** | Original code uses `days < 366` | `StudentInterest`; tests at 365 and 366 |
| A4 | Premium: "interest starts after 45 days": day 45 or day 46? | **Assumption:** day 45 earns nothing, **day 46** is the first | Original code uses `days > 45` | `PremiumInterest`; tests at 45 and 46 |
| A5 | What happens with a plan type that is not basic, student or premium? | **Assumption:** it earns **no interest** (no error) | Original code ignores unknown types; the assignment says error handling is not required | `InterestStrategyRegistry` comment; tests with `gold`, `""` |
| A6 | Are plan types case sensitive? | **Assumption:** yes, exact lower case. `"Basic"` earns nothing | Original code uses `equals` | Pinned by a characterization test |
| A7 | How exactly is interest rounded? | **Assumption:** to 2 decimals, half-up, **on the exact binary `double` value** | The original does this through `new BigDecimal(double)`. Example: 1218 on basic gives 1.015, stored as 1.01499..., so it rounds to **1.01**, whereas `BigDecimal.valueOf` would give 1.02 and silently change customers' money | `TimeDepositCalculator` comment; tests for 1206 (1.01) and 1218 (1.01) |
| A8 | Do withdrawals affect interest or the balance? | **Assumption:** no. They are informational; the stored balance is already net of them | No rule in the assignment mentions them, and the calculator takes only balance, days and plan | `TimeDepositDetails` comment |
| A9 | What does the update do to `days`? | **Assumption:** nothing. `days` is never incremented | The original never changes it; it describes the age of the deposit | `openapi.yaml` description; end-to-end test checks `days` unchanged |
| A10 | Is the update idempotent? What does a second call do? | **Assumption:** each call applies **one more month**, so it is not idempotent | "Update the balances" is read as "apply one month's interest"; the calculator has no notion of "already updated this month" | `openapi.yaml`; end-to-end test with two POSTs (1235595.81 then 1236625.47) |

---

## B. API

| # | Ambiguity or choice | Choice | Why | Where |
|---|---|---|---|---|
| B1 | URLs and HTTP methods were not specified, only "RESTful" | **Decision:** `GET /time-deposits` and `POST /time-deposits/balance-updates` | The list is a normal collection read. The update is an action that changes data and is not idempotent, so `POST` fits better than `PUT`/`PATCH`; naming it as a resource (`balance-updates`) keeps the URL free of verbs | `openapi.yaml` |
| B2 | What does the update return? | **Assumption:** `204 No Content` | The assignment asks only that balances get updated; clients read the result with the GET | `openapi.yaml`; controller test; end-to-end test |
| B3 | Order of the list | **Assumption:** deposits ordered by `id`, each deposit's withdrawals by `id` | Gives stable, testable responses | `TimeDepositRepository` comment; end-to-end test |
| B4 | JSON type of money | **Decision:** plain JSON numbers (`1000.5`), not strings | Simple for clients and matches the schema in the assignment | `openapi.yaml` description |
| B5 | "Exactly two API endpoints": do Swagger UI and the OpenAPI JSON count? | **Assumption:** no, they document the API but are not part of it. Likewise Spring Boot's built-in `/error` fallback, which only renders error responses. No actuator or health endpoint exists | Needed to satisfy "Use an OpenAPI Swagger contract" and "provide instructions using the Swagger contract" together with "exactly two endpoints" | A test asserts the application's own mappings are exactly the two; README |
| B6 | Contract first | **Decision:** `openapi.yaml` is written first and the Java interface and DTOs are generated from it at build time | The assignment prefers an OpenAPI contract; generating the code keeps code and contract from drifting apart | `java/pom.xml`; `docs/guide.md` section 1 |
| B7 | Validation, error responses, authentication | **Assumption:** none | The assignment says invalid input handling is not required and mentions no security | README "Out of scope" |

---

## C. Data

| # | Ambiguity or choice | Choice | Why | Where |
|---|---|---|---|---|
| C1 | Table and column names: the assignment says `timeDeposits`, `timeDepositId` | **Decision:** snake_case in the database (`time_deposits`, `plan_type`, `time_deposit_id`); the API keeps camelCase | Postgres lowercases unquoted names, so `timeDeposits` would silently become `timedeposits`. Quoting camelCase everywhere is error prone | Comment at the top of `V1__...sql` |
| C2 | Column types | **Decision:** `balance` and `amount` are `NUMERIC(19,2)`, ids `INTEGER`, `date` is `DATE` | Exact decimals for money; matches the assignment's types | `V1__...sql` |
| C3 | The shared `TimeDeposit` has `Double balance`, the database uses exact decimals | **Decision:** convert only inside the persistence adapter, with `BigDecimal.valueOf(...).setScale(2, HALF_UP)` | `TimeDeposit` must not change. `valueOf` avoids values like `1235595.8100000000`; a round-trip test proves there is no drift | `TimeDepositPersistenceAdapter` comment; adapter integration test |
| C4 | The GET response needs `withdrawals`, but `TimeDeposit` has no such field and must not break | **Decision:** a separate read model `TimeDepositDetails` carries the deposit plus its withdrawals, mapped in the adapter | Additive; leaves `TimeDeposit` untouched | `TimeDepositDetails` comment |
| C5 | Ids | **Assumption:** supplied, not auto-generated | There is no endpoint that creates deposits, so nothing needs generated ids | `V1__...sql` (plain `INTEGER PRIMARY KEY`) |
| C6 | Which database | **Decision:** PostgreSQL 16 | Not specified; widely used, works with Testcontainers | `pom.xml`, `docker-compose.yml`, tests |
| C7 | Who owns the schema | **Decision:** Flyway migrations; Hibernate only validates (`ddl-auto=validate`) | A versioned, repeatable schema; mismatches fail at startup | `application.properties`, `db/migration` |
| C8 | Extra schema details | **Decision:** a foreign key `withdrawals.time_deposit_id -> time_deposits.id`, plus an index on that column | The assignment requires the foreign key; the index speeds the join | `V1__...sql` |
| C9 | Demo data | **Decision:** loaded from a separate folder `db/seed`, which only the running app reads; tests start from an empty schema and insert their own data | Reviewers can try the endpoints immediately, while tests stay deterministic | `application.properties` (`spring.flyway.locations`); the integration tests override it |
| C10 | Update atomicity | **Decision:** the whole update is one transaction: all balances change or none | A half-applied month would be worse than a failed one | `UpdateAllBalancesService` |

---

## D. Code structure

| # | Choice | Why | Where |
|---|---|---|---|
| D1 | `TimeDeposit` and `TimeDepositCalculator` **stay in `org.ikigaidigital`**; all new code lives in sub-packages | Moving them would be a breaking change, which the assignment forbids | `CLAUDE.md`; package layout |
| D2 | `TimeDeposit` is untouched. `TimeDepositCalculator` keeps its `updateBalance(List<TimeDeposit>)` signature and behaviour and gained only an **additional constructor** (it can take a registry) | "No breaking changes" is read as: existing code and callers keep working; additions are allowed. The default constructor still builds the original behaviour | `TimeDepositCalculator`; the frozen tests |
| D3 | One strategy class per plan plus a registry; the 30-day rule exists in **one** place | Meets the "extensible" and SOLID requirements: a new plan is one new class plus registration, with no edits to existing plans | `domain/interest`; a test adds a throwaway `GoldInterest` plan |
| D4 | Hexagonal layout: `adapter.in.web`, `application` (use cases and ports), `domain.interest`, `adapter.out.persistence` | "Embrace Hexagonal Architecture" | README, `docs/walkthrough.md` |
| D5 | The `domain` package has no framework code. The `application` services use Spring's `@Service` and `@Transactional` | Pragmatic: they need transaction management, and the dependency rule that matters (the core knows only the repository interface, never JPA) still holds | `application/*Service` |
| D6 | JPA entities are separate classes from the domain objects, kept package-private inside the adapter | The database model can change without touching the domain | `adapter/out/persistence` |
| D7 | `TimeDepositCalculator` is plain Java, so it is registered as a Spring bean in `ApplicationConfig` instead of being annotated | Keeps the original class free of framework annotations | `ApplicationConfig` |

---

## E. Tooling and process

| # | Choice | Why |
|---|---|---|
| E1 | **Java 17 + Maven** in `java/`, plus a TypeScript solution in `typescript/` (section G); `c#/`, `kotlin/` and `python/` are untouched | Java and Spring are the expected stack for this kind of service; the toolchain was already installed |
| E2 | **Spring Boot 3.5.6**, Spring Data JPA (not plain JDBC) | Conventional for Spring; the entity/adapter split fits hexagonal |
| E3 | **Testcontainers** with real Postgres for persistence and end-to-end tests; `*Test` = fast, `*IT` = Docker (Failsafe, `mvn verify`) | The assignment prefers Testcontainers; no fake database that could behave differently from production |
| E4 | **Characterization tests first**, then refactor; that test class is frozen afterwards | Proves "no breaking changes" and the commit order shows it |
| E5 | **PIT** mutation testing limited to the domain; PIT 1.20.2 | Shows the tests catch changes. Docker-backed tests are excluded to keep runs fast. The mutant count differs between PIT versions (26 on 1.15, 21 on 1.20); both are 100% killed |
| E6 | **Extras beyond the assignment:** ECS JSON logging, GitHub Actions CI, a `docker-compose.yml`, a dev runner (`mvn spring-boot:test-run`) | Common practice for a service like this (log aggregation, CI, automated tests) and they make the project easy to run |
| E7 | **Gatling was not added** | Judged out of scope for this assignment, which has no performance requirement |
| E8 | One **branch per phase**, atomic commits inside, merged with a merge commit (never squash); work done in a personal fork only | "Follow atomic commit practices"; "do not create a PR or a branch in the ikigai-digital repository" |
| E9 | **AI-assisted** with Claude Code; rules in `CLAUDE.md` | Required by the assignment; see `docs/ai-assistance.md` |


---

## F. Out of scope

These were deliberately not built, with the reason:

| # | Not done | Reason |
|---|---|---|
| F1 | Input validation, error responses, exception handling | The assignment says invalid input handling is not required |
| F2 | Authentication and authorization | Not mentioned |
| F3 | **Concurrency control**: two simultaneous updates are not coordinated, so overlapping calls may apply a single month between them | Out of scope; would need locking or an idempotency key. Noted in a code comment |
| F4 | Scheduling the monthly update | The assignment asks for an endpoint, not a scheduler |
| F5 | Creating, changing or deleting individual deposits or withdrawals | "Exactly two endpoints" |
| F6 | Currency handling | Not in the data model |
| F7 | Load or performance tests | See E7 |

---

## G. TypeScript solution

Everything in sections A to F applies to the TypeScript solution too (same contract, same schema, same rules). These are the points that are specific to it.

| # | Ambiguity or choice | Choice | Why | Where |
|---|---|---|---|---|
| G1 | The TS original rounds differently from the Java original: `Math.round((interest + Number.EPSILON) * 100) / 100` | **Assumption:** keep the TS behaviour exactly. Basic 1218 gives 1.015 interest; Java rounds it to 1.01, TS to **1.02** (the EPSILON lifts 101.4999.. to 101.5). | The assignment calls each `updateBalance` correct and forbids changing its behaviour; aligning them would silently change one of them | `TimeDepositCalculator.ts` comment; TS characterization test for 1218 |
| G2 | The starter's `src/index.ts` did not compile (`new TimeDeposit` with three arguments instead of four) | **Assumption:** it is a demo script, not shared API; first fixed minimally so the project type-checks, then replaced by the server start-up | Needed a working entry point; nothing imports it | `src/index.ts` comment |
| G3 | "No breaking changes" in TypeScript | **Decision:** `TimeDeposit.ts` and `TimeDepositCalculator.ts` stay at their paths in `src/` with the same exports, constructor and fields; the calculator only gained an optional constructor argument | Moving a file breaks every import of it, the TS equivalent of changing a Java package | `CLAUDE.md`; frozen tests |
| G4 | node-postgres returns `NUMERIC` as a string | **Decision:** convert with `Number()` when reading and `toFixed(2)` when writing, only in the adapter; dates are read with `to_char` so they are not shifted by the time zone | Keeps the domain on plain numbers, like the shared class | `PostgresTimeDepositRepository` comment; round-trip test |
| G5 | Migrations | **Decision:** the Java Flyway SQL files are reused as they are, applied by a small Flyway-style `SqlMigrator` with its own `schema_history` table | One schema for both solutions; the planned library (node-pg-migrate) is ESM-only in current releases and does not load cleanly in this CommonJS project | `SqlMigrator` comment; migrator test |
| G6 | Transactions without a framework | **Decision:** a `TransactionRunner` port; the Postgres implementation runs the work in `BEGIN`/`COMMIT`/`ROLLBACK` | Same "all or nothing" update as Java's `@Transactional` | `TransactionRunner`; commit and rollback tests |
| G7 | The starter's `test` script only ran in watch mode | **Decision:** `yarn test` runs once (CI needs it to exit); watch mode moved to `yarn test:watch`. Babel-based Jest does not type-check, so `yarn typecheck` (`tsc --noEmit`) was added | Usable in CI; type errors are caught | `package.json` |
| G8 | Ports | **Decision:** app on 3000, local Postgres (docker compose) on 5433 | Both solutions can run at the same time without their migrations clashing | `docker-compose.yml`, `src/index.ts` |
| G9 | Where the contract lives | **Decision:** an identical copy in `typescript/openapi/openapi.yaml`; TS types are generated from it | Each solution builds on its own; the copy is byte-identical today | `openapi/` |
| G10 | Stryker equivalent mutant | **Decision:** `NoInterest.planType` is excluded from mutation with the reason in a comment | The fallback strategy is never registered, so its plan type is never read; no test could tell the difference | `InterestStrategyRegistry.ts` |
