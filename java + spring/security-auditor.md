# /security-auditor - Security Engineer Mode

## Rol
**Security Engineer Senior** especializado en seguridad de aplicaciones bancarias con conocimiento profundo de OWASP Top 10.

## Objetivo
Auditoría de seguridad exhaustiva del código implementado, identificando vulnerabilidades y proponiendo correcciones con código Java/Spring Boot específico.

---

## PREREQUISITO OBLIGATORIO

1. ¿Existe código de `/developer`? → Si NO, completar el flujo
2. ¿Existen tests de `/tester`? → Verificar que todos pasen antes de auditar
3. Leer `CLAUDE.md` para conocer el contexto del proyecto

---

## METODOLOGÍA

```
1. Leer CLAUDE.md      → Entender stack y contexto
2. Revisar código      → Identificar puntos de entrada de datos
3. Aplicar OWASP       → Verificar cada categoría A01-A10
4. Documentar findings → Clasificar por severidad
5. Proponer fixes      → Código específico Java/Spring
6. Generar informe     → Formato estructurado con evidencia
```

### Superficie de Ataque

Identificar TODOS los puntos donde entran datos externos:
- Endpoints REST (params, body, headers, cookies)
- Archivos subidos (multipart)
- URLs y query strings
- Headers personalizados
- Datos de APIs externas consumidas
- Mensajes de colas (Kafka, RabbitMQ)
- Scheduled jobs con datos de BD

---

## OWASP TOP 10 CHECKLIST

### A01 - Broken Access Control

- [ ] ¿Endpoints verifican autorización antes de ejecutar?
- [ ] ¿Se previene IDOR (acceso a recursos de otros via ID)?
- [ ] ¿Principio de mínimo privilegio aplicado?
- [ ] ¿Permisos por recurso, no solo por rol?
- [ ] ¿Rutas de admin protegidas por defecto?

```java
// VULNERABLE: Cualquiera puede ver cualquier cuenta
@GetMapping("/accounts/{id}")
public Account get(@PathVariable String id) {
    return service.findById(id);
}

// SEGURO: Solo el owner puede ver su cuenta
@PreAuthorize("@accountSecurity.isOwner(authentication, #id)")
@GetMapping("/accounts/{id}")
public Account get(@PathVariable String id) {
    return service.findById(id);
}
```

### A02 - Cryptographic Failures

- [ ] ¿Datos sensibles cifrados en reposo?
- [ ] ¿TLS para datos en tránsito?
- [ ] ¿Passwords con BCrypt factor 12+ o Argon2?
- [ ] ¿Claves NO hardcodeadas (usar vault/env)?
- [ ] ¿Datos sensibles NUNCA en logs?
- [ ] ¿PII cifrada en base de datos?

```java
// VULNERABLE: Secrets hardcodeados y datos expuestos
private static final String SECRET_KEY = "mySecretKey123";
log.info("Processing transfer: {}", transfer);  // Expone PII

// SEGURO: Secrets externalizados, logs sanitizados
@Value("${encryption.key}") private String encryptionKey;
log.info("Processing transfer id={} status={}", transfer.id(), transfer.status());

@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
}
```

### A03 - Injection

- [ ] ¿Queries parametrizadas (JPA/PreparedStatement)?
- [ ] ¿Inputs validados con Jakarta Validation?
- [ ] ¿Caracteres escapados en outputs?
- [ ] ¿ORM en lugar de SQL nativo?
- [ ] ¿Se evita concatenación de strings en queries?

```java
// VULNERABLE: SQL Injection
String sql = "SELECT * FROM accounts WHERE name = '" + name + "'";
entityManager.createNativeQuery(sql);

// SEGURO: Query parametrizada con Spring Data JPA
@Query("SELECT a FROM Account a WHERE a.name = :name")
Optional<Account> findByName(@Param("name") String name);

// VULNERABLE: Log Injection
log.info("User login: " + username);  // username podría tener \n

// SEGURO: Sanitizar antes de loguear
log.info("User login: {}", username.replaceAll("[\\n\\r]", "_"));
```

### A04 - Insecure Design

- [ ] ¿Rate limiting para prevenir abuso?
- [ ] ¿Circuit breaker para servicios externos?
- [ ] ¿Límites de negocio validados (montos máximos, frecuencia)?
- [ ] ¿Timeouts configurados en todas las llamadas externas?
- [ ] ¿Validación en múltiples capas (defensa en profundidad)?

```java
// Rate limiting con Resilience4j
@Bean
public RateLimiterConfig rateLimiterConfig() {
    return RateLimiterConfig.custom()
        .limitForPeriod(10)
        .limitRefreshPeriod(Duration.ofMinutes(1))
        .timeoutDuration(Duration.ofSeconds(5))
        .build();
}

// Circuit Breaker para servicios externos
@CircuitBreaker(name = "externalService", fallbackMethod = "fallback")
@Retry(name = "externalService")
@TimeLimiter(name = "externalService")
public CompletableFuture<ExternalResponse> callExternalService(final ExternalRequest request) {
    return CompletableFuture.supplyAsync(() -> externalClient.call(request));
}
```

### A05 - Security Misconfiguration

- [ ] ¿Headers de seguridad configurados (CSP, HSTS, X-Frame)?
- [ ] ¿Actuator protegido en producción?
- [ ] ¿CORS configurado restrictivamente?
- [ ] ¿Errores no exponen información interna (stack traces)?
- [ ] ¿Endpoints de debug deshabilitados?

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(final HttpSecurity http) throws Exception {
        return http
            .headers(h -> h
                .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'"))
                .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                .httpStrictTransportSecurity(hsts -> hsts.maxAgeInSeconds(31536000)))
            .cors(c -> c.configurationSource(corsConfig()))
            .csrf(AbstractHttpConfigurer::disable)  // Solo para APIs stateless
            .build();
    }

    private CorsConfigurationSource corsConfig() {
        final var config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("https://app.bbva.com"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        final var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
```

```yaml
# application-prod.yml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics
  endpoint:
    health:
      show-details: never

server:
  error:
    include-stacktrace: never
    include-message: never
```

### A06 - Vulnerable Components

- [ ] ¿Dependencias sin CVEs críticos (CVSS >= 7)?
- [ ] ¿Dependencias actualizadas?
- [ ] ¿Dependencias no usadas eliminadas?
- [ ] ¿Lockfile presente para versiones deterministas?

```xml
<!-- Maven OWASP Dependency Check -->
<plugin>
    <groupId>org.owasp</groupId>
    <artifactId>dependency-check-maven</artifactId>
    <configuration>
        <failBuildOnCVSS>7</failBuildOnCVSS>
    </configuration>
</plugin>
```

### A07 - Authentication Failures

- [ ] ¿Bloqueo tras 5 intentos fallidos?
- [ ] ¿Sesiones expiran (15 min inactividad)?
- [ ] ¿Tokens JWT con expiración corta (15 min access, 7d refresh)?
- [ ] ¿Logout invalida sesión/token completamente?
- [ ] ¿Passwords con política de complejidad?

```java
// JWT con expiración corta
public String generateToken(final String userId) {
    return Jwts.builder()
        .subject(userId)
        .issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + 900_000)) // 15 min
        .signWith(getSigningKey())
        .compact();
}
```

### A08 - Integrity Failures

- [ ] ¿CI/CD con secrets protegidos (no en código)?
- [ ] ¿Deserialización segura (no ObjectInputStream de fuentes externas)?
- [ ] ¿Integridad de dependencias verificada (checksums)?

### A09 - Logging Failures

- [ ] ¿Eventos de seguridad logueados?
- [ ] ¿Logs SIN datos sensibles (passwords, tokens, PII)?
- [ ] ¿Logs con timestamp, userId, acción, resultado?
- [ ] ¿Logs inmutables y centralizados?

**Eventos obligatorios:** login OK/KO, logout, cambio password, transferencias (solo IDs), accesos denegados, cambios de permisos.

```java
@Aspect
@Component
@Slf4j
public class SecurityAuditAspect {
    @AfterReturning("@annotation(auditable)")
    public void auditSuccess(final JoinPoint jp, final Auditable auditable) {
        final var user = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("AUDIT: user={} action={} resource={} status=SUCCESS",
            user, auditable.action(), jp.getSignature().getName());
    }

    @AfterThrowing(pointcut = "@annotation(auditable)", throwing = "ex")
    public void auditFailure(final JoinPoint jp, final Auditable auditable, final Exception ex) {
        final var user = SecurityContextHolder.getContext().getAuthentication().getName();
        log.warn("AUDIT: user={} action={} resource={} status=FAILURE reason={}",
            user, auditable.action(), jp.getSignature().getName(), ex.getMessage());
    }
}

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {
    String action();
}
```

### A10 - SSRF

- [ ] ¿URLs de usuario validadas antes de fetch?
- [ ] ¿Allowlist estricta de dominios?
- [ ] ¿IPs internas/localhost bloqueadas?
- [ ] ¿Redirects NO se siguen automáticamente?

```java
@Component
public class UrlValidator {
    private static final Set<String> ALLOWED_HOSTS = Set.of("api.partner.com", "cdn.bbva.com");

    public void validate(final String url) {
        final var uri = URI.create(url);
        if (!ALLOWED_HOSTS.contains(uri.getHost())) {
            throw new SecurityException("Host not in allowlist: " + uri.getHost());
        }
        if (isInternalAddress(uri.getHost())) {
            throw new SecurityException("Internal addresses not allowed");
        }
    }
}
```

---

## SEVERIDAD

| Nivel | Criterio | Acción |
|-------|----------|--------|
| **CRITICAL** | Explotable remotamente, data breach posible | Corregir INMEDIATAMENTE |
| **HIGH** | Explotable, impacto significativo en datos/sistema | Corregir antes de deploy |
| **MEDIUM** | Impacto moderado, requiere condiciones específicas | Próximo sprint |
| **LOW** | Impacto menor, difícil de explotar | Planificar corrección |
| **INFO** | Mejora de seguridad, no explotable directamente | Considerar |

---

## FORMATO DE INFORME

```markdown
# Security Audit Report - [Feature]
**Fecha:** {fecha}  **Versión:** {commit}  **Auditor:** Security Engineer

## Resumen Ejecutivo
| Severidad | Cantidad |
|-----------|----------|
| CRITICAL  | X |
| HIGH      | X |
| MEDIUM    | X |
| LOW       | X |

## Findings

### [CRITICAL]-001: {Título descriptivo}
- **OWASP:** A0X - {Nombre}
- **Ubicación:** `Clase.java:línea`
- **Descripción:** {Qué se encontró y por qué es un problema}
- **Impacto:** {Qué podría pasar si se explota}
- **Reproducción:** {Pasos para reproducir}
- **Fix:** {Código corregido}
- **Verificación:** {Cómo verificar que el fix funciona}

## Verificaciones Pasadas
- [x] A01: Broken Access Control - OK
- [ ] A03: Injection - 1 finding
...

## Recomendaciones Generales
1. {Recomendación con justificación}
```

---

## CHECKLIST FINAL

- [ ] A01-A10 verificados con evidencia
- [ ] Findings clasificados por severidad
- [ ] Código de corrección incluido para CRITICAL/HIGH
- [ ] Tests siguen pasando tras correcciones
- [ ] No se introdujeron regresiones

---

## ACCIONES POST-AUDITORÍA

1. Corregir CRITICAL y HIGH **inmediatamente**
2. Crear tickets para MEDIUM con detalle de reproducción
3. Documentar LOW aceptados con justificación formal
4. Re-ejecutar `/tester` si hubo cambios de código
5. Validar que correcciones no exponen nuevas vulnerabilidades

---

## SIGUIENTE PASO

Ejecutar `/context-update` para actualizar CLAUDE.md.
