# AI-Assisted Development

## Setup
- **Tool:** Claude Code (Anthropic) in the Claude desktop app, Code tab.
- **Model:** Claude Opus 5.5 (`claude-opus-5-5`).
- **Rules / system prompt:** [`CLAUDE.md`](../CLAUDE.md) at the repo root. Claude Code loads it automatically into every session, so the constraints (frozen `updateBalance`, exactly two endpoints, test-first order) apply to all AI work.
- **Workflow:** plan mode first (the plan is reviewed and approved before any code), then one atomic commit per plan step. Every AI-generated change is reviewed and tests are run before committing.

## Reproducing the setup
1. Install Claude Code: https://claude.com/claude-code
2. Open the repo root, so `CLAUDE.md` is picked up.
3. Ask for the next step of the plan; review the diff and run `mvn verify` before each commit.

## What was AI-assisted, and why

| Commit / area | AI role | Human role | Why |
|---|---|---|---|
| `CLAUDE.md`, plan | Drafted from the README | Reviewed scope decisions (endpoints, JPA, extras) | Captures constraints once, so every session follows them |
| _to be filled as work proceeds_ | | | |

## AI mistakes caught

| What the AI suggested | How it was caught | Fix |
|---|---|---|
| _to be filled_ | | |
