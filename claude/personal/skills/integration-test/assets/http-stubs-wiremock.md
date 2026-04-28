# Assets — WireMock Stubs for External HTTP

> Copy-paste patterns. Adjust package names and URL paths to the target project.

---

## Maven dependency

```xml
<dependency>
    <groupId>org.wiremock</groupId>
    <artifactId>wiremock-standalone</artifactId>
    <version>3.9.2</version>
    <scope>test</scope>
</dependency>
```

## Gradle dependency

```kotlin
testImplementation("org.wiremock:wiremock-standalone:3.9.2")
```

Spring Boot 2.x users on Java 8+: use `com.github.tomakehurst:wiremock-jre8-standalone:2.35.2` instead.

---

## Registering WireMock in a test class (JUnit 5 extension)

```java
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

class UserControllerIntegrationTest extends IntegrationTestBase {

    @RegisterExtension
    static final WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void externalApisBaseUrl(DynamicPropertyRegistry registry) {
        // Point the app's external client at WireMock
        registry.add("emails.service.base-url", wireMock::baseUrl);
        // Repeat for each external client configured in application.yml
    }

    @BeforeEach
    void resetWireMock() { wireMock.resetAll(); }
}
```

The `@DynamicPropertySource` block is what wires WireMock into the Spring context. Without it the app would still hit the real external service.

---

## Stub patterns

### Successful JSON response

```java
import static com.github.tomakehurst.wiremock.client.WireMock.*;

wireMock.stubFor(post("/emails")
    .willReturn(okJson("""
        {"id":"msg-123","status":"queued"}
        """)));
```

### 4xx error response

```java
wireMock.stubFor(post("/payments")
    .willReturn(status(402)
        .withHeader("Content-Type", "application/json")
        .withBody("""
            {"error":"insufficient_funds","code":"E402"}
            """)));
```

### Timeout / delay

```java
wireMock.stubFor(get("/slow-api")
    .willReturn(ok().withFixedDelay(5_000))); // 5 seconds
```

Useful for testing circuit breakers or timeout handlers.

### Connection reset / network error

```java
wireMock.stubFor(get("/flaky-api")
    .willReturn(aResponse().withFault(com.github.tomakehurst.wiremock.http.Fault.CONNECTION_RESET_BY_PEER)));
```

### Match by request body

```java
wireMock.stubFor(post("/emails")
    .withHeader("Content-Type", equalTo("application/json"))
    .withRequestBody(matchingJsonPath("$.template", equalTo("welcome")))
    .willReturn(ok()));
```

### Different response based on request

```java
// Happy path matcher (specific)
wireMock.stubFor(post("/users")
    .withRequestBody(matchingJsonPath("$.email", equalTo("john@test.com")))
    .willReturn(okJson("""{"id":1}""")));

// Default fallback (generic)
wireMock.stubFor(post("/users")
    .willReturn(badRequest()));
```

WireMock evaluates stubs in reverse declaration order — register specific matchers LAST so they take precedence.

---

## Verification patterns

**Every stub that represents a side effect MUST be verified.** If the code no longer calls the external service, the test must fail.

### Exact count + URL

```java
import static com.github.tomakehurst.wiremock.client.WireMock.*;

wireMock.verify(exactly(1), postRequestedFor(urlEqualTo("/emails")));
```

### Count + URL + body content

```java
wireMock.verify(exactly(1),
    postRequestedFor(urlEqualTo("/emails"))
        .withRequestBody(matchingJsonPath("$.to", equalTo("john@test.com")))
        .withRequestBody(matchingJsonPath("$.subject", equalTo("Welcome, John")))
        .withRequestBody(matchingJsonPath("$.template", equalTo("welcome"))));
```

### Count + URL + headers

```java
wireMock.verify(exactly(1),
    postRequestedFor(urlEqualTo("/emails"))
        .withHeader("Authorization", matching("Bearer .+"))
        .withHeader("Idempotency-Key", matching("[a-f0-9-]{36}")));
```

### Negative verification — assert NOT called

```java
wireMock.verify(0, postRequestedFor(urlEqualTo("/emails")));
```

Use this in every error-path test and every validation-failure test. The spec's "welcome email on success" implies "no welcome email on failure".

### List all unmatched requests (debugging)

```java
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import java.util.List;

List<ServeEvent> allServeEvents = wireMock.getAllServeEvents();
allServeEvents.forEach(e -> System.out.println(e.getRequest()));
```

Use only when debugging a failing test — do NOT leave in the generated code.

---

## Common scenarios mapped to stubs

| Spec scenario | WireMock setup | Verification |
|---------------|----------------|--------------|
| "Welcome email sent on registration" | `stubFor(post("/emails").willReturn(ok()))` | `verify(exactly(1), postRequestedFor("/emails").withBody(...))` |
| "Payment charged via Stripe" | `stubFor(post("/v1/charges").willReturn(okJson(stripeChargeBody)))` | `verify(exactly(1), postRequestedFor("/v1/charges").withBody(amount+currency))` |
| "No email on validation failure" | (no stub) | `verify(0, postRequestedFor("/emails"))` |
| "Retries on 5xx, 3 attempts max" | `stubFor(get("/api").willReturn(serverError()))` | `verify(exactly(3), getRequestedFor("/api"))` |
| "Idempotent external call" | `stubFor(post("/api").withHeader("Idempotency-Key", matching(".+")).willReturn(ok()))` | Verify same Idempotency-Key header used on retry |

---

## Spring Boot config wiring

In `application.yml` (or `application-integration.yml`), ensure external client base URLs are externalized:

```yaml
emails:
  service:
    base-url: ${EMAILS_SERVICE_BASE_URL:https://emails.prod.example.com}
payments:
  service:
    base-url: ${PAYMENTS_SERVICE_BASE_URL:https://payments.prod.example.com}
```

The `@DynamicPropertySource` block in the test injects the WireMock URL over these.

If the project hardcodes URLs in `@FeignClient(url = "https://...")` or in `RestTemplateBuilder().rootUri("https://...")`, flag it as a testability issue in the final report and suggest extracting the URL to `@Value`/`@ConfigurationProperties`.

---

## Per-test-class vs per-suite WireMock

| Mode | Setup | Use when |
|------|-------|----------|
| Per-class (recommended) | `static @RegisterExtension` + `resetAll()` in `@BeforeEach` | Default — isolation between tests |
| Per-method | instance `@RegisterExtension` | Rarely — only when you need fresh port per test |
| Per-suite | singleton WireMock started in `SpringBootTest.ContextInitializer` | Only if Spring context is cached across classes AND a single shared stub pool is acceptable |

---

## Debugging stub mismatches

If a request arrives but no stub matches, WireMock returns 404 by default. To see why:

```java
wireMock.setScenarioState("my-scenario", "STARTED");

// Run the test, then print:
wireMock.getAllServeEvents().forEach(e ->
    System.out.println("URL: " + e.getRequest().getUrl()
        + " | Body: " + e.getRequest().getBodyAsString()
        + " | Matched: " + (e.getStubMapping() != null)));
```

Most common causes:
- URL case/slash mismatch (`/emails` vs `/emails/`)
- Missing Content-Type in the app's outgoing request
- Body path mismatch in `matchingJsonPath` (use [JSONPath evaluator](https://jsonpath.com/) to verify)
- Stubs registered after the request was made (in `@BeforeEach` vs in-test)

---

## Anti-patterns (do NOT generate tests that do these)

### ❌ Stub the service bean, skip WireMock

```java
@MockBean EmailService emailService;
verify(emailService).send(any());   // only proves the service was called, not that HTTP actually happened
```

This defeats the point of integration testing. Use WireMock.

### ❌ Verify only the URL, not the body

```java
wireMock.verify(postRequestedFor(urlEqualTo("/emails")));
// Allowed to pass even if the app posts an empty body, or the wrong template, or the wrong recipient
```

Always verify body matchers for POST/PUT/PATCH.

### ❌ Over-permissive stub then assert call count only

```java
wireMock.stubFor(any(anyUrl()).willReturn(ok()));   // matches everything
wireMock.verify(exactly(1), postRequestedFor(urlEqualTo("/emails")));
// Weak: the app could call /admin-delete-all and the stub would still return 200
```

Register stubs per real URL, not via `any(anyUrl())`.
