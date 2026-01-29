# /resume-context - Context Summary Generator

## Objetivo
Generar un resumen estructurado del contexto actual de la conversación para:
1. **Continuar en la misma conversación** con contexto claro y evitar alucinaciones
2. **Transferir contexto a otra conversación** preservando decisiones y estado

---

## CUÁNDO USAR ESTE COMANDO

### Usar ANTES de:
- Conversación que se está volviendo larga (>50 mensajes)
- Cambiar de sesión o dispositivo
- Pausar el trabajo por tiempo prolongado
- Detectar respuestas inconsistentes o alucinaciones
- Necesitar compartir el contexto con otro desarrollador

### Usar DESPUÉS de:
- Completar una fase del workflow (/architect, /developer, /tester)
- Tomar decisiones arquitectónicas importantes
- Resolver problemas complejos
- Cambios significativos en el código

---

## FASE 1: ANÁLISIS DEL ESTADO ACTUAL

### 1.1 Identificar la Tarea Principal

**Extraer:**
- ¿Cuál fue la solicitud original del usuario?
- ¿Qué problema se está resolviendo?
- ¿Cuál es el objetivo final?
- ¿Hay subtareas o fases definidas?

### 1.2 Mapear el Progreso

**Determinar:**
- ¿En qué fase del workflow estamos? (init/architect/developer/tester/security)
- ¿Qué fases se han completado?
- ¿Qué queda pendiente?
- ¿Hay bloqueos o dependencias?

### 1.3 Recopilar Contexto Técnico

**Identificar:**
- Stack tecnológico del proyecto
- Patrones de diseño aplicados o decididos
- Convenciones de código establecidas
- Dependencias relevantes
- Estructura del proyecto

---

## FASE 2: INVENTARIO DE ARTEFACTOS

### 2.1 Archivos Creados

Listar todos los archivos nuevos con:
- Ruta completa
- Propósito/responsabilidad
- Estado (completo, parcial, pendiente)

```markdown
| Archivo | Propósito | Estado |
|---------|-----------|--------|
| src/services/UserService.ts | Lógica de usuarios | Completo |
| src/validators/UserValidator.ts | Validación de inputs | Pendiente |
```

### 2.2 Archivos Modificados

Listar archivos existentes que fueron cambiados:
- Ruta completa
- Qué se cambió y por qué
- Cambios pendientes si los hay

### 2.3 Archivos Leídos (Contexto)

Archivos que se leyeron para entender el proyecto:
- Archivos de configuración
- Código existente relacionado
- Documentación consultada

---

## FASE 3: DECISIONES Y ACUERDOS

### 3.1 Decisiones Arquitectónicas

Documentar cada decisión importante:

```markdown
### Decisión: [Título]
- **Contexto:** ¿Por qué surgió esta decisión?
- **Opciones consideradas:** ¿Qué alternativas había?
- **Decisión tomada:** ¿Qué se eligió?
- **Razón:** ¿Por qué se eligió esa opción?
- **Consecuencias:** ¿Qué implica esta decisión?
```

### 3.2 Convenciones Establecidas

- Naming conventions acordadas
- Patrones de código a seguir
- Estructura de carpetas definida
- Estilo de tests acordado

### 3.3 Restricciones Identificadas

- Limitaciones técnicas encontradas
- Requisitos no funcionales
- Dependencias externas
- Compatibilidad requerida

---

## FASE 4: ESTADO DE IMPLEMENTACIÓN

### 4.1 Lo que Funciona

- Features completamente implementadas
- Tests que pasan
- Integraciones verificadas

### 4.2 Lo que Está en Progreso

- Código parcialmente escrito
- Features incompletas
- Tests pendientes de escribir

### 4.3 Problemas Encontrados

- Errores conocidos y no resueltos
- Dudas técnicas pendientes
- Bloqueos esperando decisión

### 4.4 Próximos Pasos Inmediatos

Lista ordenada de las siguientes acciones:
1. [Acción inmediata 1]
2. [Acción inmediata 2]
3. [Acción inmediata 3]

---

## FASE 5: GENERACIÓN DEL RESUMEN

### Formato de Output

```markdown
# Resumen de Contexto - [Nombre del Proyecto/Feature]

**Fecha:** {fecha actual}
**Fase actual:** {fase del workflow}

## 1. Objetivo Principal
{Descripción clara de qué se está construyendo/resolviendo}

## 2. Stack Tecnológico
- **Lenguaje:** {lenguaje y versión}
- **Framework:** {framework y versión}
- **Testing:** {framework de tests}
- **Base de datos:** {si aplica}
- **Otras dependencias clave:** {lista}

## 3. Progreso del Workflow
- [x] /init - Completado
- [x] /architect - Completado
- [ ] /developer - EN PROGRESO (70%)
- [ ] /tester - Pendiente
- [ ] /security-auditor - Pendiente

## 4. Archivos del Proyecto

### Creados en esta sesión:
| Archivo | Propósito | Estado |
|---------|-----------|--------|
| {ruta} | {descripción} | {estado} |

### Modificados:
| Archivo | Cambios realizados |
|---------|-------------------|
| {ruta} | {descripción del cambio} |

### Contexto leído:
- {archivo1} - {por qué fue relevante}
- {archivo2} - {por qué fue relevante}

## 5. Decisiones Tomadas

### Decisión 1: {título}
- **Elegido:** {opción elegida}
- **Razón:** {justificación breve}
- **Alternativas descartadas:** {otras opciones}

### Decisión 2: {título}
...

## 6. Convenciones Establecidas
- {convención 1}
- {convención 2}
- {convención 3}

## 7. Estado Actual

### Completado:
- {item completado 1}
- {item completado 2}

### En progreso:
- {item en progreso 1} - {estado/porcentaje}

### Pendiente:
- {item pendiente 1}
- {item pendiente 2}

### Problemas/Bloqueos:
- {problema 1} - {contexto}

## 8. Próximos Pasos
1. {paso inmediato 1}
2. {paso inmediato 2}
3. {paso inmediato 3}

## 9. Notas Importantes
{Cualquier información crítica que no encaje en las secciones anteriores}

---
**Para continuar:** Pegar este resumen al inicio de una nueva conversación
o usarlo como referencia para retomar el trabajo.
```

---

## INSTRUCCIONES DE USO

### Para Continuar en la Misma Conversación

1. Ejecutar `/resume-context`
2. Revisar el resumen generado
3. Confirmar que el contexto es correcto
4. Continuar trabajando con contexto claro

### Para Transferir a Nueva Conversación

1. Ejecutar `/resume-context`
2. Copiar el resumen generado
3. En la nueva conversación, pegar el resumen como primer mensaje
4. Añadir: "Continúa desde este punto. El siguiente paso es: [descripción]"

### Para Compartir con Otro Desarrollador

1. Ejecutar `/resume-context`
2. Incluir sección adicional de "Contexto de Negocio" si es necesario
3. Compartir el resumen
4. El otro desarrollador inicia conversación con el resumen

---

## VALIDACIÓN DEL RESUMEN

### Checklist de Completitud

- [ ] ¿El objetivo principal está claro?
- [ ] ¿El stack tecnológico está documentado?
- [ ] ¿Todas las decisiones importantes están capturadas?
- [ ] ¿Los archivos creados/modificados están listados?
- [ ] ¿El estado actual es preciso?
- [ ] ¿Los próximos pasos son accionables?
- [ ] ¿Hay suficiente contexto para evitar alucinaciones?

### Señales de Buen Resumen

- Otro desarrollador podría continuar el trabajo
- No hay ambigüedad en las decisiones tomadas
- Los archivos referenciados existen y son correctos
- El progreso es verificable

### Señales de Resumen Incompleto

- Faltan decisiones importantes
- Referencias vagas a "el archivo" sin ruta
- Estado de progreso no claro
- Próximos pasos ambiguos

---

## ANTI-PATRONES

**EVITAR:**
- Resúmenes demasiado largos (máximo 2 páginas)
- Incluir código completo (solo referencias)
- Repetir información del CLAUDE.md del proyecto
- Información irrelevante para continuar el trabajo
- Suposiciones no verificadas

**PREFERIR:**
- Información concisa y accionable
- Referencias a archivos con rutas completas
- Decisiones con justificación clara
- Estado verificable y objetivo
- Próximos pasos específicos

---

## INTEGRACIÓN CON EL WORKFLOW

```
                    ┌─────────────────┐
                    │ /resume-context │
                    └────────┬────────┘
                             │
         ┌───────────────────┼───────────────────┐
         │                   │                   │
         ▼                   ▼                   ▼
┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
│  Continuar en   │ │  Nueva sesión   │ │   Compartir     │
│ misma sesión    │ │  mismo usuario  │ │  otro usuario   │
└─────────────────┘ └─────────────────┘ └─────────────────┘
```

### Frecuencia Recomendada

| Situación | Frecuencia |
|-----------|------------|
| Sesión normal | Al finalizar cada fase del workflow |
| Sesión larga | Cada 30-50 mensajes |
| Trabajo complejo | Después de cada decisión importante |
| Antes de pausa | Siempre |

---

## OUTPUT ESPERADO

Al ejecutar `/resume-context`, entregar:

1. **Resumen estructurado** siguiendo el formato definido
2. **Validación** del checklist de completitud
3. **Instrucciones** de cómo usar el resumen según el caso de uso
