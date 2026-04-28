# Asset — k6 Thresholds Catalog

> Reference for SLO → k6 threshold syntax. Copy the relevant block into `tests/performance/thresholds.js`.

---

## Threshold syntax primer

```js
thresholds: {
  '<metric>{<tag>:<value>}': ['<expression>', '<expression>'],
}
```

- **metric**: built-in (`http_req_duration`, `http_req_failed`, `checks`, `http_reqs`, `iteration_duration`, `vus`) or custom (`my_metric`)
- **tag filter** (optional): narrows to tagged requests; e.g., `{scenario:load}` or `{endpoint:create-user}`
- **expression**: `<p(95)<500>`, `<rate<0.01>`, `<count<100>`, `<value>=500>`, etc.
- **operators**: `<`, `<=`, `>`, `>=`, `==`, `!=`
- **percentiles**: `p(50)`, `p(90)`, `p(95)`, `p(99)`, `p(99.9)`
- **aggregators**: `avg`, `min`, `max`, `med`, `count`, `rate`

If ANY threshold is violated, k6 exits with code **99** and CI fails.

---

## Presets by criticality

Apply ONE of these presets depending on how critical the endpoint is.

### Mission-critical (payments, auth, anything user-facing on the hot path)

```js
{
  http_req_duration: ['p(95)<300', 'p(99)<800'],
  http_req_failed:   ['rate<0.005'],     // 0.5%
  checks:            ['rate>0.995'],
}
```

### Standard API (most CRUD endpoints)

```js
{
  http_req_duration: ['p(95)<500', 'p(99)<1000'],
  http_req_failed:   ['rate<0.01'],      // 1%
  checks:            ['rate>0.99'],
}
```

### Internal / admin / report generation

```js
{
  http_req_duration: ['p(95)<2000', 'p(99)<5000'],
  http_req_failed:   ['rate<0.02'],      // 2%
  checks:            ['rate>0.98'],
}
```

### Batch / bulk operations

```js
{
  http_req_duration: ['p(95)<10000'],    // 10s acceptable for bulk
  http_req_failed:   ['rate<0.05'],
  checks:            ['rate>0.95'],
}
```

---

## Per-scenario mapping

Same SLOs apply globally, but each scenario relaxes them based on purpose:

| Scenario | Goal | p95 multiplier | error rate tolerance |
|----------|------|----------------|----------------------|
| load     | Validate baseline | × 1.0 | 1% |
| stress   | Find capacity | × 4.0 | 5% |
| spike    | Survive surge | × 6.0 | 10% (during peak), 2% (recovery) |
| soak     | No drift over time | × 1.2 | 1% |
| warmup   | No gate (data collection) | no limit | no limit |

Template for all four scenarios:

```js
// Assume global SLO: p95 < 500ms, error rate < 1%
{
  // Load — the reference
  'http_req_duration{scenario:load}': ['p(95)<500'],
  'http_req_failed{scenario:load}':   ['rate<0.01'],

  // Stress — relaxed
  'http_req_duration{scenario:stress}': ['p(95)<2000'],
  'http_req_failed{scenario:stress}':   ['rate<0.05'],

  // Spike — very relaxed during peak
  'http_req_duration{scenario:spike}': ['p(95)<3000'],
  'http_req_failed{scenario:spike}':   ['rate<0.10'],

  // Soak — near-load, slight tolerance for GC pressure
  'http_req_duration{scenario:soak}': ['p(95)<600'],
  'http_req_failed{scenario:soak}':   ['rate<0.01'],
}
```

---

## Per-endpoint thresholds

Sometimes different endpoints have different SLOs. Use the `{endpoint:name}` tag.

```js
{
  // GET endpoints should be fast
  'http_req_duration{endpoint:list-users}': ['p(95)<300'],
  'http_req_duration{endpoint:get-user}':   ['p(95)<200'],

  // POST is slower (validation + write)
  'http_req_duration{endpoint:create-user}': ['p(95)<800'],

  // Complex aggregation
  'http_req_duration{endpoint:report-monthly}': ['p(95)<5000'],
}
```

Requires tagging in the endpoint function:

```js
http.get(`${base}/users`, { tags: { endpoint: 'list-users' } });
```

---

## Common metrics

### Built-in

| Metric | Type | Description |
|--------|------|-------------|
| `http_req_duration` | Trend | Full request time (incl. connect + TLS + wait) |
| `http_req_waiting` | Trend | TTFB only — network + backend processing |
| `http_req_connecting` | Trend | TCP handshake time |
| `http_req_tls_handshaking` | Trend | TLS handshake time |
| `http_req_sending` | Trend | Time sending request body |
| `http_req_receiving` | Trend | Time reading response body |
| `http_req_failed` | Rate | % of requests that failed (status >= 400 OR network error) |
| `http_reqs` | Counter | Total request count |
| `iteration_duration` | Trend | Full iteration time (one `default()` call) |
| `iterations` | Counter | Total iterations |
| `vus` | Gauge | Current VU count |
| `vus_max` | Gauge | Max VUs allowed |
| `data_sent` | Counter | Bytes sent |
| `data_received` | Counter | Bytes received |
| `checks` | Rate | % of `check()` calls that returned true |

### Custom metrics

Define in-script for business-specific signals:

```js
import { Rate, Trend, Counter } from 'k6/metrics';

const businessErrors = new Rate('business_errors');
const loginLatency   = new Trend('login_latency', true);  // `true` = include in Trend stats
const failedPayments = new Counter('failed_payments');

export default function () {
  const res = http.post(...);
  businessErrors.add(res.json('success') !== true);
  loginLatency.add(res.timings.duration);
  if (res.json('paymentStatus') === 'failed') failedPayments.add(1);
}

// In thresholds:
{
  business_errors: ['rate<0.001'],
  login_latency:   ['p(95)<400'],
  failed_payments: ['count<10'],
}
```

---

## Advanced: abortOnFail

By default, threshold violations are reported at the END and exit 99. For expensive runs (soak, stress), you can abort EARLY when a critical threshold is violated:

```js
{
  http_req_duration: [
    { threshold: 'p(99)<1000', abortOnFail: true, delayAbortEval: '30s' },
  ],
}
```

- `abortOnFail: true` — stop the test immediately if violated
- `delayAbortEval: '30s'` — don't evaluate for the first 30s (gives warm-up window)

Use sparingly — aborting loses data you might want for debugging. Prefer abortOnFail for soak (stops a 2h test early if GC death spiral begins).

---

## Sanity defaults

When the user has no SLOs to provide, use these defaults. Document them in the report so the user knows to tune:

```js
export const thresholds = {
  // Global
  http_req_duration: ['p(95)<500', 'p(99)<1000'],
  http_req_failed:   ['rate<0.01'],
  checks:            ['rate>0.99'],

  // Per-scenario
  'http_req_duration{scenario:load}':   ['p(95)<500'],
  'http_req_duration{scenario:stress}': ['p(95)<2000'],
  'http_req_duration{scenario:spike}':  ['p(95)<3000'],
  'http_req_duration{scenario:soak}':   ['p(95)<600'],

  'http_req_failed{scenario:load}':   ['rate<0.01'],
  'http_req_failed{scenario:stress}': ['rate<0.05'],
  'http_req_failed{scenario:spike}':  ['rate<0.10'],
  'http_req_failed{scenario:soak}':   ['rate<0.01'],

  // Warmup — no gate, just collect data
  'http_req_duration{scenario:warmup}': ['p(95)<10000'],
};
```

In the final report: *"SLOs generated from conservative defaults. Review and tune `tests/performance/thresholds.js` with your actual production targets."*
