---
name: java-spring-boot
description: >
  Java and Spring Boot coding standards, conventions, and architectural patterns.
  Use when writing, reviewing, or refactoring Java code in Spring Boot projects.
  Triggers: (1) Creating new Java classes, services, controllers, DTOs, or entities,
  (2) Writing or modifying Spring Boot application code, (3) Reviewing Java code for
  best practices, (4) Implementing REST APIs with Spring Boot, (5) Any task involving
  .java files in a Spring Boot project.
---

# Java Spring Boot Standards

## Core Principles

1. **Inmutabilidad total**: `final` en todo parametro y variable. Records para DTOs. Colecciones inmutables (`List.of()`, `.toList()`)
2. **Programacion funcional**: Streams sobre loops. Optional sobre null. Method references sobre lambdas. Usar SIEMPRE utilidades de `Objects` y `StringUtils` para comparaciones (ver seccion "Comparaciones Seguras")
3. **Metodos pequenos**: Max 10 lineas de logica. Max 3 parametros. Nombre que exprese intencion de negocio
4. **Separacion de responsabilidades**: Controller (HTTP) -> Service (orquestacion) -> Validator + Mapper + Repository

## Comparaciones Seguras

NUNCA usar comparaciones directas. Usar SIEMPRE las utilidades de `java.util.Objects` y `org.apache.commons.lang3.StringUtils` (o `org.springframework.util.StringUtils` segun el proyecto).

### `Objects` (`java.util.Objects`)

| En lugar de | Usar | Por que |
|-------------|------|--------|
| `x != null` | `Objects.nonNull(x)` | Null-safe, expresivo, compatible con predicados en Streams |
| `x == null` | `Objects.isNull(x)` | Consistencia, compatible con `filter(Objects::isNull)` |
| `a.equals(b)` | `Objects.equals(a, b)` | Null-safe: si `a` es null no lanza NPE |
| `a.hashCode()` | `Objects.hashCode(a)` | Null-safe: retorna 0 si es null |
| `Objects.hash(a, b, c)` | Para `hashCode()` compuesto | Combina multiples campos de forma limpia |
| `if (x == null) throw ...` | `Objects.requireNonNull(x, "mensaje")` | Fail-fast con mensaje claro |
| `x != null ? x : default` | `Objects.requireNonNullElse(x, default)` | Mas expresivo que ternario |
| `x.toString()` | `Objects.toString(x, "default")` | Null-safe con valor por defecto |

### `StringUtils` (Apache Commons / Spring) - Solo metodos NO deprecados

| En lugar de | Usar | Por que |
|-------------|------|--------|
| `s == null \|\| s.isEmpty()` | `StringUtils.isEmpty(s)` | Null-safe, una sola llamada |
| `s != null && !s.isEmpty()` | `StringUtils.isNotEmpty(s)` | Null-safe, legible |
| `s == null \|\| s.trim().isEmpty()` | `StringUtils.isBlank(s)` | Null-safe, incluye whitespace |
| `s != null && !s.trim().isEmpty()` | `StringUtils.isNotBlank(s)` | Null-safe, el mas comun para validar strings |
| `s != null ? s.trim() : null` | `StringUtils.trimToNull(s)` | Null-safe, retorna null si queda vacio |
| `s != null ? s.trim() : ""` | `StringUtils.trimToEmpty(s)` | Null-safe, retorna "" si queda vacio |
| `s != null ? s : ""` | `StringUtils.defaultString(s)` | Null-safe, retorna "" si es null |
| `String.format(...)` | `String.formatted(...)` | Java moderna, mas conciso |

### Metodos de StringUtils DEPRECADOS - NO USAR

Los siguientes metodos de `StringUtils` estan deprecados. Usar siempre la alternativa indicada:

| Deprecado (NO usar) | Alternativa | Por que |
|---------------------|-------------|--------|
| `StringUtils.equals(s1, s2)` | `Objects.equals(s1, s2)` | Deprecado. `Objects.equals` es null-safe y estandar de Java |
| `StringUtils.equalsIgnoreCase(s1, s2)` | Verificar null + `s1.equalsIgnoreCase(s2)` | Deprecado |
| `StringUtils.contains(s, sub)` | Verificar null + `s.contains(sub)` | Deprecado |
| `StringUtils.startsWith(s, prefix)` | Verificar null + `s.startsWith(prefix)` | Deprecado |
| `StringUtils.removeStart(s, remove)` | `s.startsWith(remove) ? s.substring(remove.length()) : s` | Deprecado. Usar API nativa de String |
| `StringUtils.removeEnd(s, remove)` | `s.endsWith(remove) ? s.substring(0, s.length() - remove.length()) : s` | Deprecado. Usar API nativa de String |
| `StringUtils.replace(s, old, new)` | `s.replace(old, new)` | Deprecado. `String.replace` ya no lanza NPE en Java 21 |
| `StringUtils.upperCase(s)` / `lowerCase(s)` | `s.toUpperCase()` / `s.toLowerCase()` | Deprecado |

### Regla general

> **Prioridad**: `Objects` (java.util) > API nativa de String (Java 21) > `StringUtils` (Apache Commons).
> Usar `StringUtils` SOLO para operaciones que NO tienen equivalente nativo null-safe (`isBlank`, `isNotBlank`, `isEmpty`, `isNotEmpty`, `trimToNull`, `trimToEmpty`, `defaultString`, `leftPad`, `rightPad`).
> Para comparaciones de igualdad, SIEMPRE usar `Objects.equals()`.

## Prevencion de APIs Deprecadas

### Regla estricta

Antes de usar CUALQUIER metodo de una libreria externa (Apache Commons, Guava, Spring Utils, etc.), verificar que NO este deprecado en la version que usa el proyecto. Si existe una alternativa en la API estandar de Java (java.util, java.lang, java.time, etc.), preferir SIEMPRE la version estandar.

### Patron de verificacion

1. Si el IDE o el build reporta un warning de deprecacion: corregirlo INMEDIATAMENTE
2. Preferir API estandar de Java sobre librerias externas cuando la funcionalidad es equivalente
3. Para Java 21+: muchos metodos de utilidad de librerias externas ya tienen equivalente nativo
4. Regla de oro: si `java.util.Objects`, `java.lang.String`, `java.util.Optional` o `java.util.stream` ofrecen la operacion, NO usar la version de Apache Commons/Guava/Spring

## Java Modern Features

Aplicar siempre que sea posible:

| Feature | Uso |
|---------|-----|
| `record` | DTOs (request/response), value objects, configuration properties |
| `sealed` | Jerarquias cerradas de tipos con `permits` |
| `switch` expression | Pattern matching con `->`, sin `default` en sealed types |
| Virtual Threads | I/O-bound async con `Executors.newVirtualThreadPerTaskExecutor()` |
| `String.formatted()` | Interpolacion de strings |
| `var` | Inferencia de tipos (siempre con `final`) |

## Architecture Quick Reference

```
Controller  (@RestController, @RequiredArgsConstructor)
  -> solo recibir request + delegar + retornar response
  -> @Valid en @RequestBody

Service     (@Service, @RequiredArgsConstructor)
  -> @Transactional / @Transactional(readOnly = true)
  -> orquestar validator + mapper + repository

Validator   (@Component)
  -> reglas de negocio, lanza excepciones custom
  -> Objects.requireNonNull() para precondiciones

Mapper      (@Component)
  -> toEntity() / toResponse() / toResponseList()
  -> sin logica de negocio

Repository  (extends JpaRepository)
  -> derived queries + @Query JPQL
  -> Optional para ID unico, List para multiples
  -> Resolver Optional internamente: orElseThrow() en el Repository, NO en el Service
  -> El Service recibe tipos concretos, nunca Optional

Exception   (@RestControllerAdvice)
  -> excepciones custom extends RuntimeException
  -> GlobalExceptionHandler mapea exception -> HTTP status
  -> ErrorResponse como record
```

## Lombok Usage

| Contexto | Anotaciones |
|----------|-------------|
| Entidades JPA | `@Getter @Builder @NoArgsConstructor(access=PROTECTED) @AllArgsConstructor(access=PRIVATE)` |
| DTOs (record) | `@Builder` |
| Services/Components | `@RequiredArgsConstructor` |
| Logging | `@Slf4j` |
| **Prohibido** | `@Setter`, `@Data`, `@ToString` en entidades |

## DTOs con Jakarta Validation

```java
@Builder
public record CreateRequest(
    @NotBlank(message = "Name is required") String name,
    @NotNull @Positive BigDecimal amount,
    @Size(max = 140) String description
) {}
```

## Detailed References

- **Modern Java patterns** (Records, Sealed Classes, Pattern Matching, Inmutabilidad, Funcional, Virtual Threads): see [references/patterns.md](references/patterns.md)
- **Spring Boot architecture** (Controller, Service, Validator, Mapper, Repository, Exceptions, Entities, Config): see [references/architecture.md](references/architecture.md)
