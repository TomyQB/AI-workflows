# /context-update - Project Context Updater

## Objetivo
Mantener el archivo `CLAUDE.md` actualizado con los cambios recientes del proyecto, preservando el contexto existente y detectando nuevas tecnologías, patrones o convenciones introducidas.

**Ejecutar después de:**
- Completar un ciclo completo (`/architect` → `/developer` → `/tester` → `/security-auditor`)
- Merges grandes o nuevas features
- Cambios significativos en dependencias o arquitectura

---

## PREREQUISITO

Verificar que existe `CLAUDE.md`:

```bash
if [ ! -f "CLAUDE.md" ]; then
    echo "No existe CLAUDE.md. Ejecutar /init primero."
fi
```

Si no existe → Sugerir ejecutar `/init` primero.

---

## FASE 1: ANÁLISIS DE CAMBIOS RECIENTES

### 1.1 Commits Recientes

```bash
# Últimos 20 commits con archivos modificados
git log --oneline --name-status -20

# Commits desde la última actualización del CLAUDE.md
git log --oneline --since="$(git log -1 --format=%ci CLAUDE.md 2>/dev/null || echo '1 week ago')"

# Estadísticas de cambios
git diff --stat HEAD~20 HEAD 2>/dev/null
```

### 1.2 Archivos Modificados

```bash
# Archivos modificados recientemente
git diff --name-only HEAD~20 HEAD 2>/dev/null || git diff --name-only

# Archivos añadidos (nuevas funcionalidades)
git diff --name-status HEAD~20 HEAD | grep "^A" | cut -f2

# Archivos eliminados (funcionalidades removidas)
git diff --name-status HEAD~20 HEAD | grep "^D" | cut -f2

# Archivos renombrados (refactoring)
git diff --name-status HEAD~20 HEAD | grep "^R" | cut -f2-3
```

### 1.3 Cambios en Dependencias

```bash
# Cambios en pom.xml (Java/Maven)
git diff HEAD~20 HEAD -- pom.xml 2>/dev/null | grep "^[+-].*<dependency\|<artifactId\|<version>"

# Cambios en build.gradle
git diff HEAD~20 HEAD -- build.gradle build.gradle.kts 2>/dev/null | grep "^[+-].*implementation\|api\|testImplementation"

# Cambios en package.json (Node.js)
git diff HEAD~20 HEAD -- package.json 2>/dev/null | grep "^[+-]"

# Cambios en requirements.txt (Python)
git diff HEAD~20 HEAD -- requirements.txt pyproject.toml 2>/dev/null
```

---

## FASE 2: DETECCIÓN DE NUEVOS ELEMENTOS

### 2.1 Nuevas Capas o Módulos

**Detectar si se han añadido:**
- [ ] Nuevos paquetes/carpetas en la estructura
- [ ] Nuevos módulos en proyecto multi-módulo
- [ ] Nuevas capas (ej: se añadió `validator/`, `mapper/`, `strategy/`, `event/`)

```bash
# Nuevos directorios creados
git diff --name-status HEAD~20 HEAD | grep "^A" | \
    xargs -I{} dirname {} 2>/dev/null | sort -u
```

### 2.2 Nuevas Tecnologías o Dependencias

**Analizar cambios en archivos de dependencias:**

| Archivo | Buscar |
|---------|--------|
| `pom.xml` | Nuevas `<dependency>` y plugins |
| `build.gradle` | Nuevas `implementation`, `api` |
| `package.json` | Nuevas entradas en `dependencies` |
| `docker-compose.yml` | Nuevos servicios (Redis, Kafka, etc.) |
| `application.yml` | Nuevas configuraciones |

### 2.3 Nuevos Patrones Introducidos

**Buscar en archivos nuevos:**

```bash
# Archivos que podrían indicar nuevos patrones
git diff --name-status HEAD~20 HEAD | grep "^A" | \
    grep -E "(Strategy|Factory|Event|Listener|Exception|Validator|Mapper|Handler|Observer|Decorator)" | \
    cut -f2
```

- [ ] Nuevos Strategy patterns (clases que implementan una interfaz común)
- [ ] Nuevos Factory patterns (clases que crean objetos)
- [ ] Nuevos Event handlers (ApplicationEvent, EventListener)
- [ ] Nuevas excepciones custom
- [ ] Nuevos validators de negocio
- [ ] Nuevos mappers Entity/DTO
- [ ] Nuevas anotaciones custom

---

## FASE 3: ANÁLISIS DE ARCHIVOS NUEVOS

### 3.1 Leer Código Añadido

```bash
# Archivos de código Java añadidos (no tests, no config)
git diff --name-status HEAD~20 HEAD | grep "^A" | \
    grep -E "\.(java|kt|ts|py|go)$" | \
    grep -v "Test\|test\|spec" | \
    cut -f2
```

**Para cada archivo nuevo, detectar:**
- Paquete/módulo al que pertenece
- Tipo de clase (Controller, Service, Entity, DTO, Strategy, etc.)
- Dependencias que inyecta
- Patrones que implementa
- Anotaciones utilizadas (@Service, @Component, @RestController, etc.)
- Si usa Java 21 features (Records, Sealed Classes, Pattern Matching)

### 3.2 Leer Tests Añadidos

```bash
# Tests nuevos
git diff --name-status HEAD~20 HEAD | grep "^A" | \
    grep -E "(Test|Spec|test)\.(java|kt|ts|py)$" | \
    cut -f2
```

**Verificar si los tests nuevos:**
- Siguen la convención existente documentada (Given/When/Then)
- Usan el mismo framework (JUnit 5, Mockito)
- Mantienen la estructura documentada (@Nested, @DisplayName)
- Usan Object Mothers para datos de test
- Usan BDDMockito (given/then) vs Mockito clásico (when/verify)

---

## FASE 4: COMPARACIÓN CON CLAUDE.md ACTUAL

### 4.1 Leer CLAUDE.md Existente

Cargar el contenido actual para comparar.

### 4.2 Identificar Secciones a Actualizar

| Sección | Actualizar si... |
|---------|------------------|
| Stack Tecnológico | Se añadieron nuevas dependencias al pom.xml |
| Arquitectura | Se añadieron nuevas capas/módulos |
| Estructura del Proyecto | Cambió el árbol de directorios |
| Convenciones de Código | Se detectaron nuevos patrones |
| Testing | Se añadieron nuevos tipos de tests |
| Comandos Útiles | Se añadieron nuevos scripts/plugins Maven |
| Reglas de Desarrollo | Se detectaron nuevas convenciones |

### 4.3 Detectar Inconsistencias

**Alertar si:**
- [ ] Código nuevo NO sigue las convenciones documentadas
- [ ] Se usan patrones diferentes a los documentados (ej: inyección por campo en vez de constructor)
- [ ] Naming inconsistente con lo establecido
- [ ] Tests con estructura diferente (ej: no usan @Nested o no siguen Given/When/Then)
- [ ] DTOs como clases en vez de Records
- [ ] Missing `final` en parámetros y variables
- [ ] Servicios con lógica de validación que debería estar en Validator separado
- [ ] Controllers con lógica de negocio que debería estar en Service
- [ ] Excepciones genéricas (RuntimeException) en vez de excepciones tipadas
- [ ] Uso de Mockito clásico (when/verify) cuando el estándar es BDDMockito (given/then)
- [ ] Nuevos endpoints sin anotaciones de seguridad (@PreAuthorize, @Secured)
- [ ] Entidades JPA con setters públicos cuando el estándar es @Builder

### 4.4 Detectar Cambios en APIs

**Verificar:**
- [ ] ¿Se añadieron nuevos endpoints?
- [ ] ¿Se modificaron contratos de endpoints existentes?
- [ ] ¿Se cambiaron DTOs de request/response?
- [ ] ¿Se añadieron nuevas validaciones Jakarta en DTOs?
- [ ] ¿Se añadieron nuevos HTTP status codes en respuestas?

```bash
# Buscar nuevos endpoints
git diff --name-status HEAD~20 HEAD | grep "^A" | grep -i "controller" | cut -f2
```

---

## FASE 5: ACTUALIZACIÓN DEL CLAUDE.md

### 5.1 Reglas de Actualización

**OBLIGATORIO:**
1. **NO eliminar** información existente válida
2. **AÑADIR** nuevas tecnologías/patrones detectados
3. **ACTUALIZAR** versiones si cambiaron
4. **MARCAR** inconsistencias encontradas
5. **PRESERVAR** personalizaciones del usuario
6. **DOCUMENTAR** razón de cada cambio

### 5.2 Añadir Changelog

Al final del CLAUDE.md, añadir:

```markdown
---

## Changelog de Contexto

### {fecha}
- **Nuevas dependencias:** {lista o "Ninguna"}
- **Nuevos módulos/capas:** {lista o "Ninguno"}
- **Nuevos patrones:** {lista o "Ninguno"}
- **Cambios en estructura:** {descripción o "Sin cambios"}
- **Inconsistencias detectadas:** {lista o "Ninguna"}
```

### 5.3 Marcar Cambios en el Archivo

Para secciones actualizadas, indicar claramente qué es nuevo:

```markdown
## Stack Tecnológico

| Categoría | Tecnología | Versión | Estado |
|-----------|------------|---------|--------|
| ... | ... | ... | Existente |
| Caching | Redis | 7.0 | **NUEVO** |
| Messaging | Kafka | 3.6 | **NUEVO** |

## Nuevos Patrones Detectados

- **Observer Pattern:** Introducido en `TransferEventListener.java`
- **Strategy Pattern:** `FeeCalculationStrategy` con 3 implementaciones
```

---

## FASE 6: RESUMEN Y CONFIRMACIÓN

### 6.1 Mostrar Resumen de Cambios

```markdown
# Resumen de Actualización

## Cambios Detectados
- X archivos nuevos analizados
- X dependencias nuevas
- X patrones nuevos
- X posibles inconsistencias

## Secciones Actualizadas
- [ ] Stack Tecnológico
- [ ] Arquitectura
- [ ] Convenciones
- [ ] Testing
- [ ] Comandos

## Inconsistencias Encontradas
- {lista de inconsistencias si las hay}
```

### 6.2 Solicitar Confirmación

Antes de guardar:
1. Mostrar diff del CLAUDE.md (antes vs después)
2. Listar cambios propuestos
3. Solicitar confirmación del usuario

---

## OUTPUT ESPERADO

1. **Análisis de cambios** mostrado al usuario
2. **CLAUDE.md actualizado** preservando contexto existente
3. **Changelog** añadido con fecha y cambios
4. **Alertas** de inconsistencias si se detectaron
5. **Resumen** de la actualización realizada

---

## CHECKLIST ANTES DE GUARDAR

- [ ] ¿Se preservó toda la información existente válida?
- [ ] ¿Se añadieron las nuevas tecnologías/dependencias?
- [ ] ¿Se documentaron los nuevos patrones?
- [ ] ¿Se actualizó la estructura si cambió?
- [ ] ¿Se marcaron las inconsistencias encontradas?
- [ ] ¿Se añadió entrada en el changelog?
- [ ] ¿El usuario confirmó los cambios?

---

## PRINCIPIOS DE ACTUALIZACIÓN

- **Ejecutar periódicamente** después de ciclos completos de desarrollo
- **No sobrescribir** personalizaciones manuales del usuario
- **Alertar** sobre inconsistencias pero no corregir automáticamente el código
- Si hay **muchos cambios**, sugerir ejecutar `/init` completo
- **Crear backup** antes de modificar: `cp CLAUDE.md CLAUDE.md.bak`

---

## NOTAS

Este comando cierra el ciclo de desarrollo:

```
/init → /architect → /developer → /tester → /security-auditor → /context-update
  ↑                                                                      │
  └──────────────────────────────────────────────────────────────────────┘
```

Si el proyecto ha evolucionado significativamente desde el último `/init`, considerar ejecutar `/init` nuevamente en lugar de `/context-update`.

### Indicadores de que se necesita `/init` completo:
- Cambio de framework principal o versión mayor de Spring Boot
- Migración de base de datos significativa (cambio de ORM, nueva BD)
- Reestructuración de paquetes/módulos (renombrado masivo)
- Más de 50 archivos nuevos desde el último análisis
- Cambio de versión mayor del lenguaje (ej: Java 17 → Java 21)
- Introducción de nuevos paradigmas (eventos, CQRS, microservicios)

### Verificación de Calidad del CLAUDE.md

**Después de actualizar, el CLAUDE.md debe:**
- Ser una guía completa para cualquier desarrollador nuevo en el proyecto
- Reflejar fielmente el estado actual del código, no un estado deseado
- Incluir solo convenciones verificables con ejemplos reales del proyecto
- Servir como referencia para `/architect`, `/developer`, `/tester` y `/security-auditor`
