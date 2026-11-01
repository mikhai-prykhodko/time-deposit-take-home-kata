# What we built (phases 0 to 6), explained for non-developers

For the practical how-to (starting the app, database access, Postman, how the generated classes work) see [`guide.md`](guide.md). This is written for readers who know testing and can read code but are not backend developers. Every term is explained when it first appears.

## 1. The big picture

The task: a bank has a small piece of logic that calculates monthly interest on time deposits. It works, but it's one long block of nested `if`s and it's not connected to anything. We have to wrap it in a real service: a database, two web endpoints, and a clean structure, **without changing how the interest is calculated**.

We build it in layers, and we start with the safety net:

| Phase | What | Why this order |
|---|---|---|
| 0 | `CLAUDE.md` and the AI log | Rules written down once, so every AI session follows them |
| 1 | Tests for the **original** code | Proves what "correct" means before anyone touches it |
| 2 | Clean up the interest logic | The only real "business logic" in the task |
| 3 | Database and Spring Boot | The plumbing that stores and loads deposits |
| 4 | The two web endpoints | Last of the build, because they only expose what's below them |
| 5 | End-to-end tests | Check the whole running system, not just the pieces |
| 6 | Logging, CI, documentation | What makes it runnable and reviewable by someone else |

## 2. Phase 0: rules and documentation

- **`CLAUDE.md`**: a file the AI assistant reads automatically at the start of every session. It lists what must not break (the `TimeDeposit` class, the `updateBalance` method), the stack, and the test rules. Think of it as the team's working agreement.
- **`docs/ai-assistance.md`**: the README requires documenting how AI was used. This is that document, to be filled in as we go.

## 3. Phase 1: characterization tests

A **characterization test** doesn't check "is this right?" but "does this still do exactly what it did before?". The README says the original calculator is correct, so we recorded its exact behaviour for:

- each plan type (basic, student, premium);
- the day limits: 30/31, 45/46, 365/366;
- unknown plan types, and wrong letter case (`"Basic"` is not `"basic"`);
- rounding to cents, including one nasty case (see section 4);
- several deposits in one call.

That's 26 tests. We ran them against the **untouched** code and they passed. That's the proof that the tests describe reality.

**PIT (mutation testing)** measures the tests themselves. PIT secretly changes the code in small ways (flips a `>` to `>=`, removes a line) and checks whether any test fails. A change that no test notices is a "surviving mutant" and means a gap. Our result: every mutant was caught (100%).

## 4. Phase 2: cleaning up the interest logic

**Before:** one method with `if student... else if premium... else if basic...`. Adding a plan means editing that method and risking the others.

**After:** one small class per plan, in `org.ikigaidigital.domain.interest`:

| File | Role |
|---|---|
| `InterestStrategy` | The contract: "give me a plan name, and the interest for a month". Also holds the rule shared by all plans: no interest in the first 30 days |
| `BasicInterest` | 1% a year |
| `StudentInterest` | 3% a year, none once the deposit is 366 days or older |
| `PremiumInterest` | 5% a year, only after 45 days |
| `InterestStrategyRegistry` | A lookup table: plan name to the right class. Unknown plan = no interest |

This is the **Strategy pattern**. The payoff is the "open/closed" goal in the README: to add a "gold" plan, you write one new class and register it. You edit no existing class. The test `NewPlanExtensibilityTest` demonstrates exactly this.

`TimeDepositCalculator.updateBalance` (the method we're not allowed to change) keeps its exact signature and now simply asks the registry for the right strategy. The 26 tests from phase 1 passed **without any edit**, which is the whole point.

**The rounding trap:** a balance of 1218 on the basic plan yields interest of 1.015. Computers store that as 1.01499999..., so the original code rounds it **down** to 1.01. A "cleaner" way of converting numbers would have given 1.02 and silently changed customers' money. A dedicated test pins this.

## 5. Phase 3: Spring Boot, the database and the adapter

### What is Spring Boot, and what changed by moving to it?
Before, the project was a plain Java library: a class and its tests, nothing that can "run". **Spring Boot** is a framework (a ready-made toolbox) that turns it into an application that can start, connect to a database and, later, answer web requests. Practically, three things changed:

1. **`pom.xml`** (the Maven build file, the equivalent of `package.json`): it now declares Spring Boot as its "parent", which brings a tested set of compatible library versions. We also added libraries for the web, the database (JPA), Flyway, Postgres and Testcontainers.
2. **`TimeDepositApplication`**: the "start button". It has a `main` method and one annotation, `@SpringBootApplication`. When run, Spring scans the code, finds the parts it should manage and wires them together.
3. **`application.properties`**: the settings file. Here: the database address and credentials, and "let Flyway own the schema; Hibernate may only check it".

Your tests were not affected: the original 37 unit tests still run in about a second and need no database.

### What is Flyway?
The database structure (tables, columns) has to be created somewhere. **Flyway** is a tool that runs numbered SQL scripts in order when the app starts, and remembers which ones already ran. It's version control for the database schema.

- **`V1__create_time_deposits_and_withdrawals.sql`**: creates two tables. `time_deposits` (id, plan type, days, balance) and `withdrawals` (id, which deposit, amount, date). A **foreign key** means a withdrawal can't point to a deposit that doesn't exist. Money columns are `NUMERIC(19,2)`: exact decimal numbers with two decimals, which is how databases should store money.
- **`V2__seed_demo_data.sql`**: six demo deposits and three withdrawals, so the endpoints have something to show. It lives in a separate folder (`db/seed`) that only the running app reads, so tests start from an empty schema and are not affected by demo data.

**Why `time_deposits` and not `timeDeposits` as in the README?** Postgres silently lowercases unquoted names, so `timeDeposits` would become `timedeposits`. We use snake_case in the database and keep camelCase in the web API. A comment in the migration records this assumption.

### What is JPA, an entity, an adapter?
- **JPA / Hibernate** converts between database rows and Java objects, so we don't write SQL by hand for every query.
- **Entity**: a Java class that mirrors one table. `TimeDepositEntity` is the `time_deposits` table, `WithdrawalEntity` is `withdrawals`.
- **`TimeDepositJpaRepository`**: Spring generates the query code from a few method names and one short query.

### What is hexagonal architecture, in plain words?
The core of the application (the interest logic and the use cases) must not know that Postgres, JPA or a web framework exist. The core defines **ports** (what it needs: "give me all deposits", "save these balances"), and the outer pieces, **adapters**, plug into those ports. Benefit: you can swap the database or test the core with a fake, without touching the logic.

In our code:

| File | Layer | Role |
|---|---|---|
| `application/port/out/TimeDepositRepository` | Port | The wish list of the core: `findAll`, `findAllWithWithdrawals`, `updateBalances` |
| `application/TimeDepositDetails` and `Withdrawal` | Core data | A deposit plus its withdrawals, for the GET response |
| `adapter/out/persistence/TimeDepositPersistenceAdapter` | Adapter | Implements the port using JPA and translates between database and domain objects |
| `TimeDepositEntity`, `WithdrawalEntity`, `TimeDepositJpaRepository` | Adapter internals | The database mapping; kept private to the adapter |

**Why a separate `TimeDepositDetails`?** The README wants `withdrawals` in the GET response, but the shared `TimeDeposit` class has no such field and we're forbidden to break it. So withdrawals travel next to it, in a wrapper.

**Double vs decimal:** `TimeDeposit` holds balances as Java `Double` (floating point) and the database uses exact decimals. The adapter converts between them in one place, using a method that avoids ugly values like `1235595.8100000000`.

### What are Testcontainers?
A library that starts a **real Postgres inside Docker** for the duration of a test, then throws it away. That's why Docker had to be running. It's far more trustworthy than a fake in-memory database, because the tests hit the same kind of database as production.

`TimeDepositPersistenceAdapterIT` has four tests: ordering, withdrawals attached to the right deposit, balance updates that don't touch other columns, and a decimal round trip. **Naming convention:** `*Test` = fast, no Docker (`mvn test`); `*IT` = integration, needs Docker (`mvn verify`).

**Honest note on that test:** the first run failed, and the fault was in my test, not the product. Hibernate keeps changes in memory until it "flushes" them, so a direct database read saw the old value. We fixed that, then did a sanity check that a test can fail: we deliberately disabled the adapter's update and 2 of 4 tests went red. A test that can't fail proves nothing.

## 5b. Phase 4: the two endpoints (contract first)

- **OpenAPI** is a standard way to describe a web API in a YAML file: which URLs exist, what you send, what comes back. **Contract first** means we wrote that file (`openapi.yaml`) *before* the code. A Maven plugin then **generates** the Java interface and response classes from it into `target/` (not committed). The controller just implements the generated interface, so the code cannot silently disagree with the contract.
- **Swagger UI** is a web page (served by the `springdoc` library) that reads the contract and lets anyone click "Try it out" to call the endpoints. This is how anyone can try the solution.
- **Use cases** (`ListTimeDepositsUseCase`, `UpdateAllBalancesUseCase`) are the application's two actions. Updating runs in **one transaction**: all balances change or none do. Their tests use a **fake repository** (a simple in-memory list), so they run in milliseconds without Spring or a database.
- **Controller slice test:** starts only the web layer with the actions replaced by mocks; checks the JSON fields, the 204, and that the app has **exactly two endpoints** of its own.

## 5c. Phase 5: end-to-end tests

`TimeDepositApiIT` starts the whole application on a random port with a real Postgres in Docker and calls it over HTTP with REST Assured. Two details worth knowing:
- The **expected balances were computed separately with exact decimal arithmetic**, not copied from the application, so the test is independent of the code it checks.
- Every request and response is also **validated against `openapi.yaml`**. We proved this works by tightening the contract on purpose (`days` max 10) and watching the test fail with a schema error.

`TestTimeDepositApplication` is a small runner so anyone with Docker can start the app with `mvn spring-boot:test-run`, no database install needed.

## 5d. Phase 6: logging, CI, docs

- **ELK / ECS:** ELK is a log search stack (Elasticsearch, Logstash, Kibana). **ECS** (Elastic Common Schema) is a standard JSON format for log lines. One property makes every log line ECS JSON, so ELK can ingest it without custom parsing. The update action logs how many deposits it updated and how long it took.
- **docker-compose.yml:** starts a local Postgres, an alternative to Testcontainers for running the app.
- **GitHub Actions CI:** on every push GitHub runs `mvn verify` and the mutation tests on a clean machine and keeps the reports. A green run proves the project builds without anything from your laptop.
- **README and AI log:** how to run, how to call the endpoints from Swagger, assumptions, and which parts were AI-assisted.

## 6. File map (what's new and where)

```
java/
  pom.xml                                    build file: libraries, test and mutation plugins
  src/main/java/org/ikigaidigital/
    TimeDeposit.java                         UNCHANGED (shared class we must not break)
    TimeDepositCalculator.java               same method signature, now delegates to strategies
    TimeDepositApplication.java              the start button
    domain/interest/                         Strategy classes + registry (pure logic, no frameworks)
    application/                             use cases, ports and the read model
    adapter/in/web/                          controller (implements the generated API)
    adapter/out/persistence/                 database side
  src/main/resources/
    application.properties                   settings
    db/migration/V1__...sql                  schema
    db/seed/V2__...sql                       demo data
    openapi.yaml                             the API contract
  docker-compose.yml                         local Postgres
  src/test/java/...
    TimeDepositCalculatorTest                26 frozen characterization tests
    domain/interest/                         registry and extensibility tests
    adapter/out/persistence/...IT            Testcontainers test for the database adapter
    TimeDepositApiIT                         end-to-end test over HTTP
    TestTimeDepositApplication               dev runner with a throwaway Postgres
.github/workflows/ci.yml                     CI pipeline
```

## 7. Glossary

| Term | Meaning |
|---|---|
| Maven / `pom.xml` | Build tool and its config file (like npm and `package.json`) |
| Spring Boot | Framework that turns a library into a runnable application |
| Annotation (`@Something`) | A label on code that tells the framework what to do with it |
| Bean | An object that Spring creates and manages for you |
| Flyway | Runs versioned SQL scripts to create and evolve the database schema |
| JPA / Hibernate | Maps Java objects to database rows |
| Entity | A Java class that mirrors a database table |
| Port / adapter | Core's interface for outside needs / the plug that implements it |
| Strategy pattern | One class per variant of a rule, picked at runtime |
| Testcontainers | Starts real services (here Postgres) in Docker during tests |
| Failsafe / Surefire | Maven plugins that run `*IT` / `*Test` classes |
| PIT | Mutation testing: checks that your tests notice small code changes |
| Characterization test | A test that records existing behaviour, right or wrong, before refactoring |
| OpenAPI / Swagger UI | Standard API description file / the page that lets you try the API from it |
| Contract first | Write the API description first, generate the code from it |
| ECS | Elastic Common Schema: a standard JSON layout for log lines (for ELK) |
| CI | Continuous integration: the pipeline that builds and tests on every push |
