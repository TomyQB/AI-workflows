# Scenario — Spike Test

> Sudden, violent burst of traffic. Simulates Black Friday, viral moments, botnet attacks, retry storms.

---

## Goal

Answer **three questions**:

1. Does the system **survive** a sudden 10×–100× traffic jump?
2. How quickly does it **recover** to normal performance after the spike ends?
3. Does recovery leave **lingering damage** (leaked connections, poisoned caches, exhausted thread pools)?

Unlike stress (gradual climb), spike simulates *instant* surges. This exposes different bottlenecks:

| Problem class | Exposed by |
|---------------|-----------|
| Connection pool sizing | Spike (burst exhausts pool instantly) |
| Thread pool / queue sizing | Spike (queue overflows, 503s) |
| Cold cache / JIT | Spike (first 1000 requests hit cold path) |
| Auto-scaling lag | Spike (scaler takes 30-60s, spike is over in 1m) |
| Retry loops | Spike (failing requests trigger client retries → amplification) |

---

## Executor: `ramping-arrival-rate`

Unlike `ramping-vus` (which says "I want N concurrent users"), `ramping-arrival-rate` says "I want N new requests per second". This is what real spikes look like — **requests per second**, not concurrent users.

```js
spikeTest: {
  executor: 'ramping-arrival-rate',
  startRate: 10,                    // baseline rps
  timeUnit: '1s',
  preAllocatedVUs: 500,             // k6 pre-allocates VUs to sustain peak
  maxVUs: 1000,                     // hard cap
  stages: [
    { duration: '30s', target: 10  },    // baseline
    { duration: '10s', target: 1000 },   // SPIKE — 100× in 10s
    { duration: '1m',  target: 1000 },   // hold peak
    { duration: '10s', target: 10   },   // drop back
    { duration: '3m',  target: 10   },   // RECOVERY window — measure post-spike
  ],
  tags: { scenario: 'spike' },
  exec: 'mixedTraffic',
  gracefulStop: '30s',
}
```

Total duration: ~5 min.

### Parameters (adjust per target)

| Parameter | Typical | When to adjust |
|-----------|---------|----------------|
| `startRate` | 10 rps | Match baseline production traffic |
| Peak target | 10×–100× baseline | 100× for consumer-facing (viral), 10× for internal APIs |
| `preAllocatedVUs` | 500 | Must be high enough to sustain peak without k6 reporting "dropped iterations" |
| `maxVUs` | 1000 | Hard safety cap |

---

## Per-scenario thresholds

Spike allows the MOST tolerance during the burst itself, but is STRICT on recovery.

```js
// During spike: generous, system is supposed to strain
'http_req_duration{scenario:spike}': ['p(95)<3000', 'p(99)<10000'],
'http_req_failed{scenario:spike}':   ['rate<0.10'],        // up to 10% errors during peak

// Recovery window: strict, system must return to normal
'http_req_duration{scenario:spike,phase:recovery}': ['p(95)<800'],
'http_req_failed{scenario:spike,phase:recovery}':   ['rate<0.02'],
```

To tag the recovery window, set a secondary tag inside `default`:

```js
export function mixedTraffic() {
  const phase = (new Date() - __ITER_START_TIME) > 100_000 ? 'recovery' : 'spike';
  group(`${phase}`, () => { ... });
}
```

(Or simpler: use `k6 run --tag phase=...` and run the spike + recovery as separate scenario stages.)

---

## Recovery is the whole point

A spike test that only measures DURING the spike misses the critical signal: can the system come back?

Record these metrics per phase (spike vs recovery):

| Metric | Spike expectation | Recovery expectation |
|--------|-------------------|----------------------|
| Error rate | Can be high (5-10%) | MUST be normal (< 2%) |
| p95 latency | Can be degraded (2-3 sec) | MUST return to baseline (< 800ms) |
| Throughput | Likely capped | Should match baseline |
| Connection count | High | MUST drop back (leaks = bug) |
| Memory usage | Climbed | MUST release (GC pressure normal) |

---

## Expected outcome: verdicts

### ✅ Elastic (healthy)

- Some 5xx during peak (acceptable up to 10%)
- Throughput caps at backend capacity (not unlimited)
- Within 60s of spike end, all metrics normal
- No lingering issues: connection counts drop, memory releases, caches warm

### ⚠️ Slow recovery (warning)

- Recovery takes > 2 min
- Some endpoints remain slow after others recover
- Memory stays elevated (needs GC round)

Investigate: likely a warming issue or GC tuning problem.

### ❌ Broken by spike (FAIL)

- System does not recover within the test window
- Lingering 5xx errors after spike ends
- Connection leaks (counts never drop)
- Cascading failures (spike killed a dependency that stayed dead)

Do not ship. The system cannot handle sudden traffic changes.

---

## Why `ramping-arrival-rate`, not `ramping-vus`

VU-based executors have a subtle problem for spikes: if the backend slows down, each VU also slows down (because VUs wait for responses), so the effective rps naturally drops. This **hides** the spike problem.

Arrival-rate executors generate requests at the configured rate REGARDLESS of response time. If the backend slows down, requests queue up (or k6 drops them and reports "dropped iterations", which is the signal).

Always use arrival-rate executors for spike and soak tests against capacity-constrained systems.

---

## Endpoint mix

Real spikes are often concentrated on specific endpoints:

- Black Friday / sale → `POST /checkout` dominates
- Viral post → `GET /posts/{id}` and `GET /feed` dominate
- Login storm after outage → `POST /auth/login` dominates

Consider asking the user: "What endpoint would see the biggest spike in a real event?" and weight heavily on it (60-80% of traffic) with the rest as a tail.

---

## When spike is a CI gate

- NOT on every PR — ~5 min is acceptable but the signal is binary and often noisy
- Runs on `main` branch + pre-production deploy pipelines
- Optional: run before known high-traffic events (sale launch, marketing push)
- Useful as a weekly regression catch

Generated workflow invocation:

```bash
k6 run tests/performance/script.js \
  --env BASE_URL=http://staging.example.com \
  --env SCENARIO=spike
```

---

## Common mistakes

- **Using `ramping-vus`** → hides the problem (see above)
- **`preAllocatedVUs` too low** → k6 reports dropped iterations, test results are invalid
- **No recovery window** → miss the post-spike damage
- **Testing against a warm system** → production spikes often hit cold caches; simulate by restarting the backend before each spike test
- **Forgetting downstream dependencies** → if the backend calls a payment gateway, the gateway might be the bottleneck, not your code; use WireMock / mock services for isolated tests

---

## Report fields to populate

- Baseline rps
- Peak rps achieved
- Peak duration
- Error rate during peak
- Error rate during recovery
- Recovery time to baseline latency
- Dropped iterations (if any — indicates preAllocatedVUs was too low)
- Verdict: elastic / slow-recovery / broken
