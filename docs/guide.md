# Full guide: generated classes, running the app, database and API

Everything here was checked against the real project (commands run, output compared). Paths are relative to the repository root unless stated.

Sections 1 to 10 cover the Java solution; [section 11](#11-typescript-solution) covers the TypeScript one.

Contents
1. [The generated classes](#1-the-generated-classes)
2. [Prerequisites](#2-prerequisites)
3. [Starting the application](#3-starting-the-application)
4. [The database](#4-the-database)
5. [Calling the API](#5-calling-the-api)
6. [How a request flows through the code](#6-how-a-request-flows-through-the-code)
7. [Tests](#7-tests)
8. [Configuration reference](#8-configuration-reference)
9. [Troubleshooting](#9-troubleshooting)
10. [Cheat sheet](#10-cheat-sheet)
11. [TypeScript solution](#11-typescript-solution)

---

## 1. The generated classes

### What you see and why it is not in git
After a build you find these files under `java/target/generated-sources/openapi/src/main/java/org/ikigaidigital/adapter/in/web/api/`:

| Generated file | What it is |
|---|---|
| `TimeDepositsApi.java` | A Java **interface** with the two endpoints: the URL, HTTP method, response codes and Swagger descriptions of each |
| `model/TimeDepositResponse.java` | The response object for one deposit: `id, planType, balance, days, withdrawals` |
| `model/WithdrawalResponse.java` | One withdrawal: `id, amount, date` |
| `ApiUtil.java` | Small helper the generator always emits; unused by us |

(The generator also writes a `pom.xml`, `README.md` and a `.openapi-generator/` folder next to them. They are noise and are not used.)

They live in `target/`, the build output folder, and `target/` is in `.gitignore`. That is deliberate: they are **derived** from a file that *is* committed, so committing them would create two sources of truth that could disagree.

### The source of truth: `java/src/main/resources/openapi.yaml`
This file *is* the API contract. It describes, in a standard format (OpenAPI 3):
- the two paths (`/time-deposits`, `/time-deposits/balance-updates`) and their HTTP methods;
- the response codes (`200` with a list, `204` with no body);
- the shape of a deposit and of a withdrawal (`components/schemas`).

This approach is called **contract first**: write the contract, generate code from it, then implement the generated interface.

### How they get generated
A Maven plugin, `openapi-generator-maven-plugin`, is configured in `java/pom.xml`. It is bound to Maven's **`generate-sources`** phase, which runs early in every build, before compilation:

```
mvn generate-sources   ->  reads openapi.yaml  ->  writes the classes into target/generated-sources/openapi
mvn compile            ->  runs generate-sources first, then compiles our code + the generated code
mvn test / verify      ->  also run it (they include compile)
```

The plugin settings in `pom.xml` and what they mean:

| Setting | Effect |
|---|---|
| `inputSpec` = `src/main/resources/openapi.yaml` | The contract to read |
| `generatorName` = `spring` | Produce Spring MVC code |
| `apiPackage` / `modelPackage` | Java packages for the interface and for the response classes |
| `interfaceOnly` = true | Generate only the interface and the models, not a ready-made server. We write the controller |
| `useSpringBoot3` = true | Use the `jakarta.*` packages that Spring Boot 3 needs |
| `useTags` = true | One interface per tag in the contract (tag `TimeDeposits` becomes `TimeDepositsApi`) |
| `skipDefaultInterface` = true | The methods are abstract, so if the contract changes and the controller is not updated, the **build fails** instead of silently doing nothing |
| `openApiNullable` = false | Plain fields instead of a `JsonNullable` wrapper |
| `documentationProvider` = `springdoc` | Adds Swagger annotations, which is how Swagger UI knows the descriptions |

### If you just downloaded (cloned) the project
You do **nothing special**. A fresh clone has no `target/` folder; I tested this: after `git clone`, a plain

```bash
cd java
mvn compile
```

creates the four generated files and compiles everything. Any Maven command that compiles (`test`, `verify`, `spring-boot:run`) does the same. To generate without compiling:

```bash
mvn generate-sources
```

To throw away and regenerate everything from scratch:

```bash
mvn clean generate-sources
```

### How our code refers to them

| Contract element (`openapi.yaml`) | Generated | Used in |
|---|---|---|
| tag `TimeDeposits` | interface `TimeDepositsApi` | `TimeDepositController implements TimeDepositsApi` |
| operation `listTimeDeposits` (GET) | method `listTimeDeposits()` | controller overrides it and returns `ResponseEntity<List<TimeDepositResponse>>` |
| operation `updateAllBalances` (POST) | method `updateAllBalances()` | controller overrides it and returns `204` |
| schema `TimeDepositResponse` | class `TimeDepositResponse` | controller's `toResponse(...)` builds it from the domain data |
| schema `WithdrawalResponse` | class `WithdrawalResponse` | same |

Because the controller *implements* the interface, the URLs and methods are not written by hand anywhere in our code; they come from the contract.

### Changing the API
1. Edit `java/src/main/resources/openapi.yaml`.
2. Run `mvn compile`. The classes are regenerated; if the controller no longer matches, **the compiler tells you exactly what to change**.
3. Update `TimeDepositController` (in `adapter/in/web`).
4. Run `mvn verify`. The end-to-end test validates every request and response against the contract, so a mismatch fails the test.

### Rules and IDE notes
- **Never edit generated files.** The next build overwrites them. Change the contract instead.
- Use **IntelliJ IDEA** for the Java code (WebStorm does not support Java projects). Import `java/pom.xml` as a Maven project; the generated folder is picked up automatically as a source root. If the IDE says it "cannot find `TimeDepositsApi`", run `mvn generate-sources` once and reload the Maven project.
- You can see what the generator produces for any contract change without running the app: `mvn generate-sources` then open the files under `target/`.

---

## 2. Prerequisites

| Need | Version | Check |
|---|---|---|
| JDK | 17 | `java -version` |
| Maven | 3.9+ | `mvn -v` |
| Docker | running (Docker Desktop on Mac) | `docker info` |

Docker is needed for the database (options A and B) and for the integration tests. The first run downloads the `postgres:16-alpine` image, so it is slower.

---

## 3. Starting the application

All commands run from the `java/` folder. The app listens on **http://localhost:8080**.

### Option A: Testcontainers (simplest, no install)
```bash
cd java
mvn spring-boot:test-run
```
- A throwaway Postgres container is started **for you** and wired into the app automatically.
- The demo data is loaded.
- When you stop the app (Ctrl+C), the container is removed and **all data is lost**. The next start begins from the demo data again.

### Option B: docker compose (a database that stays up)
```bash
cd java
docker compose up -d        # starts Postgres on localhost:5432
mvn spring-boot:run         # starts the app
# ...
docker compose down         # stops AND deletes the database
```
- Use this when you want to keep the database running, look at it with your own tool on a fixed port, or restart the app without losing data.
- Stopping only the app keeps the data. Starting it again does **not** reload the demo data (Flyway already ran), so balances you updated stay updated. For a clean reset run `docker compose down` and `docker compose up -d` again.

### Option C: your own Postgres
Create an empty database and point the app at it with environment variables (Spring reads these automatically):
```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/mydb
export SPRING_DATASOURCE_USERNAME=myuser
export SPRING_DATASOURCE_PASSWORD=mypassword
mvn spring-boot:run
```
Flyway creates the tables on first start.

### Running a built jar
```bash
cd java
mvn -DskipTests package
java -jar target/time-deposit-kata-1.0-SNAPSHOT.jar     # needs a database as in option B or C
# another port:  java -jar target/time-deposit-kata-1.0-SNAPSHOT.jar --server.port=8081
```

### Is it running?
```bash
curl -i localhost:8080/time-deposits
```
Expect `200` and JSON. In the console you will see ECS-format JSON log lines (see [section 8](#8-configuration-reference)).

### Changing the port
If 8080 is busy:
```bash
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

---

## 4. The database

Postgres 16. Schema is created by Flyway at startup from `java/src/main/resources/db/migration/V1__...sql`; demo data comes from `db/seed/V2__...sql` (only when the app runs, not in tests).

### Tables
```
time_deposits                      withdrawals
-----------------                  -----------------------------
id        integer  PK              id              integer  PK
plan_type varchar  not null        time_deposit_id integer  FK -> time_deposits.id
days      integer  not null        amount          numeric(19,2)
balance   numeric(19,2)            date            date
```
(Names are snake_case on purpose; see the assumptions in the README.)

### Demo data

| id | plan | days | balance | withdrawals |
|---|---|---|---|---|
| 1 | basic | 45 | 1000.00 | 100.00 (2024-01-15), 50.00 (2024-02-20) |
| 2 | student | 100 | 2000.00 | none |
| 3 | premium | 100 | 5000.00 | 250.00 (2024-03-05) |
| 4 | basic | 20 | 1500.00 | none |
| 5 | student | 400 | 3000.00 | none |
| 6 | premium | 40 | 8000.00 | none |

### Connecting to the database

**Option B (docker compose): fixed details**

| | |
|---|---|
| Host / port | `localhost` / `5432` |
| Database | `timedeposit` |
| User / password | `timedeposit` / `timedeposit` |
| JDBC URL | `jdbc:postgresql://localhost:5432/timedeposit` |

```bash
cd java
docker compose exec postgres psql -U timedeposit -d timedeposit
```

**Option A (Testcontainers): random port**

Testcontainers maps the database to a **random** free port on your machine, so look it up:
```bash
docker ps --format '{{.Names}}  {{.Image}}  {{.Ports}}'
# example line:  bold_goodall  postgres:16-alpine  0.0.0.0:58712->5432/tcp   (the port changes every run)
```

| | |
|---|---|
| Host / port | `localhost` / the left port from `docker ps` (58712 in the example) |
| Database | `test` |
| User / password | `test` / `test` |

Quickest way in, no port needed:
```bash
docker exec -it $(docker ps --filter ancestor=postgres:16-alpine -q | head -1) psql -U test -d test
```

**GUI tools** (DBeaver, DataGrip, IntelliJ Database tab, pgAdmin): create a new PostgreSQL connection with the host, port, database, user and password from the tables above.

### Handy queries
```sql
select id, plan_type, days, balance from time_deposits order by id;
select * from withdrawals order by id;
select version, description, success from flyway_schema_history;   -- which migrations ran
update time_deposits set balance = 1000.00 where id = 1;          -- reset one balance by hand
```

---

## 5. Calling the API

Two endpoints, nothing else:

| Method | URL | Body | Success |
|---|---|---|---|
| `GET` | `/time-deposits` | none | `200` JSON list |
| `POST` | `/time-deposits/balance-updates` | none | `204`, empty response |

No authentication, no headers, no request bodies are needed.

### Swagger UI (click-through)
1. Open **http://localhost:8080/swagger-ui.html**.
2. **GET /time-deposits** > **Try it out** > **Execute**. Note the balances.
3. **POST /time-deposits/balance-updates** > **Try it out** > **Execute**. Expect `204`.
4. Execute the GET again and compare.

The raw contract is served at **http://localhost:8080/v3/api-docs** (JSON) and lives in the repo as `java/src/main/resources/openapi.yaml`.

### curl
```bash
curl -i localhost:8080/time-deposits
curl -i -X POST localhost:8080/time-deposits/balance-updates
curl -s localhost:8080/time-deposits | python3 -m json.tool      # pretty-printed
```

### Postman (or Insomnia, Bruno, Hoppscotch)
**Manually**
1. New request, method `GET`, URL `http://localhost:8080/time-deposits`, **Send**.
2. New request, method `POST`, URL `http://localhost:8080/time-deposits/balance-updates`, leave Body empty, **Send**. Status `204 No Content` and an empty body is the correct result.

**Import the whole contract** (creates both requests for you)
- *Import > Link*: `http://localhost:8080/v3/api-docs` (the app must be running), or
- *Import > File*: `java/src/main/resources/openapi.yaml`.

If the imported requests show a `{{baseUrl}}` variable, set it to `http://localhost:8080` (collection variables).

**Handy Postman test script** for the GET request (Tests tab):
```javascript
pm.test("status 200", () => pm.response.to.have.status(200));
pm.test("deposit shape", () => {
  const first = pm.response.json()[0];
  pm.expect(first).to.have.all.keys("id", "planType", "balance", "days", "withdrawals");
});
```

### Expected results with the demo data

Before the first POST, `balance` is as in the demo data table. After **one** POST:

| id | plan, days | balance after | why |
|---|---|---|---|
| 1 | basic, 45 | **1000.83** | 1000 x 1% / 12 = 0.83 |
| 2 | student, 100 | **2005.00** | 2000 x 3% / 12 = 5.00 |
| 3 | premium, 100 | **5020.83** | 5000 x 5% / 12 = 20.83 |
| 4 | basic, 20 | 1500.00 | first 30 days earn nothing |
| 5 | student, 400 | 3000.00 | student plan earns nothing from day 366 |
| 6 | premium, 40 | 8000.00 | premium earns nothing until day 46 |

Every further POST applies **another** month on top (it is not idempotent), and `days` and the withdrawals never change.

### Example response (GET)
```json
[
  {
    "id": 1,
    "planType": "basic",
    "balance": 1000.0,
    "days": 45,
    "withdrawals": [
      { "id": 1, "amount": 100.0, "date": "2024-01-15" },
      { "id": 2, "amount": 50.0,  "date": "2024-02-20" }
    ]
  }
]
```

---

## 6. How a request flows through the code

`POST /time-deposits/balance-updates`:

```
HTTP request
  -> TimeDepositController            adapter/in/web        (implements the generated TimeDepositsApi)
  -> UpdateAllBalancesService         application           (one transaction)
       1. repository.findAll()                              -> TimeDepositPersistenceAdapter -> Postgres
       2. TimeDepositCalculator.updateBalance(deposits)     -> InterestStrategyRegistry -> Basic/Student/PremiumInterest
       3. repository.updateBalances(deposits)               -> TimeDepositPersistenceAdapter -> Postgres
       4. log "Balance update finished: N time deposits updated in X ms"
  <- 204 No Content
```

`GET /time-deposits`: controller -> `ListTimeDepositsService` -> `repository.findAllWithWithdrawals()` -> adapter reads both tables -> controller converts each `TimeDepositDetails` into a generated `TimeDepositResponse`.

The core (`domain`, `application`) knows only the `TimeDepositRepository` interface, never JPA or Postgres. See `docs/walkthrough.md` for the plain-language explanation of each layer.

---

## 7. Tests

```bash
cd java
mvn test                                       # unit tests, no Docker
mvn verify                                     # unit + Docker-backed *IT tests (54 in total)
mvn org.pitest:pitest-maven:mutationCoverage   # mutation testing; report in target/pit-reports/<timestamp>/index.html
mvn -Dtest=TimeDepositCalculatorTest test      # one test class
```
Naming rule: `*Test` = fast, no Docker; `*IT` = needs Docker (`mvn verify`). Details are in the README.

---

## 8. Configuration reference

`java/src/main/resources/application.properties`:

| Property | Value | Meaning |
|---|---|---|
| `spring.datasource.url/username/password` | `localhost:5432/timedeposit` | Default database (docker compose). Override with `SPRING_DATASOURCE_*` environment variables |
| `spring.jpa.hibernate.ddl-auto` | `validate` | Hibernate only checks the tables; Flyway owns the schema |
| `spring.jpa.open-in-view` | `false` | No database access during JSON rendering |
| `spring.flyway.locations` | `db/migration` + `db/seed` | Schema plus demo data. Tests use `db/migration` only |
| `logging.structured.format.console` | `ecs` | Log lines are Elastic Common Schema JSON (ready for ELK) |
| `server.port` | not set, default `8080` | Override with `--server.port=` |

Log lines look like this (one per line in the console; ELK/Filebeat can ingest them as they are):
```json
{"@timestamp":"...","log":{"level":"INFO","logger":"org.ikigaidigital.application.UpdateAllBalancesService"},"message":"Balance update finished: 6 time deposits updated in 30 ms","service":{"name":"time-deposit"},"ecs":{"version":"8.11"}}
```

---

## 9. Troubleshooting

| Symptom | Cause and fix |
|---|---|
| `Could not find a valid Docker environment` or `Cannot connect to the Docker daemon` | Docker is not running. Start Docker Desktop, wait until `docker info` works |
| `Port 8080 was already in use` | Another app uses it. Stop it, or start on another port (section 3) |
| `Connection refused` to `localhost:5432` when using `spring-boot:run` | The database is not up. Run `docker compose up -d` first, or use `spring-boot:test-run` |
| `port is already allocated` on compose | Something else uses 5432 (a local Postgres). Stop it or change the left port in `java/docker-compose.yml` |
| IDE: `cannot find symbol TimeDepositsApi` | Generated classes not built yet. Run `mvn generate-sources`, then reload the Maven project in the IDE |
| Balances look wrong after several runs | Every POST adds another month. Reset: restart (option A) or `docker compose down` then `up -d` (option B) |
| First run is very slow | Maven and Docker are downloading dependencies and the `postgres:16-alpine` image; later runs are fast |
| Integration tests fail on a CI or sandbox without Docker | They need Docker. `mvn test` still runs all unit tests |

---

## 10. Cheat sheet

```bash
# build and generate
cd java && mvn clean generate-sources        # regenerate classes from openapi.yaml
cd java && mvn compile                       # generate + compile

# run
mvn spring-boot:test-run                     # app + throwaway DB (data lost on stop)
docker compose up -d && mvn spring-boot:run  # app + persistent DB

# try
open http://localhost:8080/swagger-ui.html
curl localhost:8080/time-deposits
curl -X POST localhost:8080/time-deposits/balance-updates -i

# inspect the database (option B)
docker compose exec postgres psql -U timedeposit -d timedeposit

# test
mvn test            # fast
mvn verify          # + Docker tests
mvn org.pitest:pitest-maven:mutationCoverage
```

---

## 11. TypeScript solution

Same endpoints, contract, schema and demo data as the Java solution; only the tooling differs. All commands run from `typescript/`.

### Prerequisites
Node 24 (22 or later), Yarn 1 (`npm install --global yarn`), Docker. Then `yarn install`.

### Generated types (the TS equivalent of section 1)
- The contract is `typescript/openapi/openapi.yaml`, an identical copy of the Java one.
- `yarn generate` runs `openapi-typescript` and writes `src/generated/api.ts` (git-ignored). You never run it by hand: `yarn typecheck`, `yarn test`, `yarn test:it`, `yarn start`, `yarn dev` and `yarn dev:containers` all run it first (`pre*` scripts).
- `src/adapters/in/http/createApp.ts` uses the generated `components['schemas']['TimeDepositResponse']` type for the GET response, so a contract change that the code does not follow is a type error in `yarn typecheck`.
- After a fresh clone: `yarn install && yarn typecheck` is enough.

### Starting the application (port 3000)
| Option | Commands | Database |
|---|---|---|
| A: Testcontainers | `yarn dev:containers` | Throwaway Postgres on a random port, user/password/db `test`, demo data loaded; gone when you stop the app |
| B: docker compose | `docker compose up -d` then `yarn start` (`docker compose down` to reset) | `localhost:5433`, db/user/password `timedeposit`; data stays while the container exists |
| C: your own Postgres | `DATABASE_URL=postgres://user:pass@host:5432/db yarn start` | Tables are created on first start |

Other settings: `PORT` (default 3000). Option A prints the database URL on start-up. With option B: `docker compose exec postgres psql -U timedeposit -d timedeposit`.

Applied migrations are recorded in the `schema_history` table (the Java solution uses Flyway's `flyway_schema_history`).

### Calling the API
- Swagger UI: <http://localhost:3000/swagger-ui>; raw contract: <http://localhost:3000/api-docs>.
- curl: `curl localhost:3000/time-deposits` and `curl -X POST localhost:3000/time-deposits/balance-updates -i`.
- Postman: the same requests as in section 5 with port **3000**, or import `http://localhost:3000/api-docs`.
- Expected balances after one POST on the demo data are the same as in section 5 (1000.83, 2005, 5020.83; the others unchanged). TypeScript prints whole numbers without decimals (`2005`, not `2005.0`).

### Tests
```bash
yarn typecheck      # tsc --noEmit
yarn test           # unit tests (no Docker)
yarn test:watch     # unit tests in watch mode
yarn test:it        # Postgres and end-to-end tests (Docker)
yarn verify         # all of the above
yarn mutation       # Stryker; HTML report in reports/mutation/index.html
```

### Logging
pino with the Elastic ECS formatter, one JSON object per line, for example:
```json
{"log.level":"info","@timestamp":"...","ecs.version":"8.10.0","service.name":"time-deposit","message":"Balance update finished: 6 time deposits updated in 7 ms"}
```

### Troubleshooting
| Symptom | Fix |
|---|---|
| `Cannot find module '../../../generated/api'` in the IDE | Run `yarn generate` once (any `yarn test`/`typecheck` does it too) |
| `ECONNREFUSED 127.0.0.1:5433` on `yarn start` | Start the database: `docker compose up -d`, or use `yarn dev:containers` |
| `EADDRINUSE :3000` | Another app uses the port: `PORT=3001 yarn start` |
| Integration tests fail with a Docker error | Start Docker Desktop; `yarn test` still runs the unit tests |

