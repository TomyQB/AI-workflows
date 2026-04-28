# References — Cross-skill and Convention Documentation

> Local paths to canonical sources. Do NOT duplicate their content — read them directly when needed.

---

## Assertion quality rules (MANDATORY)

Full catalog of banned patterns, required assertions, and the "mutation test" discipline that every generated test must satisfy.

- **File**: `~/.claude/skills/sdd-apply/strict-tdd.md`
- **Relevant sections**:
  - Lines 205–350 — "Assertion Quality Rules"
  - Lines 290–325 — "Empty Collection Rule", "Smoke Test Rule"
  - Lines 326–350 — "Implementation Detail Coupling Rule"

Whenever generating a test, re-read the Assertion Quality section if unsure.

---

## Engram topic key conventions

Namespace and naming rules for engram observations. This skill uses `integration-tests/{controller-slug}` (no `sdd/` prefix — it is not an SDD artifact).

- **File**: `~/.claude/skills/_shared/engram-convention.md`
- **Relevant sections**:
  - Lines 7–15 — Naming rules
  - Lines 47–66 — Recovery protocol (search + get_observation)
  - Lines 118–120 — Upsert behavior (same topic_key = overwrite)

---

## Return envelope format

The standard shape the skill must return to the orchestrator (Section D of sdd-phase-common).

- **File**: `~/.claude/skills/_shared/sdd-phase-common.md`
- **Relevant section**: Section D — Return Envelope (status, executive_summary, artifacts, next_recommended, risks, skill_resolution)

---

## Skill structure template

If extending this skill or creating sibling stack modules, follow the Agent Skills spec template.

- **File**: `~/.claude/skills/skill-creator/SKILL.md`
- **Relevant sections**:
  - Lines 29–40 — Skill structure (SKILL.md / assets/ / references/)
  - Lines 107–117 — Frontmatter fields
  - Lines 119–133 — Content guidelines

---

## SDD phase conventions

How SDD phases pass artifacts, resolve skills, and handle persistence. This skill is not an SDD phase but follows the same conventions where applicable (return envelope, engram retrieval protocol).

- **File**: `~/.claude/skills/_shared/sdd-phase-common.md`
- **Relevant sections**:
  - Section A — Skill loading priority
  - Section B — Artifact retrieval (`mem_search` + `mem_get_observation`)
  - Section C — Persistence via `mem_save`
  - Section D — Return envelope

---

## Stack detection reference

How sdd-init detects project stack, HTTP framework, test runner, and DB. This skill reuses those heuristics in Step 1 (`SKILL.md`).

- **File**: `~/.claude/skills/sdd-init/SKILL.md`
- **Relevant sections**:
  - Lines 42–91 — Project context detection

---

## Companion skills

| Skill | Role | Relationship |
|-------|------|--------------|
| `sdd-apply` | Writes unit tests via RED/GREEN/REFACTOR | Produces the layer BELOW this skill — unit coverage |
| `sdd-verify` | Runs the full suite + checks compliance | Runs integration tests generated here as part of its test execution |
| `judgment-day` | Adversarial review of code | Can be used to validate generated tests are not trivial |
| `skill-creator` | Meta-skill for creating skills | Used to bootstrap this skill |

---

## External references (not files — consult when debugging)

These are NOT local files. Do not fetch them during normal operation. Use only when the user asks for background or when debugging an unusual failure.

- Testcontainers Java docs: <https://java.testcontainers.org/>
- WireMock docs: <https://wiremock.org/docs/>
- Spring Boot testing guide: <https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing>
- Spring Security Test: <https://docs.spring.io/spring-security/reference/servlet/test/index.html>
