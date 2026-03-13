---
paths:
  - "**/*.java"
---

# Principios de Arquitectura y Acceso a Datos

Aplica a: `architect`, `developer`

## Filtrado en Base de Datos

Todo filtrado, ordenamiento o agregacion que pueda resolverse a nivel de base de datos DEBE hacerse ahi, NUNCA en codigo de aplicacion.

### Regla

- Las queries deben traer SOLO los datos que se necesitan, ya filtrados.
- Prohibido traer colecciones completas para luego filtrar en memoria con Streams, loops o condicionales.

### Ejemplo

**MAL** — filtrar en codigo:
```java
// Trae TODAS las personas y filtra en memoria
final var personas = personRepository.findAll();
final var mayoresDeEdad = personas.stream()
    .filter(p -> p.getAge() > 18)
    .toList();
```

**BIEN** — filtrar en base de datos:
```java
// La query solo trae lo que se necesita
final var mayoresDeEdad = personRepository.findByAgeGreaterThan(18);
```

### Aplica a

- Filtros por campo (`WHERE`)
- Ordenamiento (`ORDER BY`)
- Paginacion (`LIMIT`, `OFFSET`)
- Agregaciones (`COUNT`, `SUM`, `AVG`)
- Busquedas parciales (`LIKE`, `ILIKE`)
- Cualquier operacion que la base de datos pueda resolver de forma nativa
