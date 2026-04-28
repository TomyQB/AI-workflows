# Stack Module — Spring Boot (Java)

> Loaded by `SKILL.md` Step 4 when the detected stack is Spring Boot.
> This file contains the full generation recipe. Follow it strictly.

---

## Required Dependencies

### Maven (`pom.xml`)

Add to `<dependencies>` with `<scope>test</scope>`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.wiremock</groupId>
    <artifactId>wiremock-standalone</artifactId>
    <version>3.9.2</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-test</artifactId>
    <scope>test</scope>
</dependency>
```

Add BOM if not present:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>testcontainers-bom</artifactId>
            <version>1.20.4</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### Gradle (`build.gradle.kts`)

```kotlin
dependencies {
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.wiremock:wiremock-standalone:3.9.2")
    testImplementation("org.springframework.security:spring-security-test")
}

dependencyManagement {
    imports {
        mavenBom("org.testcontainers:testcontainers-bom:1.20.4")
    }
}
```

If any are missing, **report the diff to the user and ABORT**. Do not silently add them.

---

## File Layout

```
src/
├── main/java/{base-pkg}/
│   └── {module}/
│       └── {Controller}.java           ← target (do not modify)
└── test/
    ├── java/{base-pkg}/integration/
    │   ├── IntegrationTestBase.java    ← shared, create once
    │   ├── support/
    │   │   ├── TestDbCleaner.java      ← truncation helper
    │   │   ├── AuthTestHelper.java     ← JWT/OAuth2 test tokens
    │   │   └── ApplicationEventCapturer.java
    │   └── {module}/
    │       └── {Controller}IntegrationTest.java
    └── resources/
        ├── application-integration.yml
        └── db/fixtures/*.sql          ← optional seeds
```

**Naming**: `{Controller}IntegrationTest` (not `{Controller}IT` — Surefire picks up `*Test` by default; if the project uses Failsafe with `*IT`, adapt accordingly).

---

## IntegrationTestBase (create once per project)

```java
package {base-pkg}.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("integration")
@Testcontainers
public abstract class IntegrationTestBase {

    @Container
    protected static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withReuse(true);

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired protected MockMvc mockMvc;
    @Autowired protected ObjectMapper objectMapper;
    @Autowired protected JdbcTemplate jdbc;

    @BeforeEach
    void resetDatabase() {
        TestDbCleaner.truncateAll(jdbc);
    }
}
```

### TestDbCleaner

```java
package {base-pkg}.integration.support;

import org.springframework.jdbc.core.JdbcTemplate;
import java.util.List;

public final class TestDbCleaner {
    private TestDbCleaner() {}

    public static void truncateAll(JdbcTemplate jdbc) {
        List<String> tables = jdbc.queryForList(
            "SELECT tablename FROM pg_tables WHERE schemaname = 'public' " +
            "AND tablename NOT IN ('flyway_schema_history', 'databasechangelog', 'databasechangeloglock')",
            String.class);
        if (tables.isEmpty()) return;
        String stmt = "TRUNCATE TABLE " +
            String.join(", ", tables) + " RESTART IDENTITY CASCADE";
        jdbc.execute(stmt);
    }
}
```

### `withReuse(true)` requires user opt-in

Append to `~/.testcontainers.properties`:

```
testcontainers.reuse.enable=true
```

If absent, first-run is slower but tests still pass. Document this in the final report.

---

## Controller Parsing Heuristics

### Detect endpoints

For each method in the class annotated with one of:

```
@GetMapping, @PostMapping, @PutMapping, @PatchMapping, @DeleteMapping, @RequestMapping
```

Extract:
- HTTP method (from annotation type or `method = RequestMethod.X`)
- Path: concatenate class-level `@RequestMapping` path (if any) + method annotation path
- Path variables from `@PathVariable`
- Query params from `@RequestParam` (respect `required`, `defaultValue`)
- Headers from `@RequestHeader`
- Request body DTO from `@RequestBody` parameter type

### Detect validation constraints on the request DTO

Read the DTO class (the type of `@RequestBody` parameter). Record constraints per field:

| Annotation | Test to generate |
|------------|------------------|
| `@NotNull` | POST/PUT with `field = null` → 400 |
| `@NotBlank` | POST/PUT with `field = ""` or `"   "` → 400 |
| `@Email` | POST/PUT with `field = "not-an-email"` → 400 |
| `@Size(min=M, max=N)` | Two tests: below min (M-1 chars), above max (N+1 chars) → 400 |
| `@Min(v)` / `@Max(v)` | Boundary-off-by-one tests → 400 |
| `@Pattern(regexp=...)` | Value NOT matching regex → 400 |
| `@Past` / `@Future` | Wrong-direction date → 400 |
| Nested `@Valid` | Recurse into nested type |

For each invalid field test, assert the error response shape (e.g., `$.errors[?(@.field == 'email')].message`).

### Detect auth requirements

Method or class annotations:
- `@PreAuthorize("hasRole('ADMIN')")` → generate 401 (no JWT) + 403 (wrong role) + 200/201 (correct role)
- `@Secured("ROLE_X")` → same
- `@RolesAllowed("X")` → same
- No annotations + `SecurityFilterChain` has `.anyRequest().authenticated()` → generate 401 only
- Controller paths matched in `.permitAll()` rules → no auth tests

### Detect exception handlers

Scan the controller and any `@ControllerAdvice`. Build a map of `ExceptionType → HTTP status`:

```
EntityNotFoundException → 404
DuplicateKeyException → 409
OptimisticLockException → 409
AccessDeniedException → 403
MethodArgumentNotValidException → 400 (automatic)
HttpMessageNotReadableException → 400 (automatic)
```

For each handler, generate a test that triggers the exception path (via DB seed, auth config, or malformed input).

### Detect side effects

Scan injected dependencies:
- `@Service`, `@Repository` → business/DB side effects → DB state assertion required
- `ApplicationEventPublisher` → event assertion required (capture via `ApplicationEventCapturer`)
- `RestTemplate`, `WebClient`, `@FeignClient` interfaces → external HTTP → WireMock stub + verify
- `JmsTemplate`, `KafkaTemplate`, `RabbitTemplate` → message broker → out of v1 scope, generate a TODO

---

## Test Class Structure

```java
package {base-pkg}.integration.{module};

import {base-pkg}.integration.IntegrationTestBase;
import {base-pkg}.integration.support.*;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("UserController — HTTP Contract")
class UserControllerIntegrationTest extends IntegrationTestBase {

    @RegisterExtension
    static final WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void externalApisBaseUrl(DynamicPropertyRegistry registry) {
        registry.add("emails.service.base-url", wireMock::baseUrl);
    }

    @Autowired ApplicationEventCapturer events;

    @BeforeEach void resetWireMock() { wireMock.resetAll(); events.clear(); }

    @Nested
    @DisplayName("POST /users")
    class CreateUser {

        // ─── Technical ────────────────────────────────────────────────
        @Test
        @DisplayName("creates a user and returns 201 with Location header")
        void createsUser() throws Exception {
            String body = """
                {"email":"john@test.com","password":"secret-123","name":"John"}
                """;
            wireMock.stubFor(post("/emails").willReturn(ok()));

            var response = mockMvc.perform(post("/users")
                    .with(jwt().authorities(() -> "ROLE_ADMIN"))
                    .contentType("application/json").content(body))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").value("john@test.com"))
                .andExpect(jsonPath("$.name").value("John"))
                .andReturn();

            assertThat(response.getResponse().getHeader("Location"))
                .matches("/users/\\d+");
        }

        @Test
        @DisplayName("rejects missing email with 400 and field error")
        void rejectsMissingEmail() throws Exception {
            String body = """
                {"password":"secret-123","name":"John"}
                """;
            mockMvc.perform(post("/users")
                    .with(jwt().authorities(() -> "ROLE_ADMIN"))
                    .contentType("application/json").content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field == 'email')]").exists());
        }

        @Test
        @DisplayName("rejects duplicate email with 409")
        void rejectsDuplicate() throws Exception {
            jdbc.update("INSERT INTO users (email, password_hash, name) VALUES (?, ?, ?)",
                    "john@test.com", "$2a$10$preexisting", "Existing");

            String body = """
                {"email":"john@test.com","password":"secret-123","name":"Another"}
                """;
            mockMvc.perform(post("/users")
                    .with(jwt().authorities(() -> "ROLE_ADMIN"))
                    .contentType("application/json").content(body))
                .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("rejects request without JWT with 401")
        void rejectsUnauthenticated() throws Exception {
            mockMvc.perform(post("/users")
                    .contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("rejects USER role when ADMIN is required with 403")
        void rejectsInsufficientRole() throws Exception {
            mockMvc.perform(post("/users")
                    .with(jwt().authorities(() -> "ROLE_USER"))
                    .contentType("application/json").content("""
                        {"email":"john@test.com","password":"x","name":"J"}
                        """))
                .andExpect(status().isForbidden());
        }

        // ─── Functional (from spec) ──────────────────────────────────
        @Test
        @DisplayName("hashes the password with BCrypt before persisting")
        void hashesPassword() throws Exception {
            wireMock.stubFor(post("/emails").willReturn(ok()));
            mockMvc.perform(post("/users")
                    .with(jwt().authorities(() -> "ROLE_ADMIN"))
                    .contentType("application/json").content("""
                        {"email":"john@test.com","password":"plain-123","name":"John"}
                        """))
                .andExpect(status().isCreated());

            String hash = jdbc.queryForObject(
                "SELECT password_hash FROM users WHERE email = ?",
                String.class, "john@test.com");
            assertThat(hash)
                .startsWith("$2a$")
                .isNotEqualTo("plain-123");
        }

        @Test
        @DisplayName("publishes UserCreatedEvent with the created user id and email")
        void publishesEvent() throws Exception {
            wireMock.stubFor(post("/emails").willReturn(ok()));
            mockMvc.perform(post("/users")
                    .with(jwt().authorities(() -> "ROLE_ADMIN"))
                    .contentType("application/json").content("""
                        {"email":"john@test.com","password":"x","name":"John"}
                        """))
                .andExpect(status().isCreated());

            assertThat(events.ofType(UserCreatedEvent.class))
                .hasSize(1)
                .first()
                .satisfies(e -> {
                    assertThat(e.email()).isEqualTo("john@test.com");
                    assertThat(e.userId()).isNotNull();
                });
        }

        @Test
        @DisplayName("sends a welcome email via the emails service")
        void sendsWelcomeEmail() throws Exception {
            wireMock.stubFor(post("/emails").willReturn(ok()));
            mockMvc.perform(post("/users")
                    .with(jwt().authorities(() -> "ROLE_ADMIN"))
                    .contentType("application/json").content("""
                        {"email":"john@test.com","password":"x","name":"John"}
                        """))
                .andExpect(status().isCreated());

            wireMock.verify(exactly(1),
                postRequestedFor(urlEqualTo("/emails"))
                    .withRequestBody(matchingJsonPath("$.to", equalTo("john@test.com")))
                    .withRequestBody(matchingJsonPath("$.template", equalTo("welcome"))));
        }

        // ─── Negative space ──────────────────────────────────────────
        @Test
        @DisplayName("does NOT expose the password hash in the response body")
        void doesNotLeakHash() throws Exception {
            wireMock.stubFor(post("/emails").willReturn(ok()));
            mockMvc.perform(post("/users")
                    .with(jwt().authorities(() -> "ROLE_ADMIN"))
                    .contentType("application/json").content("""
                        {"email":"john@test.com","password":"x","name":"John"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        }

        @Test
        @DisplayName("does NOT publish any event when validation fails")
        void noEventOnValidationFailure() throws Exception {
            mockMvc.perform(post("/users")
                    .with(jwt().authorities(() -> "ROLE_ADMIN"))
                    .contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());

            assertThat(events.all()).isEmpty();
            wireMock.verify(0, postRequestedFor(urlEqualTo("/emails")));
        }
    }
}
```

---

## Auth Helpers

For Spring Security tests, use `spring-security-test`'s request post-processors. Prefer `jwt()` for resource servers, `user(...)` for form login:

```java
.with(jwt().authorities(() -> "ROLE_ADMIN"))
.with(jwt().jwt(jwt -> jwt.subject("user-123").claim("scope", "users:write")))
.with(user("alice").roles("ADMIN"))
```

If the project uses opaque tokens or a custom token service, check `SecurityFilterChain` for `.oauth2ResourceServer()` config and adapt the helper accordingly.

---

## ApplicationEventCapturer

```java
package {base-pkg}.integration.support;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class ApplicationEventCapturer {
    private final List<Object> events = Collections.synchronizedList(new ArrayList<>());

    @EventListener
    public void capture(Object event) { events.add(event); }

    public void clear() { events.clear(); }
    public List<Object> all() { return List.copyOf(events); }

    @SuppressWarnings("unchecked")
    public <T> List<T> ofType(Class<T> type) {
        return events.stream()
            .filter(type::isInstance)
            .map(e -> (T) e)
            .collect(Collectors.toList());
    }
}
```

Register it as a `@TestConfiguration` if you do not want to pollute the main classpath:

```java
@TestConfiguration
static class CaptureConfig {
    @Bean ApplicationEventCapturer applicationEventCapturer() {
        return new ApplicationEventCapturer();
    }
}
```

---

## `application-integration.yml`

```yaml
spring:
  profiles:
    active: integration
  datasource:
    # URL/user/password are injected by @DynamicPropertySource from Testcontainers
    hikari:
      maximum-pool-size: 5
  jpa:
    hibernate:
      ddl-auto: validate     # migrations are authoritative; never create-drop here
    properties:
      hibernate.jdbc.time_zone: UTC
  flyway:
    enabled: true
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:9999   # bogus, overridden by jwt() post-processor

logging:
  level:
    org.springframework.web: INFO
    org.hibernate.SQL: WARN
```

Adjust `security.oauth2.*` to match the project's auth model, or remove the block entirely if not using OAuth2 resource server.

---

## DB Seeding Strategies

For scenarios needing pre-existing state, pick one:

### 1. Inline via `JdbcTemplate` (preferred for 1-3 rows)

```java
jdbc.update("INSERT INTO users (email, name) VALUES (?, ?)", "alice@test.com", "Alice");
```

### 2. `@Sql` annotation (multiple rows or cross-table fixtures)

```java
@Test
@Sql("/db/fixtures/ten-users.sql")
void paginatesUsers() { ... }
```

### 3. Test data builders (domain-heavy cases)

Create `UserTestDataBuilder` in `support/` that generates valid DTOs with sensible defaults and lets each test override fields. Do NOT share instances across tests.

---

## Running the Suite

```
# Maven
./mvnw test -Dtest='{Controller}IntegrationTest'

# Gradle
./gradlew test --tests '*{Controller}IntegrationTest'
```

If Surefire/Failsafe separation is in use, the suffix convention matters:
- `*Test` → Surefire (unit phase)
- `*IT` → Failsafe (integration phase, runs after package)

Check `pom.xml`'s `<build><plugins>` for Failsafe; if configured, name the file `{Controller}IT.java` instead.

---

## Rules specific to Spring Boot generation

- ALWAYS use `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `@AutoConfigureMockMvc` — this loads the full stack including filters and Security
- NEVER use `MockMvcBuilders.standaloneSetup(controller)` — it bypasses the Security filter chain
- NEVER use `@WebMvcTest` for this skill — it does not load `@Service`/`@Repository` and defeats the purpose
- NEVER use `@DataJpaTest` — no MVC layer
- ALWAYS reuse a single Postgres container per JVM via `withReuse(true)`
- ALWAYS truncate tables in `@BeforeEach`, not drop/create — schema is managed by Flyway/Liquibase
- ALWAYS inject JWT via `.with(jwt())` post-processor, never hand-craft tokens
- NEVER use `@MockBean` for the controller's own service/repository dependencies
- `@MockBean` is allowed ONLY for external system clients that cannot be reached (e.g., a payment gateway without a sandbox); in that case prefer WireMock first
- If the controller depends on `Clock` or `Instant.now()`, inject a fixed clock via `@TestConfiguration` and assert timestamps
