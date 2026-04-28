# Asset — k6 Test Data Patterns

> How to feed realistic, unique, distributed data to k6 tests without distorting metrics.

---

## Why data matters under load

Bad test data kills test validity:

- **Same id every request** → hits DB cache → latency looks artificially low
- **Same email on POST** → unique constraint violations → fake error rate, not real
- **Empty DB** → cold queries + no index pressure → optimistic numbers
- **No data variation** → misses serialization/logic branches that matter under load

Good test data mirrors production: varied ids, varied payload sizes, unique inserts, realistic distributions.

---

## Pattern 1 — SharedArray (read-only, shared across VUs)

Use for a pool of valid ids/values that any VU can read. Loaded ONCE per VM, zero memory overhead per VU.

```js
// lib/data-loader.js
import { SharedArray } from 'k6/data';
import papaparse from 'https://jslib.k6.io/papaparse/5.1.1/index.js';
import { open } from 'k6';

export const users = new SharedArray('users', function () {
  const csv = open('../data/users.csv');
  return papaparse.parse(csv, { header: true }).data;
});

// In an endpoint:
const user = users[Math.floor(Math.random() * users.length)];
http.get(`${base}/users/${user.id}`);
```

### When to use

- Random sampling from a large pool of valid ids
- Referencing existing users, products, orders etc. that you know are in the DB

### When NOT to use

- Unique data per request (use Pattern 3 instead)
- Per-VU stateful progression (use Pattern 4)

---

## Pattern 2 — Inline fixtures (small, static)

For fixed payloads that multiple tests reuse.

```js
// lib/fixtures.js
export const payloads = {
  small:  { name: 'test', size: 'S' },
  medium: { name: 'test', size: 'M', tags: ['a','b'] },
  large:  { name: 'test', size: 'L', description: 'x'.repeat(1024) },
};

// Usage:
const body = JSON.stringify(payloads.medium);
http.post(`${base}/items`, body, { headers: {...} });
```

Keep fixtures small. Anything > 100 rows → CSV via SharedArray.

---

## Pattern 3 — Per-request unique data (for inserts)

POST/PUT endpoints that write to DB with unique constraints (email, username, order_id) need NEW values per request, or k6 will log thousands of 409s.

Use `__VU` (VU id) + `__ITER` (iteration number) + a high-resolution source:

```js
function uniqueEmail() {
  return `perf-${__VU}-${__ITER}-${Date.now()}@test.local`;
}

function uniqueUsername() {
  return `user_${__VU}_${__ITER}_${Math.random().toString(36).slice(2, 8)}`;
}

function uniqueOrderId() {
  // UUID v4 via k6/crypto or simple random
  return `ord-${__VU}-${__ITER}-${Math.random().toString(36).slice(2, 12)}`;
}
```

Why these work:
- `__VU` is unique across all running VUs
- `__ITER` is unique per VU per iteration
- Combined, `(__VU, __ITER)` is unique across the entire run
- Adding timestamp or random suffix handles overlap with previous runs (DB not cleaned)

---

## Pattern 4 — Per-VU state (stateful flows)

For multi-step scenarios where each VU simulates a distinct user session.

```js
import exec from 'k6/execution';

export default function () {
  const vu = exec.vu.idInTest;        // stable per VU across iterations
  const userId = 1000 + vu;           // each VU uses a dedicated user

  // Login as that user (stateful session)
  const token = loginAs(userId);

  // Execute a multi-step flow
  viewProfile(token, userId);
  updateProfile(token, userId);
  viewDashboard(token, userId);
}
```

Use when the backend has per-user rate limits, per-user caches, or session affinity.

---

## Pattern 5 — Weighted random selection

Real traffic has uneven endpoint distribution. Don't hit all endpoints equally.

```js
const endpointWeights = [
  { fn: listUsers,   weight: 0.50 },
  { fn: getUser,     weight: 0.30 },
  { fn: createUser,  weight: 0.10 },
  { fn: updateUser,  weight: 0.05 },
  { fn: deleteUser,  weight: 0.02 },
  { fn: healthCheck, weight: 0.03 },
];

function pickWeighted() {
  const r = Math.random();
  let cumulative = 0;
  for (const { fn, weight } of endpointWeights) {
    cumulative += weight;
    if (r < cumulative) return fn;
  }
  return endpointWeights[endpointWeights.length - 1].fn;
}

export default function (data) {
  pickWeighted()(data);
}
```

Ask the user for production endpoint distribution (from APM, access logs, Prometheus). If unavailable, use reasonable defaults:

| API type | Typical read:write | Notes |
|----------|---------------------|-------|
| Social (feeds, posts) | 90:10 | Heavy read skew |
| E-commerce | 70:30 | Browsing > buying, but significant writes |
| Internal CRUD (admin) | 50:50 | More balanced |
| Analytics / reports | 99:1 | Almost pure reads |

---

## Pattern 6 — CSV for user accounts

`tests/performance/data/users.csv`:

```csv
id,email,password,name
1,alice@test.local,secret1,Alice
2,bob@test.local,secret2,Bob
3,carol@test.local,secret3,Carol
...
100,user100@test.local,secret100,User 100
```

Seeding script for the backend (run ONCE before tests):

```sql
-- Generate 100 test users (adjust for your schema)
INSERT INTO users (email, password_hash, name)
SELECT
  'perfuser-' || i || '@test.local',
  '$2a$10$somePrecomputedHash',          -- BCrypt of 'secret' + i
  'PerfUser ' || i
FROM generate_series(1, 100) AS s(i);
```

Keep the seed script in `tests/performance/scripts/seed.sql` or equivalent so CI can re-seed on a fresh DB.

---

## Pattern 7 — Realistic payload sizes

Small payloads (< 1KB) don't exercise serialization, gzip, or network buffers. Mix sizes:

```js
const sizeDistribution = [
  { name: 'small',  size: 500,   weight: 0.70 },  // < 1KB
  { name: 'medium', size: 5000,  weight: 0.25 },  // ~5KB
  { name: 'large',  size: 50000, weight: 0.05 },  // ~50KB
];

function generatePayload() {
  const choice = pickFromWeighted(sizeDistribution);
  return {
    name: 'test',
    data: 'x'.repeat(choice.size),
  };
}
```

---

## Pattern 8 — Resetting state between runs

Long tests leave junk in the DB. Options:

### A. Pre-test cleanup (recommended for local)

Add a teardown step in the CI workflow:

```yaml
- run: |
    docker compose exec backend psql -U user -c "DELETE FROM users WHERE email LIKE 'perf-%';"
    # ...or run a dedicated cleanup endpoint
```

### B. Soft-delete + TTL (if the backend supports it)

Tag test data with `test_run_id = <env var>` and clean up on teardown.

### C. Scoped DB (testcontainers-style, ephemeral)

Use a dedicated Postgres per CI run. Destroyed after. Clean but slower.

For long-running staging environments, prefer A.

---

## Anti-patterns

### ❌ Generate all data upfront in `setup()`

```js
export function setup() {
  const users = [];
  for (let i = 0; i < 100_000; i++) users.push(...);  // BAD
  return { users };
}
```

`setup()` runs on the single k6 control process. 100k entries = huge memory + slow start. Use SharedArray (VM-local, lazy-loaded per VM).

### ❌ Math.random() for unique keys

```js
const email = `test-${Math.random()}@test.local`;  // COLLISIONS at high VUs
```

Low-entropy randomness collides. Use `__VU` + `__ITER` + timestamp (Pattern 3).

### ❌ Reading a large file in the default function

```js
export default function () {
  const data = JSON.parse(open('big.json'));   // opens ONCE per iteration — SLOW
  // ...
}
```

`open()` is fine — it caches — but JSON.parse is not. Parse ONCE in SharedArray's init function.

### ❌ Generating unique data with `uuid` library

```js
import { uuidv4 } from 'https://jslib.k6.io/k6-utils/...';
const id = uuidv4();  // fine, but OVERKILL for most cases
```

UUIDs work but `__VU + __ITER + timestamp` is faster and uniqueness-sufficient for performance tests.

---

## Checklist before generating test data

- [ ] Endpoints with unique constraints have per-request unique data
- [ ] SharedArray used for read-only pools (≥ 50 items)
- [ ] Payload sizes vary (not all 100-byte hello world)
- [ ] Weighted distribution matches production if known
- [ ] Seed script exists for the DB (test users present)
- [ ] Cleanup strategy decided (pre-test, teardown, or scoped DB)
