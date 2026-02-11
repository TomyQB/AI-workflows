# Orquestación del Flujo de Trabajo

## 1. Modo Plan por Defecto

  Entra en modo plan para CUALQUIER tarea no trivial (3+ pasos o decisiones de arquitectura).

  Si algo se tuerce, PARA y replanifica inmediatamente - no sigas empujando.

  Usa el modo plan para pasos de verificación, no solo para construir.

  Escribe especificaciones detalladas de antemano para reducir la ambigüedad.

## 2. Estrategia de Subagentes

  Usa subagentes libremente para mantener limpio el contexto principal.

  Delega la investigación, exploración y análisis paralelo a subagentes.

  Para problemas complejos, métele más cómputo vía subagentes.

  Una tarea por subagente para una ejecución enfocada.

## 3. Ciclo de Auto-Mejora

  Después de CUALQUIER corrección del usuario: actualiza "tasks/lessons.md" con el patrón.

  Escríbete reglas a ti mismo para prevenir el mismo error.

  Itera despiadadamente sobre estas lecciones hasta que la tasa de errores baje.

  Revisa las lecciones al inicio de la sesión para el proyecto relevante.

## 4. Verificación Antes de Terminar

  Nunca marques una tarea como completa sin probar que funciona.

  Haz un diff del comportamiento entre main y tus cambios cuando sea relevante.

  Pregúntate: "¿Aprobaría esto un Staff Engineer?".

  Corre tests, revisa logs, demuestra que es correcto.

## 5. Exige Elegancia y Arquitectura (Equilibrada)

  Para cambios no triviales: haz una pausa y pregúntate "¿respeta esto los principios SOLID?".

  **Escalabilidad Inteligente**: Diseña interfaces limpias y desacopladas. Piensa: "¿Si este módulo crece x10 mañana, tendré que reescribirlo todo?" Usa patrones de diseño y principios SOLID.

  Si un arreglo se siente "hacky": "Sabiendo todo lo que sé ahora, implementa la solución elegante".

  Desafía tu propio trabajo: busca acoplamiento innecesario y elimínalo antes de presentar.

## 6. Arreglo Autónomo de Bugs

  Cuando te den un reporte de bug: simplemente arréglalo. No pidas que te lleven de la mano.

  Apunta a los logs, errores y tests que fallan - y luego resuélvelos.

  Cero cambio de contexto requerido por parte del usuario.

  Ve y arregla los tests que fallan sin que te digan cómo.

## 7. Estándares de Código Profesional (Seniority)

  **Tipado Estricto y Defensivo**: No uses `any` o tipos dinámicos si el lenguaje permite tipado fuerte. Valida los datos en los límites del sistema.

  **Nombres Semánticos**: Las variables y funciones deben explicar *por qué* existen, no solo *qué* hacen. Evita abreviaturas crípticas.

  **Modularidad y DRY**: Funciones pequeñas con una única responsabilidad. Si copias y pegas código, abstrae la lógica.

  **Manejo de Errores**: Nunca te comas las excepciones (swallow errors). Maneja los fallos de forma grácil y loguea el contexto necesario para depurar.

## 8. Principios Centrales

  **Simplicidad Primero**: Haz que cada cambio sea lo más simple posible, pero no simplista. Impacta el mínimo código.

  **Cero Vagancia**: Encuentra la causa raíz. Nada de arreglos temporales. Estándares de desarrollador Senior.

  **Mantenibilidad**: Escribe código para el humano que lo leerá en 6 meses. Documenta el "por qué" de las decisiones complejas, no el "qué".

  **Impacto Mínimo**: Los cambios solo deben tocar lo necesario. Evita introducir bugs por efectos secundarios (side-effects).
