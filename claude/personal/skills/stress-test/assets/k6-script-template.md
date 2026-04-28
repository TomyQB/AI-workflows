# Asset — k6 Script Template

> Copy-paste skeletons for the generated k6 suite. Adjust paths and env vars to the target project.

---

## File: `tests/performance/script.js` (main)

```js
import { group, sleep } from 'k6';
import { thresholds } from './thresholds.js';
import { login } from './auth.js';
import { mixedTraffic, warmupTraffic } from './lib/endpoints.js';

// SCENARIO can be: load, stress, spike, soak, all
const SCENARIO = __ENV.SCENARIO || 'load';

const allScenarios = {
  warmup: {
    executor: 'constant-vus',
    vus: 10,
    duration: '1m',
    startTime: '0s',
    tags: { scenario: 'warmup' },
    exec: 'warmupFn',
    gracefulStop: '10s',
  },
  loadTest: {
    executor: 'constant-vus',
    vus: __ENV.LOAD_VUS ? Number(__ENV.LOAD_VUS) : 50,
    duration: __ENV.LOAD_DURATION || '10m',
    startTime: '1m',
    tags: { scenario: 'load' },
    exec: 'mixedFn',
    gracefulStop: '30s',
  },
  stressTest: {
    executor: 'ramping-vus',
    startVUs: 0,
    stages: [
      { duration: '2m', target: 100 },
      { duration: '5m', target: 200 },
      { duration: '5m', target: 400 },
      { duration: '5m', target: 800 },
      { duration: '2m', target: 0 },
    ],
    tags: { scenario: 'stress' },
    exec: 'mixedFn',
    gracefulStop: '30s',
  },
  spikeTest: {
    executor: 'ramping-arrival-rate',
    startRate: 10,
    timeUnit: '1s',
    preAllocatedVUs: 500,
    maxVUs: 1000,
    stages: [
      { duration: '30s', target: 10 },
      { duration: '10s', target: 1000 },
      { duration: '1m',  target: 1000 },
      { duration: '10s', target: 10 },
      { duration: '3m',  target: 10 },
    ],
    tags: { scenario: 'spike' },
    exec: 'mixedFn',
    gracefulStop: '30s',
  },
  soakTest: {
    executor: 'constant-vus',
    vus: __ENV.SOAK_VUS ? Number(__ENV.SOAK_VUS) : 50,
    duration: __ENV.SOAK_DURATION || '2h',
    startTime: '30s',
    tags: { scenario: 'soak' },
    exec: 'mixedFn',
    gracefulStop: '60s',
  },
};

function selectScenarios(which) {
  if (which === 'all') return allScenarios;
  const selected = { warmup: allScenarios.warmup };
  switch (which) {
    case 'load':   selected.loadTest   = allScenarios.loadTest;   break;
    case 'stress': selected.stressTest = allScenarios.stressTest; break;
    case 'spike':  selected.spikeTest  = allScenarios.spikeTest;  break;
    case 'soak':   selected.soakTest   = allScenarios.soakTest;   break;
    default: throw new Error(`Unknown SCENARIO: ${which}`);
  }
  return selected;
}

export const options = {
  scenarios: selectScenarios(SCENARIO),
  thresholds: thresholds,
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
  // Optional: push to Prometheus / JSON
  // output: 'experimental-prometheus-rw',
};

export function setup() {
  console.log(`Starting performance test: scenario=${SCENARIO}, base=${__ENV.BASE_URL}`);
  const token = login();
  return { token };
}

// Exec functions — k6 requires them at module top level
export function warmupFn(data) { warmupTraffic(data); }
export function mixedFn(data)  { mixedTraffic(data); }

export function teardown(data) {
  console.log('Performance test complete');
}

export function handleSummary(data) {
  return {
    'stdout': textSummary(data),
    'tests/performance/reports/summary.json': JSON.stringify(data, null, 2),
    'tests/performance/reports/summary.html': htmlReport(data),
  };
}

// Standard k6 summary (text) — can replace with a prettier formatter
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.2/index.js';
import { htmlReport } from 'https://raw.githubusercontent.com/benc-uk/k6-reporter/main/dist/bundle.js';
```

---

## File: `tests/performance/thresholds.js`

Single source of truth for SLOs.

```js
export const thresholds = {
  // Global SLOs
  http_req_duration: ['p(95)<500', 'p(99)<1000'],
  http_req_failed:   ['rate<0.01'],
  checks:            ['rate>0.99'],

  // Per-scenario SLOs (relaxed for stress/spike, strict for load/soak)
  'http_req_duration{scenario:load}':   ['p(95)<500',  'p(99)<1000'],
  'http_req_failed{scenario:load}':     ['rate<0.01'],

  'http_req_duration{scenario:stress}': ['p(95)<2000', 'p(99)<5000'],
  'http_req_failed{scenario:stress}':   ['rate<0.05'],

  'http_req_duration{scenario:spike}':  ['p(95)<3000', 'p(99)<10000'],
  'http_req_failed{scenario:spike}':    ['rate<0.10'],

  'http_req_duration{scenario:soak}':   ['p(95)<600',  'p(99)<1200'],
  'http_req_failed{scenario:soak}':     ['rate<0.01'],

  // Warmup is excluded from gates — we only collect data
  'http_req_duration{scenario:warmup}': ['p(95)<10000'],  // loose, for diagnostics only
};
```

---

## File: `tests/performance/auth.js`

Auth strategy resolved at generation time. Pick ONE pattern.

### Pattern A — Login endpoint

```js
import http from 'k6/http';
import { check, fail } from 'k6';

export function login() {
  const baseUrl = __ENV.BASE_URL;
  if (!baseUrl) fail('BASE_URL env var is required');

  const res = http.post(
    `${baseUrl}/auth/login`,
    JSON.stringify({
      username: __ENV.K6_USER || 'perftest',
      password: __ENV.K6_PASS || 'perftest',
    }),
    { headers: { 'Content-Type': 'application/json' } }
  );

  const ok = check(res, {
    'login returned 200': (r) => r.status === 200,
    'login returned token': (r) => r.json('accessToken') !== undefined,
  });

  if (!ok) fail(`Login failed: status=${res.status} body=${res.body}`);
  return res.json('accessToken');
}
```

### Pattern B — Static token from env

```js
import { fail } from 'k6';
export function login() {
  const token = __ENV.K6_TOKEN;
  if (!token) fail('K6_TOKEN env var is required');
  return token;
}
```

### Pattern C — OAuth2 client credentials

```js
import http from 'k6/http';
import { check, fail } from 'k6';
import encoding from 'k6/encoding';

export function login() {
  const clientId = __ENV.K6_CLIENT_ID;
  const clientSecret = __ENV.K6_CLIENT_SECRET;
  const tokenUrl = __ENV.K6_TOKEN_URL;
  if (!clientId || !clientSecret || !tokenUrl) fail('Missing OAuth2 env vars');

  const auth = encoding.b64encode(`${clientId}:${clientSecret}`);
  const res = http.post(tokenUrl,
    'grant_type=client_credentials',
    {
      headers: {
        Authorization: `Basic ${auth}`,
        'Content-Type': 'application/x-www-form-urlencoded',
      },
    });

  check(res, { 'token returned 200': (r) => r.status === 200 });
  return res.json('access_token');
}
```

### Pattern D — No auth

```js
export function login() { return null; }
```

---

## File: `tests/performance/lib/endpoints.js`

One function per endpoint, with `check()` assertions. All endpoints use the token from `setup()` via the `data` parameter.

```js
import http from 'k6/http';
import { check, group } from 'k6';
import { loadUsers, loadPayloads } from './data-loader.js';

const users    = loadUsers();          // SharedArray, read-only, per-VM
const payloads = loadPayloads();

function authHeaders(token) {
  return token ? { Authorization: `Bearer ${token}` } : {};
}

// ─── Warmup — light touch on main endpoints ──────────────────────
export function warmupTraffic(data) {
  const base = __ENV.BASE_URL;
  http.get(`${base}/health`, { tags: { endpoint: 'health' } });
  http.get(`${base}/users?page=0&size=5`, {
    headers: authHeaders(data.token),
    tags: { endpoint: 'list-users' },
  });
}

// ─── Mixed traffic — weighted endpoint selection ─────────────────
export function mixedTraffic(data) {
  const r = Math.random();
  if (r < 0.50)      listUsers(data);
  else if (r < 0.80) getUser(data);
  else if (r < 0.90) createUser(data);
  else if (r < 0.95) updateUser(data);
  else if (r < 0.97) deleteUser(data);
  else               healthCheck(data);
}

// ─── Per-endpoint implementations with checks ────────────────────
function listUsers(data) {
  const base = __ENV.BASE_URL;
  const res = http.get(`${base}/users?page=0&size=20`, {
    headers: authHeaders(data.token),
    tags: { endpoint: 'list-users' },
  });
  check(res, {
    'list-users status 200': (r) => r.status === 200,
    'list-users returns array': (r) => Array.isArray(r.json('content')),
    'list-users has page meta': (r) => r.json('totalElements') !== undefined,
  });
}

function getUser(data) {
  const base = __ENV.BASE_URL;
  const user = users[Math.floor(Math.random() * users.length)];
  const res = http.get(`${base}/users/${user.id}`, {
    headers: authHeaders(data.token),
    tags: { endpoint: 'get-user' },
  });
  check(res, {
    'get-user status 200': (r) => r.status === 200,
    'get-user returns id': (r) => r.json('id') === user.id,
    'get-user returns email': (r) => r.json('email') !== undefined,
  });
}

function createUser(data) {
  const base = __ENV.BASE_URL;
  // Unique data per request to avoid unique-constraint violations
  const uniqueEmail = `perf-${__VU}-${__ITER}-${Date.now()}@test.local`;
  const body = JSON.stringify({
    email: uniqueEmail,
    password: 'perfSecret123!',
    name: `Perf User ${__VU}-${__ITER}`,
  });
  const res = http.post(`${base}/users`, body, {
    headers: { ...authHeaders(data.token), 'Content-Type': 'application/json' },
    tags: { endpoint: 'create-user' },
  });
  check(res, {
    'create-user status 201': (r) => r.status === 201,
    'create-user returns id': (r) => r.json('id') > 0,
    'create-user Location header': (r) => r.headers['Location'] !== undefined,
  });
}

function updateUser(data) {
  const base = __ENV.BASE_URL;
  const user = users[Math.floor(Math.random() * users.length)];
  const body = JSON.stringify({ name: `Updated ${Date.now()}` });
  const res = http.patch(`${base}/users/${user.id}`, body, {
    headers: { ...authHeaders(data.token), 'Content-Type': 'application/json' },
    tags: { endpoint: 'update-user' },
  });
  check(res, {
    'update-user status 200': (r) => r.status === 200,
    'update-user reflects change': (r) => r.json('name').startsWith('Updated '),
  });
}

function deleteUser(data) {
  const base = __ENV.BASE_URL;
  // Use a safe-to-delete range, or seed a user first
  const userId = 99000 + __VU * 1000 + __ITER;
  const res = http.del(`${base}/users/${userId}`, null, {
    headers: authHeaders(data.token),
    tags: { endpoint: 'delete-user' },
  });
  check(res, {
    'delete-user status 204 or 404': (r) => r.status === 204 || r.status === 404,
  });
}

function healthCheck(data) {
  const res = http.get(`${__ENV.BASE_URL}/health`, { tags: { endpoint: 'health' } });
  check(res, {
    'health status 200': (r) => r.status === 200,
  });
}
```

---

## File: `tests/performance/lib/data-loader.js`

```js
import { SharedArray } from 'k6/data';
import papaparse from 'https://jslib.k6.io/papaparse/5.1.1/index.js';
import { open } from 'k6';

export function loadUsers() {
  return new SharedArray('users', function () {
    const csv = open('../data/users.csv');
    return papaparse.parse(csv, { header: true }).data;
  });
}

export function loadPayloads() {
  return new SharedArray('payloads', function () {
    return JSON.parse(open('../data/payloads.json'));
  });
}
```

### File: `tests/performance/data/users.csv`

```csv
id,email,name
1,alice@test.local,Alice
2,bob@test.local,Bob
3,carol@test.local,Carol
4,dave@test.local,Dave
5,eve@test.local,Eve
```

Seed more rows as needed — the load scenario benefits from hundreds of ids to avoid cache hits.

### File: `tests/performance/data/payloads.json`

```json
{
  "small": { "name": "test", "size": "S" },
  "medium": { "name": "test", "size": "M", "tags": ["a","b","c"] },
  "large": { "name": "test", "size": "L", "description": "...1KB of lorem..." }
}
```

---

## File: `tests/performance/README.md` (generated)

````markdown
# Performance Tests — k6

Generated by `integration-test-controller`'s sibling skill `stress-test`.

## Prerequisites

- [k6 installed](https://k6.io/docs/get-started/installation/)
- Backend running locally at `http://localhost:8080` (or adjust `BASE_URL`)
- Test user in DB: `perftest` / `perftest` (or override via env)

## Run locally

```bash
# Load (5 min smoke)
k6 run tests/performance/script.js --env BASE_URL=http://localhost:8080 --env SCENARIO=load --env LOAD_DURATION=5m

# Stress (19 min — find capacity)
k6 run tests/performance/script.js --env BASE_URL=http://localhost:8080 --env SCENARIO=stress

# Spike (~5 min)
k6 run tests/performance/script.js --env BASE_URL=http://localhost:8080 --env SCENARIO=spike

# Soak (short — 2 min for sanity)
k6 run tests/performance/script.js --env BASE_URL=http://localhost:8080 --env SCENARIO=soak --env SOAK_DURATION=2m

# Full soak (2h — nightly only)
k6 run tests/performance/script.js --env BASE_URL=http://staging.example.com --env SCENARIO=soak
```

## Exit codes

- `0` — all thresholds passed
- `99` — at least one threshold was violated (CI will fail the job)

## Environment variables

| Var | Default | Description |
|-----|---------|-------------|
| `BASE_URL` | — (required) | Backend URL |
| `SCENARIO` | `load` | `load / stress / spike / soak / all` |
| `K6_USER` | `perftest` | Login username |
| `K6_PASS` | `perftest` | Login password |
| `K6_TOKEN` | — | Static token (Pattern B only) |
| `LOAD_VUS` | `50` | VU count for load scenario |
| `LOAD_DURATION` | `10m` | Load scenario duration |
| `SOAK_VUS` | `50` | VU count for soak scenario |
| `SOAK_DURATION` | `2h` | Soak duration |

## SLOs

See `thresholds.js`. Central source of truth. Do NOT edit thresholds to make failing tests pass — fix the backend.

## Reports

After each run, `tests/performance/reports/summary.json` and `summary.html` are written. CI uploads these as artifacts.
````
