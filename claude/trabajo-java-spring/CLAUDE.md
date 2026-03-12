# Orquestacion del Flujo de Trabajo

## 1. Modo Plan por Defecto

Modo plan para tareas no triviales (3+ pasos o decisiones de arquitectura).
Si algo se tuerce, PARA y replanifica. OBLIGATORIO: seguir `rules/plan-workflow.md`. Architect ANTES del plan.

## 2. Subagentes

Roster: architect, developer, tester, security-auditor. Detalle en `rules/subagent-config.md`.
Una tarea por subagente. Para problemas complejos, mas computo via subagentes.

## 3. Ciclo de Auto-Mejora

Tras correccion del usuario: crear/actualizar rule en `/home/local/.claude/rules/` (global) o `.claude/rules/` (proyecto). Preguntar donde.

## 4. Verificacion Antes de Terminar

Nunca marcar completo sin probar. Build, tests, logs, diff vs main.
Preguntate: "Aprobaria esto un Staff Engineer?"

## 5. Principios y Estandares de Codigo

- SOLID. Interfaces limpias. Si crece x10, no reescribir todo.
- Tipado estricto. Nombres semanticos. Modularidad/DRY.
- Manejo de errores gracil. Loguear contexto para depurar.
- Simplicidad primero. Causa raiz, no parches. Impacto minimo.
- Si se siente "hacky": implementar la solucion elegante.
- Desafia tu propio trabajo: busca acoplamiento innecesario.

## 6. Bugs

Arreglar autonomamente. Logs, errores, tests fallidos -> resolver. Cero hand-holding.

## 7. Comunicacion

SIEMPRE preguntar si ambiguo. Preguntar 3 veces > implementar mal 1 vez.
Anti-servilismo: senalar problemas con alternativa concreta. Aceptar si te anulan.
Declarar suposiciones ANTES de implementar. Si feedback negativo: PARAR y preguntar solucion elegante.

## 8. Setup Inicial del Proyecto

Referencia: `/home/local/.claude/hooks/references/init-setup.md`

## 9. Archivos de Configuracion del Proyecto

El archivo de configuracion local es **`openpay-onboarding-partnerintegrator-srv-local_1.yml`** (con sufijo `_1`).
NUNCA usar el archivo sin `_1`.
