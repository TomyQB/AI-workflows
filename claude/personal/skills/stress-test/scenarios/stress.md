# Scenario — Stress Test

> Ramp traffic upward until the system breaks. Find the capacity limit.

---

## Goal

Answer **two questions**:

1. **What is the maximum load this backend can handle before SLOs are violated?** (the "knee" of the performance curve)
2. **How does it degrade when pushed past that limit?** Gracefully (slower responses, queueing) or catastrophically (crashes, cascading failures, data corruption)?

The stress test is how you discover headroom. If you are running at 500 rps in production and stress reveals the backend breaks at 600 rps → you have 20% headroom. That is scary.

---

## Executor: `ramping-vus`

Climb VU count in stages. Each stage holds long enough to collect stable metrics, then climbs again.

```js
stressTest: {
  executor: 'ramping-vus',
  startVUs: 0,
  stages: [
    { duration: '2m', target: 100 },    // warm-up + baseline
    { duration: '5m', target: 200 },    // 2x baseline
    { duration: '5m', target: 400 },    // 4x
    { duration: '5m', target: 800 },    // 8x — likely past the knee
    { duration: '2m', target: 0  },     // ramp-down
  ],
  tags: { scenario: 'stress' },
  exec: 'mixedTraffic',
  gracefulStop: '30s',
}
```

Total duration: 19 min. Peak: 8x expected load.

### How to choose peak VUs

If you do not know the backend's capacity, start with **4× the load scenario** and adjust:

- If all thresholds pass at 4× → double to 8× next run
- If thresholds fail at 2× → the system is under-provisioned for the declared SLOs → fix before shipping
- If the system cannot even handle the ramp (fails during stage 1) → the load scenario is lying; rerun load with more VUs

---

## Per-scenario thresholds

Stress is allowed some tolerance — it is SUPPOSED to push the system. But the system must **degrade gracefully**, not collapse.

```js
'http_req_duration{scenario:stress}': ['p(95)<2000', 'p(99)<5000'],
'http_req_failed{scenario:stress}':   ['rate<0.05'],        // allow up to 5% errors at peak
'checks{scenario:stress}':            ['rate>0.95'],
```

**Stricter post-recovery check** — after the ramp-down, the system must return to load-scenario-level SLOs within 30 seconds. Measured via a `stressTest:recovery` subtag (optional) or comparing `http_req_duration` post-ramp vs load baseline.

---

## Expected outcome: three possible verdicts

### ✅ Graceful degradation (healthy)

- Latency climbs steadily as VUs grow
- Error rate grows slowly (connection timeouts, not 500s)
- After ramp-down, system returns to baseline quickly
- No data loss, no cascading failures

This is the goal. You have found the capacity limit, and crossing it hurts but does not kill.

### ⚠️ Brittle degradation (warning)

- Latency spikes non-linearly at some stage (knee is sharp)
- Error rate jumps (e.g., 1% → 30% in one stage)
- Recovery is slow (> 1 min to return to baseline)

Ship allowed, but document capacity clearly. Consider auto-scaling triggers BEFORE the knee.

### ❌ Catastrophic degradation (FAIL)

- Full outage (connection refused) at some VU count
- Data corruption (truncated writes, partial transactions)
- Cascading failure (one service dies and takes others with it)
- No recovery without manual intervention

Do not ship. Fix before proceeding.

---

## How stress ties into capacity planning

After a successful stress run, the generated report includes:

- **Capacity (VUs)**: the maximum stage that passed all thresholds
- **Throughput at capacity (rps)**: average throughput during the last-passing stage
- **Headroom vs load scenario**: `capacity_rps / load_rps` — if < 2, you are under-provisioned

This is the number to compare against auto-scaling rules and capacity plans.

---

## Ramp-down matters

Do NOT skip the `target: 0` final stage. Benefits:
- Validates graceful shutdown of in-flight requests
- Detects thread/connection leaks (test ends with open connections = bug)
- Provides a recovery signal for the verdict above

---

## Endpoint mix

Same mix as load scenario — real traffic is mixed. Single-endpoint stress is a microbenchmark.

Weights MAY differ from load: stress often emphasizes expensive endpoints (writes, aggregations) to find bottlenecks faster. Ask the user if they want to adjust weights for the stress scenario, otherwise reuse load weights.

---

## When stress is a CI gate

- NOT on every PR — too slow (19 min) and flaky for gating merges
- Runs on `main` branch merges + nightly schedule
- Blocks deploy to staging/prod if thresholds fail
- Alert the team via Slack/email on failure (CI secondary action)

Generated workflow invocation:

```bash
k6 run tests/performance/script.js \
  --env BASE_URL=http://staging.example.com \
  --env SCENARIO=stress
```

---

## Common mistakes

- **Ramp too fast** → every stage is 10s → metrics never stabilize → noise, false knees
- **No ramp-down** → can't tell graceful vs catastrophic
- **Same machine as backend** → local resource contention distorts the knee
- **Ignoring recovery** → the knee is only useful if the system RECOVERS; a one-way trip to dead is not "stress"
- **Running stress on a fresh DB** → cold caches + no data = optimistic results; seed representative data

---

## Report fields to populate

- Stages executed + target VUs per stage
- Stage at which thresholds first failed (or "all passed" if applicable)
- Peak throughput before failure
- Recovery time after ramp-down
- Verdict: graceful / brittle / catastrophic
- Headroom ratio vs load scenario
