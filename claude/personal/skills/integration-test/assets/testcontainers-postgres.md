# Assets — Testcontainers Postgres for Spring Boot

> Copy-paste snippets. Adjust package names and paths to the target project.

---

## Maven dependencies (add to `pom.xml`)

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

<dependencies>
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
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>
</dependencies>
```

## Gradle dependencies (add to `build.gradle.kts`)

```kotlin
dependencies {
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    runtimeOnly("org.postgresql:postgresql")
}
dependencyManagement {
    imports { mavenBom("org.testcontainers:testcontainers-bom:1.20.4") }
}
```

---

## Enable container reuse (one-time, per-developer)

Append to `~/.testcontainers.properties`:

```
testcontainers.reuse.enable=true
```

Effect: the Postgres container is created once and reused across JVM runs. Saves ~30s per test session.

On CI, skip this — ephemeral containers per run are cleaner.

---

## IntegrationTestBase.java

Place at `src/test/java/{base-pkg}/integration/IntegrationTestBase.java`:

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

import {base-pkg}.integration.support.TestDbCleaner;

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

---

## TestDbCleaner.java

Place at `src/test/java/{base-pkg}/integration/support/TestDbCleaner.java`:

```java
package {base-pkg}.integration.support;

import org.springframework.jdbc.core.JdbcTemplate;
import java.util.List;

public final class TestDbCleaner {
    private TestDbCleaner() {}

    /**
     * Truncates all tables in the `public` schema except migration bookkeeping tables.
     * Uses RESTART IDENTITY so generated ids start fresh each test.
     * Uses CASCADE to bypass FK constraint ordering issues.
     */
    public static void truncateAll(JdbcTemplate jdbc) {
        List<String> tables = jdbc.queryForList(
            "SELECT tablename FROM pg_tables " +
            "WHERE schemaname = 'public' " +
            "AND tablename NOT IN ('flyway_schema_history', 'databasechangelog', 'databasechangeloglock')",
            String.class);
        if (tables.isEmpty()) return;
        jdbc.execute("TRUNCATE TABLE " + String.join(", ", tables) + " RESTART IDENTITY CASCADE");
    }
}
```

---

## `application-integration.yml`

Place at `src/test/resources/application-integration.yml`:

```yaml
spring:
  datasource:
    # URL / username / password injected by @DynamicPropertySource
    hikari:
      maximum-pool-size: 5
      connection-timeout: 5000
  jpa:
    hibernate:
      ddl-auto: validate        # migrations are authoritative — never create-drop in tests
    open-in-view: false
    properties:
      hibernate.jdbc.time_zone: UTC
      hibernate.show_sql: false
  flyway:
    enabled: true
    baseline-on-migrate: true

  # OPTIONAL — remove if the project does not use OAuth2 resource server
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:9999   # dummy, overridden by jwt() post-processor

logging:
  level:
    org.springframework.web: INFO
    org.springframework.security: INFO
    org.hibernate.SQL: WARN
    org.testcontainers: INFO
    tc.postgres: WARN
```

Tweaks:
- If the project uses Liquibase, replace `flyway` config with `liquibase.enabled: true`.
- If the project uses Hibernate-managed schema (no migrations), change `ddl-auto` to `create-drop` — acceptable for integration but fragile.

---

## Schema management

The skill does NOT generate or run migrations. It relies on the project's existing Flyway / Liquibase setup to apply the schema when Spring Boot starts during `@SpringBootTest`.

If the project has no migrations:
1. Flag it as a risk in the final report.
2. Suggest introducing Flyway with a `V1__init.sql` baseline.
3. As a temporary workaround, set `spring.jpa.hibernate.ddl-auto: create-drop` in `application-integration.yml` — but mark this as debt.

---

## Seeding data for specific scenarios

### Option A — inline `JdbcTemplate` (preferred for 1–3 rows)

```java
jdbc.update("INSERT INTO users (email, password_hash, name) VALUES (?, ?, ?)",
    "alice@test.com", "$2a$10$seeded.bcrypt.hash.xxx", "Alice");
```

### Option B — `@Sql` annotation (multi-row fixtures)

Place SQL files in `src/test/resources/db/fixtures/`:

```sql
-- src/test/resources/db/fixtures/ten-users.sql
INSERT INTO users (email, password_hash, name) VALUES
  ('u1@test.com', '$2a$10$hash1', 'User 1'),
  ('u2@test.com', '$2a$10$hash2', 'User 2'),
  -- ...
  ('u10@test.com', '$2a$10$hash10', 'User 10');
```

Use via:

```java
@Test
@Sql("/db/fixtures/ten-users.sql")
@DisplayName("paginates users 3 per page")
void paginatesUsers() throws Exception { ... }
```

### Option C — test data builder (for domain-heavy setups)

Build a fluent builder in `integration/support/UserTestBuilder.java` that produces valid entities with sensible defaults and `.with{Field}(value)` chain methods. Persist via `TestEntityManager` or `JdbcTemplate`.

---

## Troubleshooting

| Symptom | Cause | Fix |
|---------|-------|-----|
| `Could not find a valid Docker environment` | Docker not running | Start Docker, re-run |
| First test run takes 30–60s | Postgres image pull | Expected; subsequent runs are fast |
| Tests pass locally, fail on CI | `testcontainers.reuse.enable=true` locally, missing properties on CI | Remove reuse on CI; accept per-run startup |
| `unable to start container: ...` | Docker daemon misconfigured (Colima, Rancher, WSL) | Set `DOCKER_HOST` env var correctly |
| Schema is empty when test runs | Flyway not enabled for test profile | Check `spring.flyway.enabled: true` in `application-integration.yml` |
| Test leaks data to next test | Truncate not running or transaction not rolling back | Confirm `IntegrationTestBase.resetDatabase()` runs; avoid `@Transactional` on the test class (it suppresses real DB state) |
| `ERROR: relation "X" does not exist` | Truncate happens before migrations on first container start | Ensure migrations run automatically via Spring Boot; if they depend on `@ContextConfiguration` trickery, rework |
