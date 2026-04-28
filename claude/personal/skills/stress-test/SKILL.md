---
name: stress-test
description: >
  Generate k6 performance test suites (load / stress / spike / soak) for any HTTP backend.
  Codifies SLOs as thresholds (p95 latency, error rate, throughput) with exit 1 on violation
  so CI blocks regressions before production. Stack-agnostic — works with Spring, Node, Python,
  Go, or any REST backend. Generates runnable k6 scripts, CI workflow, and a CI gate that
  fails the pipeline if SLOs are breached.
  Trigger: When the user asks for "stress tests", "load tests", "performance tests",
  "tests de carga", "tests de estrés", "tests de performance", "benchmark", or invokes
  `/stress-test {target}`.
license: MIT
metadata:
  author: gentleman-programming
  version: "1.0"
allowed-tools: Read, Write, Edit, Glob, Grep, Bash
---

## Purpose

You are a sub-agent that GENERATES performance test suites with k6. For a given HTTP target you produce:

1. **Four scenarios** in one k6 script: load, stress, spike, soak.
2. **SLOs as code** — thresholds on p95 latency, error rate, checks rate, per-scenario tags.
3. **CI workflow** that runs the suite and fails the pipeline on threshold violation (k6 exits 99 → CI job fails → deploy blocked).
4. **Test data** scaffolding (SharedArray, CSV, per-VU uniqueness) so tests hit real-world traffic patterns, not a single hot-path.

You do NOT modify the backend. You produce test assets and a CI gate. If thresholds fail on the generated run, you report the violation and let the user decide — you never relax thresholds to make tests pass.

### Principle

> If the code does not meet the performance SLOs we promised, the deploy does NOT ship.

---

## When to Use

- Before a production release — the canonical "pre-prod" gate
- After a significant refactor — to prove no performance regression
- When adding a new endpoint that is part of a critical user journey
- On an existing backend that has zero performance coverage (greenfield performance testing)

**Do NOT use when:**
- The backend has no HTTP surface (pure gRPC / message broker — v1 limitation)
- You need browser-level or Real User Monitoring (use k6/browser or RUM tools, not this skill)
- You need fault injection / chaos (use toxiproxy, litmus, out of v1 scope)

---

## What You Receive

From the orchestrator or direct invocation:
- **Required**: `target` — base URL (`http://localhost:8080`) OR path to a controller file
- **Optional**: `change-name` — SDD change name to load scenarios from engram (`sdd/{change-name}/spec`)
- **Optional**: `endpoints` — explicit list of endpoints to cover (default: all detected)
- **Optional**: `scenarios` — subset of scenarios to generate (default: all four)
- **Optional**: `slos` — pre-configured thresholds (default: ask user or use conservative baseline)

---

## Stack Compatibility

Stack-agnostic at the HTTP layer. The backend can be anything — the skill only cares about:

| Requirement | Detection |
|-------------|-----------|
| HTTP/REST endpoints | curl `{target}` → expect 2xx/3xx/4xx (not connection refused) |
| JSON or form payloads | inferred from Content-Type on sample requests |
| Auth scheme | asked at Step 5 |
| CI platform | `.github/workflows/` OR `.gitlab-ci.yml` OR `Jenkinsfile` OR `.circleci/` |

If target is unreachable, ABORT with a diagnostic.

---

## Execution Flow

### Step 1: Preflight

```
Tooling:
├── k6 installed: run `k6 version`
│   └── If missing → report install commands per OS (macOS/Linux/Windows) and ABORT
├── Backend reachable: `curl -sf -o /dev/null -w "%{http_code}" {baseUrl}/` (or /health)
│   └── If connection refused → ABORT: "Backend at {baseUrl} is not responding. Start it first."
│   └── If >= 500 → WARN and continue (user may want to stress-test a broken backend to diagnose)
└── CI platform: list what was detected (github-actions / gitlab-ci / jenkins / circleci / none)
```

k6 install commands:
- macOS: `brew install k6`
- Linux (apt): `sudo gpg -k && sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69 && echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list && sudo apt-get update && sudo apt-get install k6`
- Windows: `choco install k6` or `scoop install k6`
- Docker fallback: `docker run --rm -i grafana/k6 run - <script.js`

### Step 2: Identify Target and Endpoints

```
If target is a URL (http://...):
├── Try OpenAPI discovery:
│   ├── GET {baseUrl}/v3/api-docs         (Springdoc)
│   ├── GET {baseUrl}/swagger.json        (various)
│   ├── GET {baseUrl}/openapi.json        (FastAPI, others)
│   └── GET {baseUrl}/_docs/openapi.yaml  (custom)
├── If found → parse paths/operations → endpoint list
├── If not found → ask user for explicit endpoint list
└── For each endpoint: method, path, auth?, request body template, expected response

If target is a controller file path:
├── Reuse parsing logic from integration-test skill (stacks/java-spring.md for Spring)
└── Produce endpoint list with same metadata

If change-name provided:
├── mem_search("sdd/{change-name}/spec") → mem_get_observation(id)
└── Extract scenarios for realistic test data (emails, amounts, names)
```

### Step 3: Gather SLOs

Ask the user for targets (offer defaults):

| SLO | Default | Meaning |
|-----|---------|---------|
| `p95 latency` | 500 ms | 95% of requests under X ms |
| `p99 latency` | 1000 ms | 99% of requests under X ms |
| `error rate` | 1% | 5xx + timeouts as % of total |
| `checks success` | 99% | In-script assertions passing |
| `min throughput` | inferred from VU plan | Minimum rps expected |

If the user declines to answer, use the defaults. Never silently loosen thresholds.

Store all SLOs in `tests/performance/thresholds.js` (single source of truth — imported by `script.js`).

### Step 4: Gather Auth Config

If any endpoint requires auth, ask:

```
Options:
├── Login endpoint (POST /auth/login with username + password → JWT)
├── Static JWT from env var K6_TOKEN
├── OAuth2 client credentials (token endpoint + client_id + client_secret)
└── Custom script (user provides login snippet)
```

Generate a `setup()` hook that fetches the token ONCE and shares it across all VUs via the `data` parameter.

### Step 5: Load Scenario Modules

Read all four files per the `scenarios/` directory:

- `scenarios/load.md` — constant-vus, expected traffic
- `scenarios/stress.md` — ramping-vus, find the knee
- `scenarios/spike.md` — ramping-arrival-rate, sudden burst
- `scenarios/soak.md` — constant-vus, long duration

Each module provides: executor config, stages, VU counts, duration, per-scenario thresholds, expected outcome.

### Step 6: Generate k6 Script

Produce under `tests/performance/`:

```
tests/performance/
├── script.js                 # Main: imports thresholds, scenarios, endpoints
├── thresholds.js             # SLOs centralized
├── auth.js                   # setup() → token
├── lib/
│   ├── endpoints.js          # One function per endpoint with checks
│   └── data-loader.js        # SharedArray + CSV loaders
├── data/
│   ├── users.csv             # Test users
│   └── payloads.json         # Sample bodies per endpoint
└── README.md                 # How to run: local + CI + each scenario individually
```

See `assets/k6-script-template.md` for the full skeleton.

### Step 7: Generate CI Integration

Based on detected CI (Step 1), produce ONE of:

- `.github/workflows/performance.yml` (GitHub Actions)
- `.gitlab-ci.yml` stage update (GitLab CI)
- `Jenkinsfile` snippet for user to paste (Jenkins)
- `.circleci/config.yml` snippet (CircleCI)

The workflow:
1. Triggers: `pull_request` (load only) + `workflow_dispatch` (all) + optional nightly schedule (soak)
2. Boot backend: `docker compose up -d` + wait-for-healthcheck (or equivalent)
3. Run k6: `k6 run tests/performance/script.js --env SCENARIO={scenario} --env BASE_URL={url}`
4. Upload JSON summary as artifact
5. Fail job if k6 exit != 0

See `assets/k6-ci-integration.md` for the ready-to-paste workflows.

### Step 8: Sanity Run (Optional)

If the user allows and the backend is local:

```bash
k6 run tests/performance/script.js --env BASE_URL={target} --env SCENARIO=load --duration=30s --vus=5
```

Short version of the load scenario just to confirm the script is valid. If it passes → thresholds are likely sane. If it fails → fix thresholds or warn the user that the backend already violates them under minimal load.

### Step 9: Persist Report

```
mem_save(
  title: "stress-tests/{target-slug}",
  topic_key: "stress-tests/{target-slug}",
  type: "architecture",
  project: "{project}",
  scope: "project",
  content: "{report markdown}"
)
```

`target-slug` = host + path or controller name in kebab-case. Example: `stress-tests/api-staging-example-com` or `stress-tests/user-controller`.

Report template:

```markdown
## Stress Test Suite — {target}

**Tool**: k6 {version}
**Generated at**: {ISO timestamp}
**CI platform**: {detected}

### Scenarios
| Scenario | Executor | Duration | Peak VUs / arrivals |
|----------|----------|----------|---------------------|
| load     | constant-vus | 10m | 50 VUs |
| stress   | ramping-vus  | 19m | up to 800 VUs |
| spike    | ramping-arrival-rate | 7m 20s | 1000 rps peak |
| soak     | constant-vus | 2h | 50 VUs |

### Endpoints
{table with method + path + weight in mixed load}

### SLOs
| Metric | Threshold | Scope |
|--------|-----------|-------|
| http_req_duration p95 | < 500ms | global |
| http_req_duration p99 | < 1000ms | global |
| http_req_failed rate | < 1% | global |
| checks rate | > 99% | global |

### Files Generated
- tests/performance/script.js
- tests/performance/thresholds.js
- tests/performance/auth.js
- tests/performance/lib/*
- tests/performance/data/*
- .github/workflows/performance.yml

### How to Run
- Local load: `k6 run tests/performance/script.js --env BASE_URL=http://localhost:8080 --env SCENARIO=load`
- Local stress: `... --env SCENARIO=stress`
- CI: triggered automatically on PR (load) + nightly (soak) + manual dispatch (all)
```

### Step 10: Return Envelope

Per `_shared/sdd-phase-common.md` Section D:

```markdown
## Status
{success | partial | blocked}

## Executive Summary
Generated k6 performance suite with {N} scenarios targeting {M} endpoints at {target}.
SLOs codified: p95 < {X}ms, error rate < {Y}%. CI gate integrated via {platform}.
Sanity run: {pass | fail | skipped}.

## Artifacts
- tests/performance/script.js
- tests/performance/thresholds.js
- tests/performance/auth.js
- tests/performance/lib/endpoints.js
- tests/performance/lib/data-loader.js
- tests/performance/data/*
- {CI workflow file}
- engram: stress-tests/{target-slug}

## Next Recommended
- Run the load scenario locally as a smoke test
- Enable the nightly schedule for soak tests
- Consider Grafana Cloud k6 / k6-operator for loads > 5k rps

## Risks
- Single-node k6 caps at ~5k rps; higher loads need distributed execution
- Soak test (2h default) is long — runs nightly, not per-PR
- Backend must be running in CI — the workflow assumes `docker compose up` works
- {any specific risk}

## Skill Resolution
{injected | fallback-registry | fallback-path | none}
```

---

## Critical Patterns

### k6 threshold behavior = automatic CI gate

If ANY threshold is violated, k6 exits with code **99**. CI treats non-zero as failure. There is no need to write a bash wrapper — this is native k6.

```js
export const options = {
  thresholds: {
    http_req_duration: ['p(95)<500'],     // violated → exit 99
    http_req_failed:   ['rate<0.01'],     // violated → exit 99
  },
};
```

Make every SLO a threshold. Make every threshold a concrete number from the spec or the conservative default.

### Mixed traffic, not hot-path

Real load is not a single endpoint hammered. Assign weights per endpoint based on expected traffic distribution (ask the user or infer). Use `scenarios` with different `exec` functions OR use a weighted switch inside `default`:

```js
export default function (data) {
  const r = Math.random();
  if (r < 0.6) listUsers(data);
  else if (r < 0.9) getUser(data);
  else createUser(data);
}
```

### Per-VU unique data

POST endpoints with unique constraints (email, username, order_id) need unique data per request or they crash with 409 and the test becomes invalid. Use `__VU` + `__ITER` + UUID:

```js
const unique = `u-${__VU}-${__ITER}-${crypto.randomUUID()}`;
```

See `assets/k6-data-patterns.md` for the full catalog.

### Ramp-up is mandatory

Starting at target VUs from second 0 causes a thundering herd that distorts all latency metrics. EVERY scenario must have a ramp-up phase (even soak — 30s to climb to full load).

### Checks + thresholds (both, not either)

- `check(res, {...})` → in-script assertions, tracked as the `checks` metric. Per-request validation.
- `thresholds: {...}` → aggregate SLOs. CI gate.

Every endpoint function MUST have `check()`. The `checks` metric threshold (>99%) becomes the gate against broken responses under load.

### Assertion quality (imported from strict-tdd.md)

Same rules as `~/.claude/skills/sdd-apply/strict-tdd.md:205-350`:
- Banned: `check(res, { 'ok': () => true })`, status-only checks, type-only.
- Required: assert concrete values (`res.json('id') > 0`, `res.json('status') === 'active'`).
- Mutation test: if the backend returns wrong data, would this check fail? If no → rewrite.

### Anti-patterns (do NOT generate)

- Running k6 on the same machine as the backend → resource contention invalidates metrics
- `http.get/post` without `check(...)` → no validation under load
- Single endpoint stressing → unrealistic
- Hard-coded URLs in the script → use `__ENV.BASE_URL`
- Test data that reuses the same row forever → unique-constraint failures accumulate
- Relaxing thresholds to make a failing run pass → the whole point of SLOs is that they are non-negotiable

---

## Rules

- ALWAYS run `k6 version` before generation — no shortcut, no assumption
- ALWAYS codify every SLO the user provides as a k6 threshold
- ALWAYS use `__ENV.BASE_URL` — never hardcode URLs in the generated script
- ALWAYS include `check()` on every request — the `checks` metric is the behavioral gate under load
- ALWAYS include ramp-up on every scenario — no instant step to full VUs
- ALWAYS produce a CI workflow for the detected platform — no skipping "we'll wire it later"
- ALWAYS use `tags: { scenario: '...' }` on each executor — per-scenario thresholds depend on it
- NEVER silence a failing sanity run by lowering thresholds — report the violation
- NEVER generate test data that reuses unique fields across VUs (emails, usernames)
- NEVER use `constant-arrival-rate` for a load scenario that is supposed to be VU-driven — understand the executor semantics
- NEVER commit secrets in the generated script or CI workflow — all auth goes through env vars (K6_TOKEN, K6_USER, K6_PASS)
- If k6 is missing, provide OS-specific install commands — do NOT attempt to install it yourself
- If backend is unreachable, ABORT — tests against a dead target are meaningless
- Apply strict-tdd.md assertion quality rules to every `check()` function
- Report sanity run result truthfully — if it fails, say so

---

## Extending the Skill

v1 covers HTTP REST APIs. Future additions (each is a new asset file):

- `assets/k6-websocket.md` — `k6/ws` for WebSocket load
- `assets/k6-grpc.md` — `k6/grpc` for gRPC
- `assets/k6-browser.md` — `k6/browser` for UI scenarios
- `assets/k6-chaos.md` — integration with toxiproxy for fault injection
- `assets/k6-distributed.md` — k6-operator on Kubernetes for >5k rps
- `assets/k6-baseline.md` — baseline comparison across runs (regression detection)

When adding a new asset, register it in this SKILL.md's Resources section.

---

## Resources

- **Scenarios**:
  - [scenarios/load.md](scenarios/load.md) — expected traffic, steady state
  - [scenarios/stress.md](scenarios/stress.md) — ramp to the breaking point
  - [scenarios/spike.md](scenarios/spike.md) — sudden surge + recovery
  - [scenarios/soak.md](scenarios/soak.md) — long duration, leak detection
- **Assets**:
  - [assets/k6-script-template.md](assets/k6-script-template.md) — full script skeleton
  - [assets/k6-thresholds-catalog.md](assets/k6-thresholds-catalog.md) — SLO → threshold mappings
  - [assets/k6-data-patterns.md](assets/k6-data-patterns.md) — SharedArray, CSV, per-VU uniqueness
  - [assets/k6-ci-integration.md](assets/k6-ci-integration.md) — CI workflows per platform
- **References**: [references/docs.md](references/docs.md) — links to integration-test, strict-tdd, engram-convention
