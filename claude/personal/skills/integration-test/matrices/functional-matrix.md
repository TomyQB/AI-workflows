# Functional Matrix — Deriving Tests from Spec Scenarios

> Loaded by `SKILL.md` Step 5. Stack-agnostic guide that converts spec scenarios into test cases
> and enforces the "functionally-wrong changes must fail" principle.

---

## The core idea

A **technical test** proves the endpoint returned the correct HTTP status and body. That is necessary but insufficient. A **functional test** proves the endpoint produced the correct side effects in the real world — and that nothing else slipped through.

If someone changes the code such that:
- The endpoint still returns 201
- The response body still matches the schema
- The unit tests still pass

...but the password is no longer hashed, or the welcome email is not sent, or two events are published instead of one, or an extra row is written to an audit table — the **functional test** is the layer that catches it.

---

## The four dimensions of a functional assertion

Every functional test must assert ALL FOUR of these, per scenario:

### 1. Response
The HTTP response: status code + headers + body. Assert concrete expected values derived from the spec's `THEN`, not type checks.

### 2. Persistent state
Read the database directly after the call. Assert that the right rows exist with the right values, and nothing extra exists.

### 3. Out-of-process side effects
- Events published (count + payload)
- Outbound HTTP calls (count + body + headers)
- Messages produced to brokers (v2 scope)
- Cache writes / invalidations (if observable)

### 4. Negative space
What should NOT change or NOT happen. Often missing in hand-written tests. Always present in generated tests.

---

## Scenario → Test translation

### Input: spec scenario (GIVEN / WHEN / THEN)

```
Scenario: Successful user registration
  GIVEN the database has no user with email "john@test.com"
  AND the emails service is reachable
  WHEN a POST /users is made with
    | email    | john@test.com |
    | password | secret-123    |
    | name     | John          |
    and a valid ADMIN JWT
  THEN the response is 201 Created
  AND the Location header points to the new user
  AND the response body contains id, email, name
  AND the password is stored hashed with BCrypt
  AND a UserCreatedEvent is published with the new user id
  AND a welcome email is sent to john@test.com
```

### Output: one generated `@Test`

```java
@Test
@DisplayName("successfully registers a user and emits expected side effects")
void successfullyRegistersUser() throws Exception {
    // GIVEN
    int usersBefore = jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
    wireMock.stubFor(post("/emails").willReturn(ok()));
    events.clear();

    // WHEN
    var response = mockMvc.perform(post("/users")
            .with(jwt().authorities(() -> "ROLE_ADMIN"))
            .contentType("application/json").content("""
                {"email":"john@test.com","password":"secret-123","name":"John"}
                """))
        // THEN — response
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", matchesPattern("/users/\\d+")))
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.email").value("john@test.com"))
        .andExpect(jsonPath("$.name").value("John"))
        .andReturn();

    // THEN — persistent state
    var row = jdbc.queryForMap(
        "SELECT id, email, name, password_hash FROM users WHERE email = ?",
        "john@test.com");
    assertThat(row.get("name")).isEqualTo("John");
    assertThat(row.get("password_hash"))
        .asString()
        .startsWith("$2a$")
        .isNotEqualTo("secret-123");
    int usersAfter = jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
    assertThat(usersAfter).isEqualTo(usersBefore + 1);

    // THEN — events
    assertThat(events.ofType(UserCreatedEvent.class))
        .hasSize(1)
        .first()
        .satisfies(e -> {
            assertThat(e.userId()).isEqualTo(row.get("id"));
            assertThat(e.email()).isEqualTo("john@test.com");
        });

    // THEN — outbound HTTP
    wireMock.verify(exactly(1),
        postRequestedFor(urlEqualTo("/emails"))
            .withRequestBody(matchingJsonPath("$.to", equalTo("john@test.com")))
            .withRequestBody(matchingJsonPath("$.template", equalTo("welcome"))));

    // THEN — negative space
    assertThat(events.all())
        .as("only UserCreatedEvent should be published")
        .hasSize(1);
    wireMock.verify(0, anyRequestedFor(anyUrl()).atPriority(5));  // no other calls
}
```

---

## Deriving negative space

For every `THEN` clause in the spec, think of its **implicit complement**. Generate an assertion that proves the complement did not happen.

| THEN says… | Negative-space assertion |
|------------|--------------------------|
| …password is stored hashed | Response body does NOT include the plaintext OR the hash |
| …email is sent to the new user | Email is NOT sent on validation failure |
| …UserCreatedEvent is published | No `UserCreatedEvent` is published on error paths |
| …audit row is inserted | Audit row is NOT inserted when the request fails at validation |
| …rate-limit counter increments | Rate-limit counter does NOT increment for a rejected auth request |
| …cache is invalidated | Unrelated cache keys remain untouched |
| …other users' data is unchanged | Count other users' rows before + after, assert equal |

Generate a negative-space test PER side effect that appears in the spec. Do NOT skip these — they are the highest-value tests this skill produces.

---

## Assertion Quality Rules (MANDATORY)

Imported from `~/.claude/skills/sdd-apply/strict-tdd.md:205-350`. Summary here for convenience — the full catalog is authoritative.

### Banned (generated tests must NEVER contain these)

```java
// ❌ Tautologies
assertThat(true).isTrue();
assertThat(1).isEqualTo(1);

// ❌ Type-only
assertThat(result).isNotNull();
assertThat(response.getBody()).isInstanceOf(String.class);

// ❌ Empty-collection without setup context
assertThat(rows).isEmpty();    // ONLY if you set up conditions for emptiness

// ❌ CSS class / style assertions
assertThat(element.getAttribute("class")).contains("btn-primary");

// ❌ Smoke-only
mockMvc.perform(get("/users")).andExpect(status().isOk());
// and nothing else — proves only that the endpoint didn't crash

// ❌ Over-mocking
// 8+ @MockBean on a single test class means wrong layer — delete the test
```

### Required

Every assertion must:
1. **Call production code** — the test invokes the endpoint through the real servlet/filter chain.
2. **Assert a concrete expected value from the spec** — `"john@test.com"`, `201`, `1`, not "is not null".
3. **Fail if the implementation logic is broken** — if you mutate the logic in a controller to a plausible-but-wrong variant, the test must turn red.

### The mutation test

Before finalizing a generated test, mentally ask: *"If the implementation did THE WRONG THING, would this test fail?"* Specific checks:

| Mutation | Will the test fail? |
|----------|---------------------|
| Controller returns 200 instead of 201 | ✅ status assertion |
| Controller omits Location header | ✅ header assertion |
| Controller stores password in plaintext | ✅ DB hash assertion |
| Controller publishes 0 events | ✅ event count assertion |
| Controller publishes 2 events | ✅ event count assertion (uses `hasSize(1)`, not `hasSizeGreaterThanOrEqualTo(1)`) |
| Controller sends welcome email to the wrong address | ✅ WireMock body matcher |
| Controller sends welcome email with wrong template | ✅ WireMock body matcher |
| Controller forgets to log an audit row | ✅ audit table assertion |
| Controller writes audit row with wrong user_id | ✅ assert audit row values |

If any of these would pass silently with the generated test, the test is incomplete. Add the missing assertion.

---

## Mapping spec DATA TABLES to tests

When the spec uses a data table, generate one test per row (parameterized or duplicated):

```
Scenario Outline: Email validation
  WHEN a POST /users is made with email "<email>"
  THEN the response is 400 with a field error on "email"
  Examples:
    | email           |
    | ""              |
    | "plain-string"  |
    | "no-domain@"    |
    | "@no-local"     |
    | "spaces in@m.c" |
```

Use JUnit 5's `@ParameterizedTest`:

```java
@ParameterizedTest
@DisplayName("rejects invalid email format with 400")
@ValueSource(strings = {"", "plain-string", "no-domain@", "@no-local", "spaces in@m.c"})
void rejectsInvalidEmail(String invalidEmail) throws Exception {
    String body = "{\"email\":\"" + invalidEmail + "\",\"password\":\"x\",\"name\":\"J\"}";
    mockMvc.perform(post("/users")
            .with(jwt().authorities(() -> "ROLE_ADMIN"))
            .contentType("application/json").content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[?(@.field == 'email')]").exists());
}
```

---

## Time-dependent scenarios

When a scenario references "now", "yesterday", "expired":

1. Inject a fixed `Clock` bean via `@TestConfiguration`:
   ```java
   @TestConfiguration
   static class FixedClockConfig {
       @Bean @Primary Clock testClock() {
           return Clock.fixed(Instant.parse("2026-04-19T10:00:00Z"), ZoneOffset.UTC);
       }
   }
   ```
2. Controller / service MUST take `Clock` as a dependency — if they use `Instant.now()` directly, flag it as a design issue in the report.
3. Assert exact timestamps in DB rows: `assertThat(row.get("created_at")).isEqualTo(Timestamp.from(Instant.parse(...)))`.

---

## Concurrency scenarios

If the spec specifies "when two requests arrive simultaneously":

1. Use `CompletableFuture.supplyAsync(...)` to fire parallel `MockMvc` calls.
2. Assert exactly one succeeds and one returns 409 (for optimistic locking) or the spec's defined outcome.
3. Assert only one side effect (event, email) fired in total.

These are expensive tests — generate them ONLY when the spec explicitly mentions concurrency.

---

## When to SKIP a scenario

- The scenario is covered 1:1 by a unit test AND has no side effects (rare — most scenarios have some side effect)
- The scenario references infrastructure out of v1 scope (WebSocket, gRPC, message broker)
- The scenario references an external dependency that cannot be stubbed (human approval step, physical device)

In all skip cases, record it in the final report with the reason.

---

## Final checklist per generated test

Before writing the test to disk, confirm:

- [ ] Every `THEN` clause from the spec has at least one assertion
- [ ] Persistent state is asserted post-call (DB read)
- [ ] Every side effect declared in the spec has a count + payload assertion
- [ ] Negative space is asserted for each declared side effect
- [ ] No banned assertion patterns (tautology, type-only, smoke-only)
- [ ] Mutation test: a plausible-but-wrong implementation would fail this test
- [ ] `@DisplayName` uses the spec's language, not the method name
- [ ] No `@MockBean` on the controller's own dependencies
