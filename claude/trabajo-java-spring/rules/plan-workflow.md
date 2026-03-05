# Flujo de Planificación Obligatorio

## Regla estricta

Toda tarea no trivial (3+ pasos, decisiones de arquitectura, nueva funcionalidad, cambios multi-archivo) DEBE seguir este flujo ANTES de presentar el plan al usuario. No se puede saltar ningún paso.

## Flujo secuencial

### Paso 1: Entender el contexto

- Leer los archivos relevantes del proyecto (controllers, services, entities, DTOs, configuración).
- Si el proyecto tiene `.claude/flows/_index.md`, leer el índice y los flujos del dominio afectado.
- Identificar qué se necesita: nueva funcionalidad, modificación, corrección, refactorización.
- Si los requisitos son ambiguos: PREGUNTAR al usuario antes de continuar.

### Paso 2: Invocar al subagente `architect` (OBLIGATORIO)

**SIEMPRE** lanzar el subagente `architect` mediante la herramienta Task con:
- Descripción clara de los requisitos funcionales.
- Contexto del código existente relevante (nombres de clases, estructura actual, patrones ya usados en el proyecto).
- Restricciones o decisiones ya tomadas por el usuario.

El `architect` devuelve:
- Arquitectura por capas con interfaces y contratos.
- Patrones de diseño aplicados.
- Flujos principales.

**NO presentar plan al usuario sin haber recibido la respuesta del architect.**

### Paso 3: Construir el plan basado en la arquitectura

Usar la salida del `architect` como base para construir el plan de implementación:
- Listar los archivos a crear o modificar.
- Definir el orden de implementación (qué va primero).
- Incluir los contratos/interfaces definidos por el architect.
- Incluir paso de tests y verificación.

### Paso 4: Presentar el plan al usuario

Solo después de tener la arquitectura del `architect`, presentar el plan completo para aprobación usando ExitPlanMode.

## Cuándo NO se requiere el architect

Estas situaciones están exentas del paso 2:

- Correcciones menores de bugs donde el cambio es obvio (1-2 líneas, sin cambio de arquitectura).
- Cambios de configuración (`.yml`, `.properties`, `.xml`).
- Tareas puramente de lectura, exploración o consulta.
- Cambios cosméticos (renombrar, reformatear) que no alteran la estructura.

## Antipatrones

- "Ya entiendo lo que hay que hacer, no necesito al architect" → **NO**. Si la tarea es no trivial, SIEMPRE lanzar el architect.
- "El cambio es parecido a algo que ya hicimos" → **NO**. El architect valida que se respeten SOLID y la arquitectura existente.
- Presentar plan al usuario y DESPUÉS lanzar el architect → **NO**. El architect va ANTES del plan.
