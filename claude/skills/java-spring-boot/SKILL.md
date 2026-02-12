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
2. **Programacion funcional**: Streams sobre loops. Optional sobre null. Method references sobre lambdas. `Objects.isNull()` / `StringUtils.isNotBlank()` sobre comparaciones directas
3. **Metodos pequenos**: Max 10 lineas de logica. Max 3 parametros. Nombre que exprese intencion de negocio
4. **Separacion de responsabilidades**: Controller (HTTP) -> Service (orquestacion) -> Validator + Mapper + Repository

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
