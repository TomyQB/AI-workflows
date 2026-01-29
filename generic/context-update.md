# /context-update - Project Context Updater

## Objetivo
Mantener el archivo `CLAUDE.md` actualizado con los cambios recientes del proyecto, preservando el contexto existente y detectando nuevas tecnologías, patrones o convenciones introducidas.

**Ejecutar después de:**
- Completar un ciclo completo (`/architect` → `/developer` → `/tester` → `/security-auditor`)
- Merges grandes o nuevas features
- Cambios significativos en dependencias o arquitectura
- Añadir nuevas tecnologías al proyecto

---

## PREREQUISITO

Verificar que existe `CLAUDE.md`:
- Si NO existe → Sugerir ejecutar `/init` primero
- Si SÍ existe → Leerlo para tener la referencia base

---

## FASE 1: ANÁLISIS DE CAMBIOS RECIENTES

### 1.1 Cambios en Control de Versiones

```bash
# Últimos 20 commits con archivos modificados
git log --oneline --name-status -20

# Commits desde la última actualización del CLAUDE.md
git log --oneline --since="$(git log -1 --format=%ci CLAUDE.md 2>/dev/null || echo '1 week ago')"

# Archivos modificados recientemente
git diff --name-only HEAD~20 HEAD 2>/dev/null || git diff --name-only

# Archivos añadidos
git diff --name-status HEAD~20 HEAD | grep "^A" | cut -f2

# Archivos eliminados
git diff --name-status HEAD~20 HEAD | grep "^D" | cut -f2

# Archivos renombrados
git diff --name-status HEAD~20 HEAD | grep "^R" | cut -f2-3
```

### 1.2 Cambios en Dependencias

Revisar archivos de dependencias por cambios:

| Stack | Archivo | Qué buscar |
|-------|---------|------------|
| Node.js | `package.json` | Nuevas dependencies/devDependencies |
| Python | `requirements.txt`, `pyproject.toml`, `Pipfile` | Nuevas líneas |
| Java | `pom.xml`, `build.gradle` | Nuevas dependencies |
| Go | `go.mod` | Nuevos require |
| Rust | `Cargo.toml` | Nuevas dependencies |
| .NET | `*.csproj` | Nuevos PackageReference |
| PHP | `composer.json` | Nuevos require |
| Ruby | `Gemfile` | Nuevas gem |

```bash
# Diff de archivos de dependencias
git diff HEAD~20 HEAD -- package.json pom.xml build.gradle requirements.txt \
    pyproject.toml go.mod Cargo.toml composer.json Gemfile 2>/dev/null
```

### 1.3 Cambios Estructurales

Detectar:
- [ ] Nuevas carpetas/módulos añadidos
- [ ] Carpetas eliminadas o renombradas
- [ ] Reorganización de estructura
- [ ] Nuevos archivos de configuración

```bash
# Nuevos directorios creados recientemente
git diff --name-status HEAD~20 HEAD | grep "^A" | \
    xargs -I{} dirname {} 2>/dev/null | sort -u

# Directorios eliminados
git diff --name-status HEAD~20 HEAD | grep "^D" | \
    xargs -I{} dirname {} 2>/dev/null | sort -u
```

---

## FASE 2: DETECCIÓN DE NUEVOS ELEMENTOS

### 2.1 Nuevas Tecnologías

**Analizar dependencias nuevas:**
- ¿Se añadió un nuevo framework?
- ¿Se añadió una nueva librería importante (ORM, cache, auth)?
- ¿Se añadió una nueva herramienta de testing?
- ¿Se añadió integración con servicio externo?
- ¿Se cambió alguna herramienta existente por otra?

### 2.2 Nuevos Patrones

**Buscar en código nuevo:**
- ¿Se introdujo un nuevo patrón de diseño?
- ¿Se creó una nueva capa/abstracción?
- ¿Hay nuevos tipos de componentes?
- ¿Se introdujeron nuevas convenciones de naming?

**Indicadores por nombre de archivo:**
- Archivos con sufijos nuevos (`*Strategy`, `*Factory`, `*Handler`, `*Middleware`)
- Nuevas carpetas en la estructura (`middleware/`, `guards/`, `pipes/`)
- Nuevos decoradores/anotaciones
- Nuevos tipos de archivos de configuración

```bash
# Archivos nuevos que podrían indicar patrones
git diff --name-status HEAD~20 HEAD | grep "^A" | \
    grep -iE "(strategy|factory|event|listener|handler|middleware|guard|pipe|validator|mapper)" | \
    cut -f2
```

### 2.3 Nuevas Convenciones

**Comparar código nuevo vs existente:**
- ¿Se mantiene el mismo estilo de naming?
- ¿Se usan las mismas convenciones de código?
- ¿Los tests siguen el mismo patrón?
- ¿El manejo de errores es consistente?
- ¿Los imports siguen el mismo orden?

---

## FASE 3: ANÁLISIS DE CÓDIGO NUEVO

### 3.1 Leer Código Añadido

```bash
# Archivos de código fuente añadidos (excluyendo tests)
git diff --name-status HEAD~20 HEAD | grep "^A" | \
    grep -E "\.(js|ts|jsx|tsx|py|java|go|rs|kt|rb|php|cs)$" | \
    grep -vi "test\|spec\|__test" | cut -f2
```

**Para cada archivo nuevo, detectar:**
- Módulo/carpeta al que pertenece
- Tipo de componente (controller, service, model, etc.)
- Dependencias que usa/importa
- Patrones que implementa
- Convenciones de naming que sigue

### 3.2 Leer Tests Añadidos

```bash
# Tests nuevos
git diff --name-status HEAD~20 HEAD | grep "^A" | \
    grep -iE "(test|spec)\.(js|ts|py|java|go|rs)$" | cut -f2
```

**Verificar si los tests nuevos:**
- Siguen la convención existente documentada
- Usan el mismo framework
- Mantienen la estructura documentada (Given/When/Then, Arrange/Act/Assert)
- Usan los mismos helpers/factories

---

## FASE 4: COMPARACIÓN CON CLAUDE.md ACTUAL

### 4.1 Leer CLAUDE.md Existente

Cargar el contenido actual y hacer un inventario de:
- Stack tecnológico documentado
- Estructura del proyecto documentada
- Convenciones documentadas
- Patrones documentados
- Comandos documentados

### 4.2 Identificar Desactualizaciones

| Sección | Actualizar si... |
|---------|------------------|
| Stack Tecnológico | Se añadieron/actualizaron/eliminaron dependencias |
| Arquitectura | Se añadieron nuevas capas/módulos |
| Estructura | Cambió el árbol de directorios |
| Convenciones de Código | Se detectaron nuevos patrones de naming |
| Testing | Se añadieron nuevos tipos de tests o cambió el framework |
| Comandos Útiles | Se añadieron nuevos scripts en package.json/Makefile |
| Variables de Entorno | Se añadieron nuevas variables en .env.example |
| Reglas de Desarrollo | Se detectaron nuevas convenciones |

### 4.3 Detectar Inconsistencias

**Alertar si:**
- [ ] Código nuevo NO sigue las convenciones documentadas
- [ ] Se usan patrones diferentes a los documentados
- [ ] Naming inconsistente con lo establecido
- [ ] Tests con estructura diferente a la documentada
- [ ] Manejo de errores diferente al patrón establecido
- [ ] Nuevas dependencias que duplican funcionalidad de existentes

---

## FASE 5: ACTUALIZACIÓN

### 5.1 Reglas de Actualización

**OBLIGATORIO:**
1. **NO eliminar** información existente válida
2. **AÑADIR** nuevas tecnologías/patrones detectados
3. **ACTUALIZAR** versiones si cambiaron
4. **MARCAR** inconsistencias encontradas
5. **PRESERVAR** personalizaciones del usuario
6. **DOCUMENTAR** la razón de cada cambio

### 5.2 Marcar Cambios en el Archivo

Para secciones actualizadas, indicar claramente qué es nuevo:

```markdown
## Stack Tecnológico

| Categoría | Tecnología | Versión | Estado |
|-----------|------------|---------|--------|
| Framework | Express | 4.18 | Existente |
| Cache | Redis | 7.0 | **NUEVO** |
| Testing | Vitest | 1.0 | **ACTUALIZADO** (era Jest) |
| ORM | Sequelize | - | **ELIMINADO** |

## Nuevos Patrones Detectados

- **Observer Pattern:** Introducido en `EventEmitter.ts`, usado para notificaciones
- **Strategy Pattern:** Nuevo archivo `PaymentStrategy.ts` para tipos de pago
```

### 5.3 Añadir Changelog

Al final del CLAUDE.md:

```markdown
---

## Changelog de Contexto

### {YYYY-MM-DD}
- **Nuevas dependencias:** {lista o "Ninguna"}
- **Nuevos módulos:** {lista o "Ninguno"}
- **Nuevos patrones:** {lista o "Ninguno"}
- **Cambios en estructura:** {descripción o "Sin cambios"}
- **Inconsistencias detectadas:** {lista o "Ninguna"}
- **Dependencias eliminadas:** {lista o "Ninguna"}
```

---

## FASE 6: RESUMEN Y CONFIRMACIÓN

### 6.1 Mostrar Resumen

```markdown
# Resumen de Actualización

## Cambios Detectados
- X archivos nuevos analizados
- X dependencias nuevas / Y actualizadas / Z eliminadas
- X patrones nuevos introducidos
- X módulos/carpetas añadidos

## Secciones Actualizadas en CLAUDE.md
- [ ] Stack Tecnológico
- [ ] Arquitectura
- [ ] Estructura del Proyecto
- [ ] Convenciones de Código
- [ ] Testing
- [ ] Comandos Útiles
- [ ] Variables de Entorno

## Inconsistencias Encontradas
- {descripción de cada inconsistencia}
- {qué convención se viola y dónde}

## Acción Requerida
- Confirmar cambios propuestos
- Revisar inconsistencias detectadas
```

### 6.2 Solicitar Confirmación

Antes de guardar:
1. Mostrar diff del CLAUDE.md (cambios propuestos)
2. Listar cambios organizados por sección
3. Solicitar confirmación del usuario
4. Opción de ajustar manualmente antes de guardar

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

## PRINCIPIOS

- **Ejecutar periódicamente** después de ciclos de desarrollo
- **No sobrescribir** personalizaciones manuales del usuario
- **Alertar** sobre inconsistencias, no corregir código automáticamente
- Si hay **muchos cambios**, sugerir `/init` completo
- **Backup** antes de modificar: `cp CLAUDE.md CLAUDE.md.bak`

### Indicadores de que se necesita `/init` completo:
- Cambio de framework principal o migración significativa
- Reestructuración mayor de directorios o módulos
- Más de 50 archivos nuevos desde el último análisis
- Cambio de versión mayor del lenguaje
- Nuevo monorepo o split de microservicios
- Cambio de paradigma (REST → GraphQL, SQL → NoSQL)

---

## CICLO COMPLETO

```
/init → /architect → /developer → /tester → /security-auditor → /context-update
  ↑                                                                      │
  └──────────────────────────────────────────────────────────────────────┘
```

Si el proyecto ha evolucionado significativamente, considerar ejecutar `/init` nuevamente.

### Verificación de Calidad del CLAUDE.md

**Después de actualizar, el CLAUDE.md debe:**
- Ser una guía completa para cualquier desarrollador nuevo en el proyecto
- Reflejar fielmente el estado actual del código, no un estado deseado
- Incluir solo convenciones verificables con ejemplos reales del proyecto
- Servir como referencia para `/architect`, `/developer`, `/tester` y `/security-auditor`
- No contener información obsoleta o que contradiga el código actual
