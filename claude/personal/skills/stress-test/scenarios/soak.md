# Scenario — Soak (Endurance) Test

> Sustained moderate load over HOURS. Catches problems that only appear with time.

---

## Goal

Answer **four questions that short tests cannot**:

1. Is there a **memory leak**? (RSS climbing steadily over hours → out-of-memory eventually)
2. Are **connections / threads / file descriptors** being leaked? (counts grow without releasing)
3. Does **latency grow over time** due to GC pressure, index fragmentation, log file growth?
4. Do **background jobs interfere** with user-facing traffic when both run continuously?

Short tests pass because they end before the leak matters. Soak is the test that catches what 10 minutes cannot.

---

## Executor: `constant-vus`

Hold a steady, moderate load for a long time. This is NOT about peak capacity — it is about **duration**.

```js
soakTest: {
  executor: 'constant-vus',
  vus: 50,                      // moderate — about load scenario level
  duration: '2h',               // the key parameter
  startTime: '30s',             // small ramp window before measurement
  tags: { scenario: 'soak' },
  exec: 'mixedTraffic',
  gracefulStop: '60s',
}
```

Total duration: 2h + ramp + graceful stop ≈ 2h 2 min.

### Parameters

| Parameter | Typical | When to adjust |
|-----------|---------|----------------|
| `vus` | 50 (same as load) | Do NOT use peak — soak is about endurance, not capacity |
| `duration` | 2h | Min for detecting leaks. 4-8h for paranoid pre-prod. 24h for "it must survive" systems |
| `startTime` | 30s | Small ramp to avoid thundering herd |
| `gracefulStop` | 60s | Graceful shutdown matters to detect leaks at teardown |

---

## Per-scenario thresholds

Soak is the STRICT test for drift. Even small degradation over time is a red flag.

```js
'http_req_duration{scenario:soak}': ['p(95)<600'],          // slight tolerance vs load p95<500
'http_req_failed{scenario:soak}':   ['rate<0.01'],          // same strict error rate
'checks{scenario:soak}':            ['rate>0.99'],

// Drift check (optional advanced — compare early vs late)
// Latency in last 30 min should not exceed latency in first 30 min by more than 10%
```

The p95 drift is the KEY signal for leaks. If first-hour p95 is 300ms and last-hour p95 is 1200ms, you have drift even if the global p95 is under 600ms.

To detect drift, post-process the k6 JSON output:

```bash
k6 run --out json=soak.json tests/performance/script.js --env SCENARIO=soak
# Then compare time-bucketed p95: first_hour_p95 vs last_hour_p95
```

Include a Node/Python/Bash post-process script in the generated suite to fail if drift > 10%.

---

## What to monitor DURING the soak (external to k6)

k6 only knows about HTTP metrics. Leaks show up in BACKEND metrics. The generated workflow should capture these in parallel:

| Metric | How to capture | Healthy trend | Leak signal |
|--------|---------------|---------------|-------------|
| Heap memory (JVM) | `jcmd <pid> GC.heap_info` every 5 min, or Actuator `/actuator/metrics/jvm.memory.used` | Stable (oscillates with GC) | Monotonic growth |
| Heap memory (Node) | `process.memoryUsage()` endpoint | Stable | Growth |
| Heap memory (Python) | `tracemalloc` / `resource.getrusage` | Stable | Growth |
| Open connections | `ss -tan | wc -l` on the backend host | Stable at pool size | Growth past pool size |
| File descriptors | `lsof -p <pid> | wc -l` | Stable | Growth |
| Thread count | `jstack <pid> | grep -c java.lang.Thread.State` | Stable | Growth |
| DB connection pool | `pg_stat_activity` count | Stable at pool max | Stuck at max, app timing out |
| GC time % | Actuator `/actuator/metrics/jvm.gc.pause` | < 5% of wall time | Growing toward 100% (GC death spiral) |

Generated companion script `scripts/monitor-soak.sh` samples these and writes CSV → post-process for leak detection.

---

## Expected outcome: verdicts

### ✅ Stable (healthy)

- Latency curve is flat (±10%) across the full 2h
- Error rate is flat
- Heap memory oscillates between GC cycles, no upward trend
- Connection / thread counts stable
- Post-soak: system shuts down cleanly, no dangling resources

### ⚠️ Slow drift (warning)

- Latency grows 20-50% over 2h
- One metric (heap, connections, FDs) grows steadily but stays within limits
- Restart resolves it

Investigate before shipping. Likely a leak that just hasn't hit the limit yet.

### ❌ Degrading / dying (FAIL)

- Latency doubles/triples over 2h
- OOM kill at some point
- Connection pool exhausted mid-run
- GC death spiral (>50% GC time)
- Post-soak: backend must be restarted to recover

Do not ship. There is a leak. Find and fix it before prod.

---

## Endpoint mix

Same mix as load. Soak is essentially "load for much longer" in terms of traffic shape.

CONSIDER including:
- **Periodic admin operations**: if the backend has periodic jobs (cron, scheduled flushes), let them run during the soak. Sometimes the leak is in the background job, not the hot path.
- **DB maintenance**: if the project has auto-vacuum, TTL cleanup, or index rebuilds, ensure they are running too.

---

## When soak is a CI gate

- **NEVER on PRs** — too slow (2h)
- Runs on **nightly schedule** against staging
- Blocks the next morning's release if it fails
- Alerts the team via Slack on failure

Generated workflow invocation:

```yaml
# .github/workflows/performance-soak.yml
on:
  schedule:
    - cron: '0 2 * * *'    # 2 AM nightly
  workflow_dispatch:
```

```bash
k6 run tests/performance/script.js \
  --env BASE_URL=http://staging.example.com \
  --env SCENARIO=soak
```

---

## Short-version soak for local validation

For local sanity testing of the generated script, include a `SOAK_DURATION` override:

```bash
k6 run tests/performance/script.js --env SCENARIO=soak --env SOAK_DURATION=2m
```

Inside `scenarios`:
```js
soakTest: {
  executor: 'constant-vus',
  vus: 50,
  duration: __ENV.SOAK_DURATION || '2h',
  ...
}
```

2 minutes is enough to validate the script compiles, hits endpoints, and collects metrics. NOT enough to detect leaks — only for structural validation.

---

## Common mistakes

- **Running soak at peak VUs** → this is a long stress test, not a soak; you want MODERATE sustained, not stressed sustained
- **Only measuring HTTP metrics** → leaks are in heap/FDs/connections; must capture backend metrics in parallel
- **Restarting the backend mid-run** → defeats the purpose; soak must run against a single continuous process
- **Tiny duration** → 30-min soaks miss most leaks; 2h is the minimum credible duration
- **Missing post-run analysis** → soak produces a mountain of data; must have a post-process script that says "drift detected: YES/NO"

---

## Report fields to populate

- Duration
- Total requests
- p95 by hour (to detect drift)
- Memory trend (flat / growing / oscillating)
- Connection trend
- FD trend
- GC time % trend
- Verdict: stable / slow-drift / degrading
- Drift values if measured (e.g., "p95 grew from 280ms in hour 1 to 310ms in hour 2: acceptable")
