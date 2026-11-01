# AI-Assisted Development

## Setup
- **Tool:** Claude Code (Anthropic) in the Claude desktop app, Code tab, working directly in this repository with file, shell, Maven and Docker access.
- **Models:** Claude Opus 5.5 (`claude-opus-5-5`) for planning and plan review, Claude Sonnet 5.5 (`claude-sonnet-5-5`) for implementation; switched with `/model`.
- **Rules / system prompt:** [`CLAUDE.md`](../CLAUDE.md) at the repo root. Claude Code loads it automatically into every session, so the constraints apply to all AI work: frozen `TimeDeposit` and `updateBalance`, exactly two endpoints, test-first order, conventions for tests and commits, interest rules verified against the legacy code.
- **Workflow:**
  1. Read the assignment, review the task and agree scope (language, endpoints, persistence, extras).
  2. **Plan mode:** the AI writes a plan, which is reviewed and corrected before any code.
  3. One branch per phase, atomic commits inside it, merged into `main` with a merge commit (never squash).
  4. Every change is run (`mvn test` / `mvn verify` / PIT) before it is committed. Test claims are checked by sabotage: deliberately breaking the code or the contract and confirming the tests go red.

## Reproducing the setup
1. Install Claude Code: https://claude.com/claude-code
2. Open the repo root, so `CLAUDE.md` is picked up automatically.
3. Ask for the next step of the plan; review the diff, run `cd java && mvn verify` (Docker running), then commit.

No other configuration, MCP server or custom agent is needed; the whole rule set is `CLAUDE.md`.

## What was AI-assisted, and why

Everything in the repository was written with the AI. The candidate decided scope and reviewed, ran, merged and questioned the results (see the last section).

| Phase | AI role | Why AI was used |
|---|---|---|
| 0: `CLAUDE.md`, this log | Drafted the rules from the README and from reading the legacy code | Writes down constraints once so every session follows them |
| 1: characterization tests, PIT | Generated the test matrix and ran the **original** calculator to record exact outputs; set up PIT | Tedious boundary enumeration; the AI could execute the legacy code to get real expected values |
| 2: interest strategies | Designed the strategy and registry classes and refactored the calculator | Mechanical refactor guarded by the frozen tests |
| 3: Spring Boot, Flyway, JPA adapter, Testcontainers | Wrote the Maven setup, migrations, entities, adapter, integration test | Boilerplate-heavy, and the candidate is a QE rather than a backend developer |
| 4: OpenAPI, use cases, controller | Wrote the contract, generator setup, services, fake repository and slice tests | Same boilerplate reason |
| 5: end-to-end test, dev runner | Wrote the REST Assured test with contract validation; started the real app and called both endpoints | Wiring a full-stack test is fiddly |
| 6: ECS logging, compose, CI, README | Wrote configuration and documentation | Boilerplate and writing |
| Docs: guide, assumptions | Wrote `docs/guide.md` and `docs/assumptions.md`, ran every command in the guide, added the missing assumption comments to the code | The candidate asked for a full how-to and one place listing every assumption with its reasoning |

## AI mistakes caught

| What went wrong | How it was caught | Resolution |
|---|---|---|
| First plan put the new domain classes in a new package, which would have moved `TimeDeposit` and broken the "no breaking changes" rule; also ignored that Postgres lowercases `timeDeposits` and that the update is not idempotent | Plan review before any code | Plan corrected: shared classes stay in `org.ikigaidigital`, snake_case tables, assumptions documented |
| Suggested opening pull requests with `gh`; opening a PR from a fork defaults to the upstream repository, which the assignment forbids | The candidate asked where the PR would be created | Switched to explicit fork-only instructions, then local `--no-ff` merges or PRs with the base repository set to the fork |
| After the Spring Boot upgrade PIT crashed (the JUnit version Boot manages is too new for PIT 1.15.8); the AI's command chain **committed anyway** | Read the error output after the commit | Bumped PIT to 1.20.2, amended the unpushed commit, noted the new baseline count in `CLAUDE.md` |
| Flyway files were written to a stray `src/` at the repo root instead of `java/src/` | `git` reported the wrong working directory | Moved the files and re-applied the missing property |
| The persistence test failed on its first run: it read through JDBC before Hibernate flushed, and a second test could have been served from Hibernate's cache | Test run output | Flush and clear the persistence context in the test; then verified the test can fail by disabling the adapter's update (2 of 4 tests went red) |
| The controller file was not written (missing `mkdir`) | Compilation error on the next run | Created the directory and wrote the file again |
| First attempt to prove the contract check works (changing `balance` to a string) broke compilation, so it never tested the validator | Output showed no test results | Used a change that still compiles (`maximum: 10` on `days`); the validator then failed with a schema error as expected |

| AI-written docs contained the candidate's private interview-preparation notes (questions for the reviewer, talking points) | The candidate's review before merging | Moved out of the repository into a local, git-excluded file |
| The README described the controller as "generated from openapi.yaml"; only its interface is generated | Final review of the README against the code | Corrected the wording |

## What stayed human
- Scope decisions: Java and Spring Boot, endpoint paths, JPA, which QE extras to include (PIT, CI, ECS logging) and which to skip (Gatling).
- Reviewing every phase and merging it into `main` of the fork.
- Running Docker and trying the endpoints.
- Questioning AI proposals, for example the pull request target.
