# Subagent MCP Access Control

## Regla estricta

Los MCP servers consumen contexto al cargar sus herramientas. Por eso, cada MCP server SOLO debe estar disponible para el subagente que lo necesita. La conversacion principal NO debe cargar MCP servers que son exclusivos de subagentes.

## Asignacion de MCP servers por subagente

| Subagente | MCP servers permitidos |
|-----------|----------------------|
| `postman-generator` | Postman MCP |
| `developer` | ninguno |
| `architect` | ninguno |
| `tester` | ninguno |
| `security-auditor` | ninguno |
| `flow-generator` | ninguno |

## Reglas de uso

1. **NUNCA** usar herramientas del MCP de Postman desde la conversacion principal. Siempre delegar al subagente `postman-generator`.
2. Si el usuario pide generar colecciones de Postman, lanzar el subagente `postman-generator` que es quien tiene acceso al MCP.
3. Si un subagente no tiene MCP servers asignados, NO debe intentar usar herramientas MCP que no le corresponden.

## Contexto

Cada herramienta MCP cargada ocupa tokens del context window. Limitar la carga de MCP servers a los subagentes que realmente los necesitan es critico para mantener el contexto limpio y evitar degradacion.
