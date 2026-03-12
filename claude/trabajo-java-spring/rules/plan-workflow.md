# Flujo de Planificacion

## Flujo secuencial (tareas no triviales)

1. **Entender contexto**: Leer archivos relevantes. Si hay `.claude/flows/_index.md`, leer indice del dominio afectado. Si ambiguo: PREGUNTAR.
2. **Invocar architect** (OBLIGATORIO): Lanzar subagente con requisitos, contexto de codigo, restricciones. NO presentar plan sin su respuesta.
3. **Construir plan**: Basado en salida del architect. Archivos a crear/modificar, orden, contratos, tests.
4. **Presentar al usuario**: Solo tras respuesta del architect. Usar ExitPlanMode.

## Exentos del architect

- Bug fixes obvios (1-2 lineas, sin cambio arquitectura)
- Cambios de configuracion (.yml, .properties, .xml, .md)
- Tareas de lectura/exploracion/consulta
- Cambios cosmeticos (renombrar, reformatear)
