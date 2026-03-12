# Gestion de Contexto - Referencia Completa

## Higiene Pre-Compactacion

Antes de compactar:
1. Deshabilitar MCP servers no usados (consumen 8-30% del contexto)
2. Verificar que no hay trabajo pendiente que necesite contexto completo
3. Si hay estado complejo: escribirlo en NOTES.md antes de compactar

## Continuidad Multi-Sesion

Antes de terminar una sesion larga, crear resumen con:
- Estado actual del trabajo (hecho, en progreso, pendiente)
- Blockers y preguntas abiertas
- Archivos tocados y por que
- Errores encontrados y enfoques descartados
- Proximos pasos recomendados

En nueva sesion: leer el resumen primero.

## Scratchpad Externo (NOTES.md)

Para estado que necesita sobrevivir compactacion y resets de sesion:
- Escribir en un archivo NOTES.md temporal en el proyecto
- Incluir: plan actual, decisiones tomadas, estado de progreso
- Patron: escribir antes de compactar, leer al iniciar nueva sesion
- Eliminar cuando la tarea se complete

## Deteccion de Degradacion

Cada ~10 acciones en tarea larga:
- Re-leer el objetivo original
- Verificar que aun se entiende que se esta haciendo y por que
- Si no se puede reconstruir el intent original: STOP y preguntar

### 5 Patrones de Degradacion

| Patron | Senal | Recuperacion |
|--------|-------|-------------|
| **Lost-in-Middle** | Olvida informacion del medio de la conversacion | /compact o mover info critica al final |
| **Context Poisoning** | Repite errores ya corregidos | Marcar correccion explicita, /clear si persiste |
| **Context Distraction** | Se distrae con info irrelevante cargada | Eliminar fuentes irrelevantes, filtrar agresivamente |
| **Context Confusion** | Mezcla requisitos de tareas diferentes | Separar en sesiones/subagentes por tarea |
| **Context Clash** | Info contradictoria acumulada causa razonamiento erratico | Resolver contradiccion explicitamente, priorizar lo reciente |

### Senales de accion inmediata

- Outputs descuidados o genericos -> /compact
- Repetir trabajo ya hecho -> /compact o nueva sesion
- Olvidar restricciones mencionadas antes -> nueva sesion
- Tool parameters incorrectos -> nueva sesion inmediata
- Hallucinations persistentes -> /clear (hard reset)
