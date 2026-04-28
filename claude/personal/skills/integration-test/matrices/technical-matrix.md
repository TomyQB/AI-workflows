# Technical Matrix — Universal HTTP Scenarios

> Loaded by `SKILL.md` Step 5. Stack-agnostic catalog of technical cases to generate per endpoint.
> The goal is **100% coverage of the HTTP contract surface**: every status code the endpoint can legitimately return must have a test that triggers it.

---

## How to read this matrix

For each HTTP method below, iterate through every scenario. For each scenario:

1. Check the endpoint is capable of producing it (auth required? validation present? idempotency key? rate limit filter?).
2. If yes → GENERATE the test.
3. If no → SKIP with a comment in the report noting why it was skipped.

Scenarios marked `ALWAYS` must always be generated regardless of controller features.

---

## POST — Create a resource

| Scenario | Trigger | Status | Assertions |
|----------|---------|--------|------------|
| Happy path (ALWAYS) | Valid auth + valid body | 201 | Location header matches `/resource/{id}` pattern · Response body matches spec shape · Row persisted in DB matches request |
| Happy path: returns 200 (ALWAYS for spec-defined 200) | Same as above, when endpoint is a creating PUT-like POST | 200 | Response body matches spec shape |
| JSON malformed | Raw string `"not json"` body | 400 | Error response has a code/message indicating parse failure |
| Missing body | No body + Content-Type: application/json | 400 | Error response mentions body is required |
| Missing required field | One test PER `@NotNull`/`@NotBlank` field, omitted | 400 | `errors[?(@.field == 'X')]` present |
| Invalid field format | One test per `@Email`/`@Pattern`/`@UUID`-like constraint | 400 | Error references the field with a format message |
| Field length violation | One test per `@Size` below min, one above max | 400 | Error references the field |
| Field numeric range | One test per `@Min`/`@Max` boundary violation | 400 | Error references the field |
| Nested object invalid | Invalid nested `@Valid` object | 400 | Error references the nested path (`items[0].sku`) |
| Wrong Content-Type | `Content-Type: text/plain` with JSON body | 415 | 415 or 400 depending on Spring config |
| Missing Content-Type | No Content-Type header | 415 | 415 or 400 |
| Payload too large | Body > `spring.servlet.multipart.max-request-size` (only if configured) | 413 | 413 |
| Unauthenticated | No JWT / no session cookie | 401 | 401 with WWW-Authenticate header if configured |
| Invalid auth | Expired / malformed JWT | 401 | 401 |
| Insufficient role | Valid JWT but role missing | 403 | 403 |
| Duplicate unique key | POST a second time with same unique field | 409 | 409 |
| Foreign key violation | POST with FK pointing to non-existent parent | 400 or 409 | Status per controller advice |
| Idempotency: repeat with same key | POST twice with same `Idempotency-Key` header (ONLY if feature exists) | 201 + 200 | Second call returns cached response · Only ONE row persisted · Side effects fired once |
| Idempotency: different payload, same key | POST with same key but different body (if feature exists) | 422 | Business error |
| Rate limit exceeded | N+1 calls within window (ONLY if filter exists) | 429 | Retry-After header present |
| Method not allowed | PUT/PATCH on the POST endpoint | 405 | Allow header lists supported methods |

**Side-effect assertions bundled with the happy path** (ALWAYS check):
- DB row persisted with exactly the fields from the request (spot-check 3+ fields)
- Generated ID is non-null and unique
- `createdAt`/`updatedAt` populated if the entity has them
- Published events fired exactly once (assert count + payload)
- Outbound HTTP calls matched WireMock stubs exactly (assert count + body matchers)
- No `@Async` side effect has escaped the test (use `@SpyBean` on the executor or a synchronous test executor)

**Negative space** (ALWAYS on happy path):
- Response body does NOT include secret fields (password hash, API keys, internal IDs if spec says so)
- No additional DB rows written (count the target table before + after)
- No unexpected events published

---

## GET `/{id}` — Read a single resource

| Scenario | Trigger | Status | Assertions |
|----------|---------|--------|------------|
| Happy path (ALWAYS) | Existing id + valid auth | 200 | Body matches spec shape · All fields present |
| Not found (ALWAYS) | Non-existent id | 404 | Error response follows global error schema |
| Id format invalid | `/users/abc` when id is numeric | 400 | Converter-level error |
| Unauthenticated (if required) | No JWT | 401 | 401 |
| Insufficient role (if required) | Valid JWT, wrong role | 403 | 403 |
| Access control: other user's resource | Valid JWT, trying to read resource owned by a different user (if ownership is enforced) | 403 or 404 | Whichever the spec chose — do not leak existence |
| Soft-deleted resource | Resource exists but `deleted_at` is set (if soft delete exists) | 404 | Treated as not found |
| ETag round-trip | First GET captures ETag, second GET with `If-None-Match` (if controller returns ETag) | 304 | Empty body |
| Cache-Control header | Any GET | 200 | Header value matches spec (`max-age=0`, `no-store`, etc.) |

---

## GET `/` — List / search

| Scenario | Trigger | Status | Assertions |
|----------|---------|--------|------------|
| Empty list | DB has no rows | 200 | `content: []` · `totalElements: 0` |
| Non-empty list (ALWAYS) | DB seeded with N rows | 200 | `content.length == min(N, pageSize)` · Each item matches spec shape |
| Pagination: first page | `?page=0&size=2` with 5 seeded rows | 200 | 2 items · `totalElements: 5` · `totalPages: 3` |
| Pagination: middle page | `?page=1&size=2` | 200 | 2 items · consistent ordering |
| Pagination: last page | `?page=2&size=2` with 5 rows | 200 | 1 item · `last: true` |
| Pagination: beyond last page | `?page=99` | 200 | `content: []` · `last: true` |
| Page size too large | `?size=9999` with limit of 100 | 200 | Capped at 100 (assert) OR 400 (if rejected) per spec |
| Sort ascending | `?sort=field,asc` on a sortable field | 200 | Items ordered ASC by that field |
| Sort descending | `?sort=field,desc` | 200 | Items ordered DESC |
| Sort by non-existent field | `?sort=bogus,asc` | 400 | Clear error |
| Filter by query param | One test per `@RequestParam` that maps to a filter | 200 | Only matching rows returned |
| Combined filters | Two filters at once | 200 | Intersection |
| Invalid filter value | Wrong type for typed param | 400 | Converter error |
| Unauthenticated (if required) | No JWT | 401 | 401 |
| Scoped to user | Valid JWT as user A — should only see own resources (if scoped) | 200 | No resources of user B appear |

---

## PUT — Full update

| Scenario | Trigger | Status | Assertions |
|----------|---------|--------|------------|
| Happy path (ALWAYS) | Existing id + full valid body | 200 or 204 | Body reflects new state · DB row reflects new state |
| Not found | Non-existent id | 404 | 404 |
| Validation failures | Same catalog as POST — one test per constraint | 400 | Field errors |
| Unauthenticated / Insufficient role | Same as POST | 401 / 403 | — |
| Optimistic locking conflict | Stale `@Version` in request (if entity uses it) | 409 | `OptimisticLockException` mapped to 409 |
| Concurrent write (integration-level) | Two sequential PUTs, second uses the stale version | 200 + 409 | First succeeds, second 409 |
| Unique constraint violation | Change unique field to one already in use | 409 | — |
| Missing required field | PUT is full replace — missing field is a validation error | 400 | — |

**Side effects**:
- DB row shows NEW values
- DB row is the SAME row (same `id`, `createdAt`)
- `updatedAt` advanced
- Changed-field events published (if spec defines them)

**Negative space**:
- Rows with different ids untouched
- No duplicate rows created

---

## PATCH — Partial update

| Scenario | Trigger | Status | Assertions |
|----------|---------|--------|------------|
| Happy path (ALWAYS) | Existing id + partial body touching one field | 200 or 204 | Only the touched field changed · Other fields preserved |
| Not found | Non-existent id | 404 | 404 |
| Empty body | `{}` | 200 or 400 per spec | No changes made OR explicit 400 |
| Invalid field in body | Validation failure | 400 | — |
| Read-only field | Attempt to patch `createdAt` or `id` | 400 or 403 | Rejected per spec |
| Unauthenticated / Insufficient role | Same as POST | 401 / 403 | — |

**Side effects**: same preservation assertions as PUT but stricter — untouched fields MUST remain byte-identical.

---

## DELETE

| Scenario | Trigger | Status | Assertions |
|----------|---------|--------|------------|
| Happy path (ALWAYS) | Existing id | 204 | Row no longer queryable (hard delete) OR `deleted_at` populated (soft delete) |
| Not found | Non-existent id | 404 | — |
| Idempotent repeat | DELETE twice in a row | 204 + 404 (or 204 + 204 per spec) | Assert consistent choice |
| Unauthenticated / Insufficient role | Same as POST | 401 / 403 | — |
| Blocked by constraint | Has dependent children, no cascade configured | 409 | Clear error |
| Cascade delete verified | Has dependent children, cascade configured | 204 | Children gone too |
| Soft delete preserves data | Soft delete mode | 204 | Row still exists with `deleted_at` set |

**Side effects**:
- Events published (`ResourceDeletedEvent`)
- Outbound cleanup calls (email unsubscribe, cache purge) fired

**Negative space**:
- Other rows untouched
- If soft delete: row count unchanged, only flag flipped

---

## HEAD / OPTIONS

Generate ONLY if the controller explicitly declares them. Do not invent.

| Method | Scenario | Status |
|--------|----------|--------|
| HEAD | Maps to GET — same status codes, empty body | Match GET |
| OPTIONS | CORS preflight for cross-origin endpoints | 200 with `Access-Control-Allow-*` headers |

---

## Multipart / File Upload

If the endpoint uses `@RequestPart` or `MultipartFile`:

| Scenario | Status |
|----------|--------|
| Happy path with small file | 201/200 + file persisted (check storage path or binary bytes in DB) |
| No file attached | 400 |
| File exceeds max size | 413 |
| Wrong MIME type | 415 |
| Corrupt file (if validation exists) | 400 or 422 |

---

## WebSocket / SSE / gRPC

**Out of scope for v1.** If detected in the controller, emit a TODO in the report and skip.

---

## Rate of generation

Typical coverage for a CRUD controller with 5 endpoints (POST, GET list, GET one, PUT, DELETE) and Bean Validation on the input DTO with 4 fields:

| Matrix area | Tests |
|-------------|-------|
| Happy paths | 5 |
| Validation (4 fields × 2 constraints avg) | ~8 |
| Auth (401, 403 per mutating endpoint) | 6 |
| Not found | 3 |
| Pagination + filters on list | 5 |
| Idempotency / rate limit (if applicable) | 0–3 |
| Content-type / method not allowed | 2–3 |
| **Subtotal technical** | **~30** |

Add ~10–15 functional tests from the spec + ~5 negative-space tests → total ~45–50 tests per controller.
