# Delegación obligatoria a subagentes

## Regla estricta

NUNCA editar archivos `.java` (ni de producción ni de test) directamente en la conversación principal. Sin importar el tamaño del cambio (1 línea o 50 líneas), SIEMPRE delegar:

| Tipo de código | Subagente |
|---------------|-----------|
| Código de producción (`.java` en `src/main`) | `developer` |
| Código de tests (`.java` en `src/test`) | `tester` |

## Motivo

Los subagentes leen sus skills asignados (`java-spring-boot`, `junit-mockito`) antes de escribir código, lo que asegura adherencia a los estándares del proyecto. Editar directamente salta esa verificación.

## Excepciones

- Archivos de configuración (`.yml`, `.properties`, `.xml`, `.md`) pueden editarse directamente.
- Archivos que no sean código Java pueden editarse directamente.

## Antipatrón

"El cambio es pequeño, lo hago yo directamente" → NO. Delegar siempre.
