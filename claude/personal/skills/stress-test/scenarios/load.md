# Scenario — Load Test

> Expected traffic, steady state, extended duration. The baseline "this should work under normal conditions" test.

---

## Goal

Verify the system meets SLOs under **the load you expect to see in production**.

- Not the peak, not a burst — the steady 9am-to-5pm baseline.
- Failure means: the system cannot handle normal traffic → do not ship.

---

## Executor: `constant-vus`

Hold a fixed number of VUs for a fixed duration. No ramp-up except a small warm-up window.

```js
loadTest: {
  executor: 'constant-vus',
  vus: 50,
  duration: '10m',
  startTime: '0s',
  tags: { scenario: 'load' },
  exec: 'mixedTraffic',          // function from lib/endpoints.js
  gracefulStop: '30s',
}
```

### Parameters (adjust per target)

| Parameter | Typical | When to adjust |
|-----------|---------|----------------|
| `vus` | 50 | Up if the backend expects hundreds of concurrent users; down for internal APIs |
| `duration` | 10m | Down to 5m for PR gates; up to 30m for weekly regression |
| `gracefulStop` | 30s | Allow in-flight requests to finish before tearing down |

---

## Per-scenario thresholds

Load is the MOST STRICT scenario. All SLOs must pass — no tolerance.

```js
'http_req_duration{scenario:load}': ['p(95)<500', 'p(99)<1000'],
'http_req_failed{scenario:load}':   ['rate<0.01'],
'checks{scenario:load}':            ['rate>0.99'],
```

If load fails, do not bother running stress/spike/soak — the system is already broken.

---

## Expected outcome

- All thresholds pass
- Latency is stable (no growth over time)
- No 5xx in logs
- Throughput roughly equals `VUs * 1000 / average_latency_ms`

---

## Warm-up

Include a brief warm-up before collecting metrics so JIT compilation, connection pools, and caches are primed. Two options:

### A. Warm-up scenario (preferred)

```js
warmup: {
  executor: 'constant-vus',
  vus: 10,
  duration: '1m',
  startTime: '0s',
  tags: { scenario: 'warmup' },
  exec: 'warmupTraffic',         // light touch on main endpoints
},
loadTest: {
  executor: 'constant-vus',
  vus: 50,
  duration: '10m',
  startTime: '1m',               // starts AFTER warmup
  tags: { scenario: 'load' },
  exec: 'mixedTraffic',
}
```

Then exclude `warmup` from thresholds via the scenario tag filter — thresholds on `{scenario:load}` only.

### B. Short ramp inside the scenario

Switch executor to `ramping-vus` with a 30s ramp to the target VUs. Simpler but mixes warm-up into the measured window.

Prefer Option A.

---

## Endpoint mix

Load scenarios MUST hit multiple endpoints in realistic proportions. A single-endpoint load test is not a load test — it is a microbenchmark.

Example weights for a typical CRUD API:

| Endpoint | Weight | Rationale |
|----------|--------|-----------|
| GET /resources      | 50% | List views dominate real traffic |
| GET /resources/{id} | 30% | Detail views |
| POST /resources     | 10% | Writes are rarer |
| PUT /resources/{id} | 5%  | Updates |
| DELETE /resources/{id} | 2% | Deletes are rarest |
| Other (auth, health) | 3% | Misc |

Ask the user for weights if they have production analytics. Otherwise use the defaults above and document the assumption in the report.

Implement via `lib/endpoints.js`:

```js
export function mixedTraffic(data) {
  const r = Math.random();
  if (r < 0.50) return listResources(data);
  if (r < 0.80) return getResource(data);
  if (r < 0.90) return createResource(data);
  if (r < 0.95) return updateResource(data);
  if (r < 0.97) return deleteResource(data);
  return healthCheck(data);
}
```

---

## When load test is a CI gate

- Runs on every `pull_request` in CI
- Short duration (3-5 min) for PR speed
- Full duration (10-30 min) for `main` branch or nightly
- Blocks merge if thresholds fail

Generated workflow invocation:

```bash
k6 run tests/performance/script.js \
  --env BASE_URL=http://localhost:8080 \
  --env SCENARIO=load \
  --duration=5m
```

---

## Common mistakes

- **No warm-up** → JIT + cold caches inflate p95, failing threshold on a system that is actually fine
- **Single endpoint** → misses coupling issues (e.g., DB pool exhaustion when reads + writes compete)
- **Too short** → 30-second runs have so much variance that thresholds pass/fail randomly
- **Unrealistic data** → GET on the same id 50k times hits the DB cache and looks faster than reality; vary ids from a SharedArray

---

## Report fields to populate

For the final report (Step 9 of SKILL.md):

- VUs used
- Actual duration
- Total requests
- Throughput (rps)
- p95, p99, max latency
- Error rate
- Threshold pass/fail per metric
