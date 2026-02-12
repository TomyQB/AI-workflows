# Generacion de Flujos Mermaid

## Lectura Automatica al Inicio de Sesion

Al inicio de cada sesion en un proyecto que tenga `.claude/flows/_index.md`:

1. **Leer `.claude/flows/_index.md`** para cargar el contexto general (dominios, endpoints, relaciones).
2. **NO leer** los archivos de detalle de cada dominio automaticamente (costaria contexto innecesario).
3. **Cuando se trabaje en un dominio especifico**: leer el flujo funcional (`functional/{dominio}.md`) y tecnico (`technical/{dominio}.md`) de ese dominio antes de empezar a codificar.

## Sugerencia de Actualizacion

Al finalizar una tarea que modifique archivos de codigo fuente (controllers, services, repositories, entities):

- Sugerir al usuario: "Los flujos del dominio {X} pueden estar desactualizados. Quieres actualizarlos con `/user:generate-flows`?"
- Solo sugerir, nunca ejecutar automaticamente.
- Si el usuario acepta, ejecutar el command.
