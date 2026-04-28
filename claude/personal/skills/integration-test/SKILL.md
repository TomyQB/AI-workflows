---
name: integration-test
description: >
  Generate HTTP controller integration tests per endpoint with real Postgres (Testcontainers)
  and WireMock for external APIs. Covers 100% of technical scenarios (auth, validation, status codes,
  idempotency, rate limit) AND functional scenarios derived from SDD specs (DB state, events, headers,
  negative space). Locks the functional contract so technically-correct but functionally-wrong code
  changes fail the tests. v1: Spring Boot (Java) + Postgres only.
  Trigger: When the user asks for "integration tests", "controller tests", "endpoint tests",
  "tests de integración", "contract tests", or invokes `/integration-test {controller-path}`
  in a Spring Boot project.
license: MIT
metadata:
  author: gentleman-programming
  version: "1.0"
allowed-tools: Read, Write, Edit, Glob, Grep, Bash
---

## Purpose

You are a sub-agent responsible for GENERATING integration tests at the HTTP controller level. You target Spring Boot projects. For each endpoint in the provided controller, you produce tests that:

1. Cover the full **technical HTTP matrix** (status codes, validation, auth, content-types, idempotency, rate limit).
2. Cover the **functional spec scenarios** (side effects: DB state, published events, outbound HTTP calls, headers).
3. Assert **negative space** — what must NOT happen.

Execute with **real Postgres** (Testcontainers) and **WireMock** for external APIs. Mocks at the service/repository layer are forbidden — they hide functional regressions, which is exactly what this skill prevents.

### Principle

> If the code changes in a way that is technically valid but functionally incorrect, the generated test MUST fail.

You do NOT modify controllers. You GENERATE tests only. If a generated test fails, you report it — you do not silence it or skip it.

---

## When to Use

- User asks for integration tests / controller tests / endpoint tests on a Spring Boot controller
- After `sdd-apply` completes and unit tests exist — this is the HTTP contract hardening layer
- On legacy Spring Boot controllers that lack integration coverage
- Before a production release, to lock the functional contract

**Do NOT use when:**
- Stack is not Spring Boot (v1 limitation — abort with clear message)
- Controller has zero side effects and is a pure data pass-through (add a TODO instead)
- Target is a GraphQL resolver, gRPC service, WebSocket endpoint, or message consumer (out of scope v1)

---

## What You Receive

From the orchestrator or direct invocation:
- **Required**: `target` — path to the `@RestController` or `@Controller` file, OR fully-qualified class name (`com.app.users.UserController`)
- **Optional**: `change-name` — SDD change name to load spec scenarios from engram (`sdd/{change-name}/spec`)
- **Optional**: `endpoints` — subset of endpoints to cover (default: all endpoints in the controller)

---

## Supported Stack (v1)

| Dimension | Supported |
|-----------|-----------|
| Language | Java (8+) |
| Framework | Spring Boot (2.x, 3.x) |
| Web model | Servlet (MVC). WebFlux reactive NOT supported in v1 |
| Test framework | JUnit 5 |
| HTTP test client | MockMvc (default) or `TestRestTemplate` with random port |
| Database | Postgres only, via Testcontainers |
| External HTTP | WireMock |
| Build tool | Maven or Gradle |

If the detected stack does not match, **ABORT with a clear message** pointing to the "Extending the skill" section.

---

## Execution Flow

### Step 1: Detect Project Context

```
Preflight:
├── Run: docker info
│   └── If fails → ABORT: "Docker is required for Testcontainers. Start Docker and retry."
├── Read pom.xml OR build.gradle(.kts)
├── Verify: spring-boot-starter-web present
├── Verify: spring-boot-starter-test present (includes MockMvc + JUnit 5)
├── Check for: org.testcontainers:junit-jupiter + org.testcontainers:postgresql
│   └── If missing → report add-dep commands, ABORT until user adds them
├── Check for: com.github.tomakehurst:wiremock-jre8-standalone OR org.wiremock:wiremock-standalone
│   └── If missing AND controller has external HTTP clients → report add-dep commands, ABORT
├── Detect Spring Security: spring-boot-starter-security present?
│   └── If yes, auth tests WILL be generated (401/403 cases)
├── Detect DB driver: org.postgresql:postgresql present
│   └── If missing → ABORT: "Postgres driver missing, add it to generate Testcontainers-based tests"
└── Reuse cache: mem_search("sdd-init/{project}") → if hit, skip redundant detection
```

### Step 2: Parse the Controller

Read the target file. Extract for each handler:
- HTTP method + path: `@GetMapping`, `@PostMapping`, `@PutMapping`, `@PatchMapping`, `@DeleteMapping`, `@RequestMapping(method = ...)`
- Class-level base path: `@RequestMapping("/prefix")` on class
- Path variables: `@PathVariable`
- Query params: `@RequestParam` (note `required=true/false`, `defaultValue`)
- Headers: `@RequestHeader`
- Body: `@RequestBody` + `@Valid` → inspect the DTO class for Bean Validation constraints (`@NotNull`, `@NotBlank`, `@Email`, `@Size`, `@Min`, `@Max`, `@Pattern`)
- Response: return type (`ResponseEntity<T>`, `T`, `void`) + `@ResponseStatus`
- Auth: `@PreAuthorize`, `@Secured`, `@RolesAllowed` on method or class
- Exception handlers in same class or `@ControllerAdvice` → map to expected status codes
- Side effects: injected `@Service`, `@Repository`, `ApplicationEventPublisher`, `RestTemplate`, `WebClient`, Feign clients

### Step 3: Load Spec Scenarios (if available)

```
If change-name provided:
├── mem_search(query: "sdd/{change-name}/spec", project: "{project}")
├── mem_get_observation(id) → full spec content
└── Extract GIVEN/WHEN/THEN blocks → functional scenario list

Else:
├── Search openspec/changes/*/specs/*.md for files that mention the controller name
├── Search docs/specs/ for matching files
├── Parse OpenAPI YAML/JSON if present (look for operationId matching handler names)
└── Parse JavaDoc on handlers for informal scenarios
```

### Step 4: Load the Stack Module

```
Read: stacks/java-spring.md
```

This module contains the detailed test generation patterns for Spring Boot. Follow it exactly.

### Step 5: Build the Test Case Matrix

Combine two sources:

**Technical cases** (from `matrices/technical-matrix.md`): universal HTTP matrix applied per endpoint method.

**Functional cases** (from `matrices/functional-matrix.md`): derived from spec scenarios (Step 3) and side-effect analysis of the controller (Step 2).

For each endpoint, produce a tagged list:
```
POST /users
├── [TECH] creates user → 201 + Location header + persisted row
├── [TECH] missing email → 400 with field error
├── [TECH] invalid email format → 400 with field error
├── [TECH] duplicate email → 409
├── [TECH] no JWT → 401
├── [TECH] USER role when ADMIN required → 403
├── [TECH] wrong Content-Type → 415
├── [FUNC] password is BCrypt hashed in DB
├── [FUNC] UserCreatedEvent published with correct payload
├── [FUNC] welcome email sent (WireMock verify)
├── [FUNC] audit log row inserted
├── [NEG] response body does NOT include password hash
└── [NEG] no event published on validation failure
```

### Step 6: Generate the Test Files

Follow `stacks/java-spring.md` patterns. Produce:

1. **`IntegrationTestBase.java`** (once per project, skip if exists and compatible):
   - `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `@AutoConfigureMockMvc` + `@Testcontainers`
   - Shared Postgres container with `withReuse(true)`
   - `@DynamicPropertySource` wiring datasource
   - DB cleanup helper (`@BeforeEach truncate all tables except flyway_schema_history`)

2. **`{Controller}IntegrationTest.java`**:
   - Extends `IntegrationTestBase`
   - One `@Nested class` per endpoint
   - Sections: `// Technical`, `// Functional`, `// Negative space`
   - `@DisplayName` on every class and test — use human-readable spec language

3. **`application-integration.yml`** (if absent):
   - `spring.profiles.active: integration`
   - `spring.jpa.hibernate.ddl-auto: validate` (migrations are authoritative)
   - WireMock base URL wiring

4. **`src/test/resources/db/fixtures/`** (optional):
   - SQL seed files referenced via `@Sql` for scenarios that need pre-existing state

### Step 7: Execute the Suite

Detect test runner and execute ONLY the newly generated tests:

```
Maven:   ./mvnw test -Dtest='{Controller}IntegrationTest'
Gradle:  ./gradlew test --tests '*{Controller}IntegrationTest'
```

Capture: total / passed / failed / skipped + exit code.

If any test fails:
- Report the failing test name + assertion error + captured stack trace
- DO NOT `@Disabled` or `@Ignore` the test
- DO NOT modify the controller to make the test pass
- Return `status: partial` and let the user decide

### Step 8: Persist Report to Engram

```
mem_save(
  title: "integration-tests/{controller-slug}",
  topic_key: "integration-tests/{controller-slug}",
  type: "architecture",
  project: "{project}",
  scope: "project",
  content: "{report markdown — see template below}"
)
```

`controller-slug` = kebab-case of class name (`UserController` → `user-controller`).

Report template:
```markdown
## Integration Test Report — {ControllerName}

**Stack**: Spring Boot {version} + JUnit 5 + MockMvc + Testcontainers-Postgres + WireMock
**Generated at**: {ISO timestamp}
**Source spec**: {change-name or "inferred from controller"}

### Endpoints Covered
| Method | Path | Tests Generated | Passing |
|--------|------|-----------------|---------|
| POST   | /users | 13 | 13/13 ✅ |

### Technical Matrix Coverage
{table per endpoint with checklist of the technical matrix}

### Functional Matrix Coverage (spec scenarios)
| Scenario | Test | Result |
|----------|------|--------|
| Password hashed before persist | `hashes password with BCrypt before persisting` | ✅ PASS |
| Welcome email sent | `calls email service with welcome template` | ✅ PASS |

### Negative Space Coverage
{list of negative-space tests and their results}

### Files Generated
- src/test/java/{package}/integration/IntegrationTestBase.java
- src/test/java/{package}/integration/{Controller}IntegrationTest.java
- src/test/resources/application-integration.yml

### Dependencies Added
{list of pom.xml / build.gradle additions, if any}
```

### Step 9: Return Envelope

Return this structure (follows `_shared/sdd-phase-common.md` Section D):

```markdown
## Status
{success | partial | blocked}

## Executive Summary
Generated {N} integration tests covering {M} endpoints on {Controller}. All passing ({X}/{Y}).
Technical matrix: {P}% coverage. Functional matrix: {Q}/{R} spec scenarios covered.

## Artifacts
- src/test/java/{package}/integration/{Controller}IntegrationTest.java
- src/test/java/{package}/integration/IntegrationTestBase.java
- src/test/resources/application-integration.yml
- engram: integration-tests/{controller-slug}

## Next Recommended
- Add `./mvnw verify` (or equivalent Gradle task) to the CI pipeline
- Consider running this skill on sibling controllers: {list}

## Risks
- Testcontainers requires Docker running on CI agents
- First-run Postgres image pull ~30s (cached thereafter)
- {any specific risk found}

## Skill Resolution
{injected | fallback-registry | fallback-path | none}
```

---

## Critical Patterns

Do not violate these under any circumstances.

### Assertion Quality (imported from strict-tdd.md)

See `~/.claude/skills/sdd-apply/strict-tdd.md:205-350` for the full catalog. Summary:

- **Banned**: `assertThat(result).isNotNull()` alone, CSS class assertions, smoke-only tests, type-only assertions, tautologies.
- **Required**: every assertion must call production code, assert a concrete expected value from the spec, and fail if the implementation logic is broken.
- **Side effects**: every scenario with side effects MUST assert BOTH the response AND the side effect (DB row, published event, WireMock call).
- **Negative space**: for every `THEN` in the spec, consider the implicit "and NOT X" — generate an assertion that proves X did not happen.

### No Mocks at the Service/Repository Layer

**Forbidden**: `@MockBean UserRepository`, `Mockito.when(userService.save(...))`.

Why: mocks at the business layer hide exactly the regressions this skill is designed to catch. If a refactor breaks the `save → hash` pipeline, a mocked `UserRepository` makes the test pass; a real Postgres write does not.

**Allowed**: WireMock for outbound HTTP (Step 5 of `http-stubs-wiremock.md`). That is a network boundary, not a business one.

### Real DB State Assertions

After every mutating call, assert DB state via `JdbcTemplate` or `TestEntityManager.find(...)`:

```java
// After POST /users
Long userId = extractId(response);
var row = jdbc.queryForMap("SELECT email, password_hash FROM users WHERE id = ?", userId);
assertThat(row.get("email")).isEqualTo("john@test.com");
assertThat(row.get("password_hash"))
    .asString()
    .startsWith("$2a$")   // BCrypt signature
    .isNotEqualTo("plain-password-123");
```

### Event Assertions

For `ApplicationEventPublisher` usage, spy on the publisher or use a `@TestConfiguration` that captures events into a list:

```java
@TestConfiguration
static class EventCaptureConfig {
    @Bean ApplicationEventCapturer capturer() { return new ApplicationEventCapturer(); }
}

@Autowired ApplicationEventCapturer events;

// In test:
events.clear();
mockMvc.perform(post("/users").contentType(JSON).content(...));
assertThat(events.ofType(UserCreatedEvent.class))
    .hasSize(1)
    .first()
    .satisfies(e -> {
        assertThat(e.userId()).isNotNull();
        assertThat(e.email()).isEqualTo("john@test.com");
    });
```

### External HTTP Assertions (WireMock)

Always verify: the stub was called, exactly N times, with the expected body:

```java
wireMock.verify(exactly(1),
    postRequestedFor(urlEqualTo("/emails"))
        .withRequestBody(matchingJsonPath("$.to", equalTo("john@test.com")))
        .withRequestBody(matchingJsonPath("$.template", equalTo("welcome"))));
```

Verifying the stub ran without checking the body = broken contract test.

---

## Rules

- ALWAYS run `docker info` before any generation — fail fast if Docker is absent
- ALWAYS use Testcontainers Postgres, never H2 or in-memory alternatives
- ALWAYS parse the controller AST/annotations before generating — do not guess endpoints
- ALWAYS generate both technical and functional tests per endpoint — not one or the other
- ALWAYS assert DB state after every mutating endpoint call
- ALWAYS verify WireMock stub invocations with body matchers, not just url
- ALWAYS include negative-space tests for scenarios with multiple possible side effects
- ALWAYS use `@DisplayName` with the language of the spec, not the method name
- NEVER modify the controller under test — report issues, don't fix them
- NEVER mock `@Service`, `@Repository`, `@Component` beans — use real beans + Testcontainers
- NEVER use `@Disabled` / `@Ignore` to hide failing generated tests
- NEVER skip a scenario because it seems redundant with a unit test — this layer is the contract
- NEVER use Spring's `@DataJpaTest` for controller tests — it does not load the MVC layer
- NEVER use `MockMvcBuilders.standaloneSetup()` — it bypasses Spring Security and filters
- If the controller has zero observable side effects (pure pass-through), report `blocked` with a TODO
- Apply assertion quality rules from `strict-tdd.md` to every generated assertion
- If stack detection fails (not Spring Boot, not Java, WebFlux), ABORT with clear guidance

---

## Extending the Skill

For future stack support, add a new file under `stacks/` and update Step 1 detection in this `SKILL.md`:

- `stacks/node.md` — Express/Fastify/NestJS + supertest
- `stacks/python.md` — FastAPI/Flask/Django + httpx TestClient
- `stacks/go.md` — Gin/Echo/chi/stdlib + httptest
- `stacks/dotnet.md` — ASP.NET Core + WebApplicationFactory

Same structure applies: matrices and assets are stack-agnostic; only the generation recipe per stack changes.

---

## Resources

- **Stack module**: [stacks/java-spring.md](stacks/java-spring.md) — generation recipe for Spring Boot
- **Technical matrix**: [matrices/technical-matrix.md](matrices/technical-matrix.md) — universal HTTP cases per method
- **Functional matrix**: [matrices/functional-matrix.md](matrices/functional-matrix.md) — spec-derived scenario guide
- **Testcontainers snippets**: [assets/testcontainers-postgres.md](assets/testcontainers-postgres.md)
- **WireMock patterns**: [assets/http-stubs-wiremock.md](assets/http-stubs-wiremock.md)
- **References**: [references/docs.md](references/docs.md) — cross-links to strict-tdd, engram-convention
