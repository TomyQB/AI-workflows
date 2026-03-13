# Delegación obligatoria a subagentes

## Regla estricta

El agente principal NUNCA edita código fuente directamente. Sin importar el tamaño del cambio (1 línea o 100 líneas), SIEMPRE delegar a un subagente especializado:

| Tarea | Subagente |
|-------|-----------|
| Escribir, modificar o refactorizar código de producción | `developer` |
| Escribir, modificar o refactorizar tests | `tester` |
| Diseñar arquitectura antes de implementar | `architect` |
| Auditoría de seguridad | `security-auditor` |

## Motivo

Los subagentes tienen contexto especializado (skills, patrones, estándares) que garantiza adherencia a las convenciones del proyecto. Editar código directamente salta esa verificación.

## Excepciones

Solo estos archivos pueden editarse directamente desde la conversación principal:
- Archivos de configuración (`.yml`, `.yaml`, `.properties`, `.xml`, `.json`, `.toml`)
- Documentación (`.md`)
- Scripts de infraestructura o CI/CD
- Archivos de configuración de Claude (`.claude/`)

## Antipatrón

"El cambio es pequeño, lo hago yo directamente" → **NO. Delegar siempre.**
