# References — Cross-skill and Convention Documentation

> Local paths to canonical sources. Do NOT duplicate their content — read them directly when needed.

---

## Assertion quality for `check()` calls (MANDATORY)

Every `check()` function in the generated k6 script must follow the strict-tdd assertion quality rules. Banned patterns include tautologies, type-only assertions, and smoke-only checks. Required: concrete expected values that fail if the backend returns wrong data.

- **File**: `~/.claude/skills/sdd-apply/strict-tdd.md`
- **Relevant sections**:
  - Lines 205–350 — Assertion Quality Rules
  - Lines 290–325 — Empty Collection Rule, Smoke Test Rule

---

## Engram topic key conventions

This skill saves reports under `stress-tests/{target-slug}`. No `sdd/` prefix — it is not an SDD artifact.

- **File**: `~/.claude/skills/_shared/engram-convention.md`
- **Relevant sections**:
  - Lines 7–15 — Naming rules
  - Lines 47–66 — Recovery protocol (`mem_search` + `mem_get_observation`)
  - Lines 118–120 — Upsert behavior (same topic_key = overwrite)

---

## Return envelope format

Standard shape the skill must return to the orchestrator.

- **File**: `~/.claude/skills/_shared/sdd-phase-common.md`
- **Relevant section**: Section D — Return Envelope (status, executive_summary, artifacts, next_recommended, risks, skill_resolution)

---

## Skill structure template

If extending this skill (adding new `scenarios/*.md` or `assets/*.md` for WebSocket, gRPC, distributed k6, etc.), follow the Agent Skills spec template.

- **File**: `~/.claude/skills/skill-creator/SKILL.md`
- **Relevant sections**:
  - Lines 29–40 — Skill structure
  - Lines 107–117 — Frontmatter fields
  - Lines 119–133 — Content guidelines

---

## Sibling skill — `integration-test`

The integration-test skill generates HTTP contract tests (functional correctness). This skill generates performance tests (non-functional correctness). They complement each other:

- **integration-test**: "Does the endpoint return the correct response?" (Testcontainers + MockMvc)
- **stress-test**: "Does the endpoint return correctly UNDER LOAD, over TIME, under BURSTS?" (k6 + CI gate)

When generating k6 endpoint functions, reuse the list of endpoints discovered by `integration-test` if the user has run it on the same controller. The discovery logic lives in:

- **File**: `~/.claude/skills/integration-test/stacks/java-spring.md`
  - Controller parsing heuristics

Many of the request shapes (DTO schemas, auth requirements) overlap — treat integration-test as the authoritative endpoint catalog when both skills target the same codebase.

---

## Companion skills

| Skill | Role | Relationship |
|-------|------|--------------|
| `sdd-apply` | Writes unit tests (TDD RED/GREEN/REFACTOR) | Smallest scope |
| `integration-test` | HTTP contract tests | Mid scope |
| `stress-test` | Performance + SLO gate | Largest scope (this skill) |
| `sdd-verify` | Validates everything against specs | Runs unit + integration during verify |
| `judgment-day` | Adversarial review | Can validate generated tests are not trivial |

---

## External references (consult only when debugging)

These are NOT local files. Do not fetch them during normal operation. Use only when the user asks for background or when debugging.

- k6 documentation: <https://k6.io/docs/>
- k6 executors reference: <https://k6.io/docs/using-k6/scenarios/executors/>
- k6 thresholds: <https://k6.io/docs/using-k6/thresholds/>
- k6 metrics: <https://k6.io/docs/using-k6/metrics/>
- Grafana k6 examples: <https://github.com/grafana/k6/tree/master/examples>
