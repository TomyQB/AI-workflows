# Gestion de Contexto

El context window es memoria finita. Degradar = acumular errores.
Un 2% de desalineacion temprana puede crear 40% de tasa de fallos al final.

## Umbrales de Compactacion

| Uso del contexto | Accion |
|------------------|--------|
| 70-75% | `/compact` proactivo (RECOMENDADO) |
| 80% | Considerar nueva sesion para trabajo multi-archivo |
| 95% | Auto-compact se dispara (TARDE — puede perder contexto critico) |

NUNCA esperar al auto-compact. Compactar proactivamente deja buffer para operaciones finales.

## Instrucciones de Compactacion

Cuando compactes, SIEMPRE preservar:
- Lista completa de archivos modificados
- Comandos de test ejecutados y sus resultados
- Decisiones arquitectonicas tomadas en la sesion
- Estado actual de la tarea y proximos pasos
- Errores encontrados y como se resolvieron

Para compactacion dirigida: `/compact Focus on [area especifica]`

## Higiene Pre-Compactacion

Antes de compactar:
1. Deshabilitar MCP servers no usados (consumen 8-30% del contexto)
2. Verificar que no hay trabajo pendiente que necesite contexto completo
3. Si hay estado complejo: escribirlo en NOTES.md antes de compactar

## Sesiones

- Sesiones nuevas para temas diferentes (contexto fresco > contexto largo)
- `/clear` cuando el tema cambio completamente
- Un objetivo claro por sesion. Si el objetivo cambia: nueva sesion.

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

- Outputs descuidados o genericos → /compact
- Repetir trabajo ya hecho → /compact o nueva sesion
- Olvidar restricciones mencionadas antes → nueva sesion
- Tool parameters incorrectos → nueva sesion inmediata
- Hallucinations persistentes → /clear (hard reset)

## Subagentes

- Delegar investigacion y analisis paralelo a subagentes
- Mantener el contexto principal limpio
- Una tarea enfocada por subagente
- Subagentes tienen su propio context window de 200k — aprovecharlo
- Instrucciones de delegacion CLARAS: scope, archivos relevantes, criterio de exito
- Delegacion vaga = fallo del subagente. "Fix auth" es malo. "Fix OAuth redirect loop in src/lib/auth.ts" es bueno.