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

### 1.3 Recopilar Contexto Técnico Java/Spring

**Identificar:**
- Versión de Java (17, 21, etc.)
- Versión de Spring Boot
- Módulos Spring utilizados (Security, Data JPA, Web, etc.)
- Build tool (Maven/Gradle) y configuración relevante
- Base de datos y ORM
- Dependencias clave del pom.xml/build.gradle

---

## FASE 2: INVENTARIO DE ARTEFACTOS

### 2.1 Archivos Creados

Listar por capa arquitectónica:

```markdown
### Controllers
| Archivo | Endpoints | Estado |
|---------|-----------|--------|
| TransferController.java | POST /transfers, GET /transfers/{id} | Completo |

### Services
| Archivo | Interfaz | Implementación | Estado |
|---------|----------|----------------|--------|
| TransferService | ✓ | TransferServiceImpl | Completo |

### Repositories
| Archivo | Entidad | Queries Custom | Estado |
|---------|---------|----------------|--------|
| TransferRepository | Transfer | findByStatus() | Completo |

### DTOs
| Archivo | Tipo | Validaciones | Estado |
|---------|------|--------------|--------|
| TransferRequest | Record | @NotNull, @Positive | Completo |
| TransferResponse | Record | - | Completo |

### Entities
| Archivo | Tabla | Relaciones | Estado |
|---------|-------|------------|--------|
| Transfer | transfers | ManyToOne Account | Completo |

### Validators/Mappers/Strategies
| Archivo | Tipo | Propósito | Estado |
|---------|------|-----------|--------|
| TransferValidator | Validator | Reglas de negocio | Completo |
| TransferMapper | Mapper | Entity ↔ DTO | Completo |

### Tests
| Archivo | Clase Testeada | Cobertura | Estado |
|---------|----------------|-----------|--------|
| TransferServiceImplTest | TransferServiceImpl | 95% | Completo |
```

### 2.2 Archivos Modificados

```markdown
| Archivo | Cambios | Motivo |
|---------|---------|--------|
| pom.xml | +spring-security | Añadir autenticación |
| application.yml | +datasource config | Configurar BD |
```

### 2.3 Contexto Leído

- `pom.xml` / `build.gradle` - Dependencias del proyecto
- `application.yml` - Configuración
- Entidades existentes relacionadas
- Services existentes que se integran

---

## FASE 3: DECISIONES Y ACUERDOS

### 3.1 Decisiones Arquitectónicas

```markdown
### Decisión: Patrón para cálculo de comisiones
- **Contexto:** Diferentes tipos de cuenta tienen diferentes comisiones
- **Opciones:** if-else en service vs Strategy Pattern
- **Elegido:** Strategy Pattern con sealed interface
- **Razón:** OCP - nuevos tipos sin modificar código existente
- **Implementación:** FeeCalculationStrategy + 3 implementaciones

### Decisión: Manejo de transacciones
- **Contexto:** Transferencias requieren consistencia
- **Elegido:** @Transactional en ServiceImpl
- **Razón:** Spring gestiona rollback automático
```

### 3.2 Patrones SOLID Aplicados

| Principio | Aplicación | Clases Afectadas |
|-----------|------------|------------------|
| SRP | Validator separado del Service | TransferValidator |
| OCP | Strategy para tipos de cuenta | AccountStrategy |
| DIP | Inyección de interfaces | TransferServiceImpl |
| ISP | Interfaces segregadas | AccountQueryService, AccountCrudService |

### 3.3 Features Java 21 Utilizadas

- [ ] Records para DTOs
- [ ] Sealed classes para Strategies
- [ ] Pattern matching en switch
- [ ] Virtual threads para I/O
- [ ] Otras: {especificar}

---

## FASE 4: ESTADO DE IMPLEMENTACIÓN

### 4.1 Capas Completadas

```markdown
[x] Entity + Repository
[x] DTOs (Request/Response)
[x] Service Interface
[ ] Service Implementation (80%)
[ ] Validator
[ ] Mapper
[ ] Controller
[ ] Tests
[ ] Security config
```

### 4.2 Tests Estado

| Suite | Tests | Passing | Coverage |
|-------|-------|---------|----------|
| TransferServiceImplTest | 15 | 15 | 95% |
| TransferValidatorTest | 8 | 8 | 100% |
| TransferMapperTest | 5 | 5 | 100% |
| **Total** | 28 | 28 | 96% |

### 4.3 Problemas Encontrados

```markdown
### Problema: Lazy loading en tests
- **Descripción:** LazyInitializationException en tests
- **Causa:** Sesión Hibernate cerrada
- **Solución aplicada:** @Transactional en test
- **Estado:** Resuelto

### Problema: Validación circular
- **Descripción:** Validator necesita Repository
- **Estado:** Pendiente de decisión
```

### 4.4 Próximos Pasos

1. Completar TransferServiceImpl.executeTransfer()
2. Crear TransferValidator con reglas de negocio
3. Implementar tests para edge cases
4. Añadir Spring Security a endpoints

---

## FASE 5: GENERACIÓN DEL RESUMEN

### Formato de Output para Java/Spring

```markdown
# Resumen de Contexto - [Módulo/Feature]

**Fecha:** {fecha}
**Fase actual:** {fase del workflow}

## 1. Objetivo
{Qué se está construyendo}

## 2. Stack Técnico
- **Java:** 21
- **Spring Boot:** 3.2.x
- **Spring Modules:** Web, Data JPA, Security, Validation
- **Build:** Maven/Gradle
- **DB:** PostgreSQL + Hibernate
- **Testing:** JUnit 5, Mockito, AssertJ

## 3. Progreso del Workflow
- [x] /init
- [x] /architect
- [ ] /developer - EN PROGRESO
- [ ] /tester
- [ ] /security-auditor

## 4. Arquitectura Implementada

### Estructura de Paquetes
```
com.example.module/
├── controller/     [Pendiente]
├── dto/            [Completo]
├── entity/         [Completo]
├── repository/     [Completo]
├── service/        [80%]
├── validator/      [Pendiente]
├── mapper/         [Completo]
└── exception/      [Completo]
```

### Componentes Creados
| Componente | Clase | Estado |
|------------|-------|--------|
| Entity | Transfer | ✓ |
| Repository | TransferRepository | ✓ |
| DTO Request | TransferRequest (Record) | ✓ |
| DTO Response | TransferResponse (Record) | ✓ |
| Service | TransferServiceImpl | 80% |
| Mapper | TransferMapper | ✓ |
| Validator | TransferValidator | Pendiente |
| Controller | TransferController | Pendiente |

## 5. Decisiones Tomadas

### Arquitectura
- **Patrón:** Clean Architecture con capas separadas
- **DTOs:** Records con Jakarta Validation
- **Entities:** @Builder de Lombok, sin setters públicos
- **Services:** Interface + Impl, inyección por constructor

### Patrones SOLID
- **SRP:** Validator, Mapper, Service separados
- **OCP:** Strategy para {especificar si aplica}
- **DIP:** Todas las dependencias son interfaces

### Testing
- **Framework:** JUnit 5 + Mockito (BDDMockito)
- **Estructura:** Given/When/Then con @Nested
- **Data:** Object Mothers (TransferRequestMother, etc.)

## 6. Código Clave

### Service (en progreso)
```java
// TransferServiceImpl.java - Línea 45
// Pendiente: implementar validación de fondos
public TransferResponse executeTransfer(TransferRequest request) {
    // TODO: llamar a validator
    // TODO: ejecutar transferencia
    // TODO: publicar evento
}
```

### Pendiente de Implementar
- `TransferValidator.validate()` - Reglas de negocio
- `TransferController` - Endpoints REST
- Tests de integración

## 7. Problemas/Bloqueos
- {problema 1}: {descripción y estado}

## 8. Próximos Pasos
1. Completar `TransferServiceImpl.executeTransfer()`
2. Crear `TransferValidator` con reglas:
   - Cuenta origen existe y activa
   - Cuenta destino existe
   - Fondos suficientes
   - Límite diario no excedido
3. Tests unitarios para validator
4. Controller con endpoints

## 9. Dependencias pom.xml Relevantes
```xml
<dependency>spring-boot-starter-web</dependency>
<dependency>spring-boot-starter-data-jpa</dependency>
<dependency>spring-boot-starter-validation</dependency>
<dependency>lombok</dependency>
```

## 10. Configuración Relevante
```yaml
# application.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/db
  jpa:
    hibernate:
      ddl-auto: validate
```

---
**Para continuar:** Pegar este resumen en nueva conversación + indicar siguiente paso
```

---

## INSTRUCCIONES DE USO

### Para Continuar en la Misma Conversación

1. Ejecutar `/resume-context`
2. Revisar el resumen generado
3. Confirmar que el contexto es correcto
4. Continuar: "Basándome en este contexto, el siguiente paso es..."

### Para Transferir a Nueva Conversación

1. Ejecutar `/resume-context`
2. Copiar el resumen generado
3. En nueva conversación, pegar el resumen
4. Añadir: "Continúa desde este contexto. Siguiente paso: [descripción]"
5. Importante: Mencionar que use los comandos `/developer`, `/tester`, etc.

### Para Compartir con Otro Desarrollador

1. Ejecutar `/resume-context`
2. Verificar que no hay información sensible (passwords, tokens)
3. Añadir contexto de negocio si es necesario
4. Compartir el resumen

---

## CHECKLIST DE VALIDACIÓN

- [ ] ¿Versiones de Java y Spring Boot especificadas?
- [ ] ¿Módulos Spring utilizados listados?
- [ ] ¿Estructura de paquetes documentada?
- [ ] ¿Componentes por capa identificados con estado?
- [ ] ¿Decisiones SOLID documentadas?
- [ ] ¿Patrones de diseño aplicados especificados?
- [ ] ¿Features Java 21 utilizadas marcadas?
- [ ] ¿Tests y cobertura documentados?
- [ ] ¿Próximos pasos son específicos y accionables?
- [ ] ¿Código pendiente tiene ubicación exacta?

---

## OUTPUT ESPERADO

Al ejecutar `/resume-context`, entregar:

1. **Resumen estructurado** con formato Java/Spring
2. **Estado por capa** (controller, service, repository, etc.)
3. **Cobertura de tests** actual
4. **Código pendiente** con ubicación exacta
5. **Instrucciones** de continuación
