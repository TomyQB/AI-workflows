# /init - Project Context Analyzer

## Objetivo
Analizar el repositorio completo, detectar tecnologías, arquitectura, patrones y convenciones para generar un archivo `CLAUDE.md` con el contexto específico del proyecto.

Este archivo será usado por todos los workflows (`/architect`, `/developer`, `/tester`, `/security-auditor`) para mantener coherencia con el proyecto.

---

## PREREQUISITO

Verificar que estamos en la raíz de un proyecto de código:
- ¿Existe `pom.xml`, `build.gradle`, `package.json`, `requirements.txt` u otro archivo de dependencias?
- ¿Existe estructura de código fuente (`src/`, `app/`, `lib/`)?
- Si NO existe → Preguntar si es el directorio correcto

---

## FASE 1: ANÁLISIS DE ESTRUCTURA

### 1.1 Detectar Tipo de Proyecto

```bash
# Listar estructura de directorios (excluyendo builds y dependencies)
ls -la
find . -maxdepth 3 -type d \
    -not -path "*/node_modules/*" \
    -not -path "*/target/*" \
    -not -path "*/build/*" \
    -not -path "*/.git/*" \
    -not -path "*/dist/*" \
    -not -path "*/__pycache__/*" \
    -not -path "*/venv/*" \
    2>/dev/null | head -60
```

**Identificar:**
- [ ] Tipo: monorepo, multi-módulo, single-module
- [ ] Estructura: por capas (controller/service/repository) o por features
- [ ] Ubicación de tests: `src/test`, `__tests__`, `*.spec.ts`
- [ ] Archivos de configuración
- [ ] Separación frontend/backend

### 1.2 Archivos Clave a Leer

| Archivo | Información que proporciona |
|---------|----------------------------|
| `pom.xml` / `build.gradle` | Dependencias Java, versión, plugins |
| `package.json` | Dependencias Node, scripts, versión |
| `requirements.txt` / `pyproject.toml` | Dependencias Python |
| `docker-compose.yml` | Servicios, bases de datos, infra |
| `Dockerfile` | Imagen base, configuración runtime |
| `.github/workflows/` | CI/CD, checks, pipeline |
| `README.md` | Documentación existente del proyecto |
| `.editorconfig` / `.prettierrc` / `.eslintrc` | Convenciones de formato |
| `tsconfig.json` | Configuración TypeScript |
| `application.yml` / `application.properties` | Configuración Spring |
| `.env.example` / `.env.template` | Variables de entorno necesarias |
| `Makefile` / `justfile` | Comandos del proyecto |

---

## FASE 2: DETECCIÓN DE TECNOLOGÍAS

### 2.1 Stack Backend

**Detectar:**
- [ ] Lenguaje: Java, Kotlin, Python, Node.js, Go, Rust, C#, Ruby, PHP
- [ ] Versión del lenguaje (en pom.xml, package.json, etc.)
- [ ] Framework: Spring Boot, Quarkus, Django, FastAPI, Express, NestJS, Gin
- [ ] Versión del framework
- [ ] ORM: JPA/Hibernate, MyBatis, Prisma, TypeORM, SQLAlchemy, GORM
- [ ] Base de datos: PostgreSQL, MySQL, MongoDB, Oracle, Redis
- [ ] Mensajería: Kafka, RabbitMQ, SQS
- [ ] Cache: Redis, Caffeine, Hazelcast

### 2.2 Stack Frontend (si aplica)

**Detectar:**
- [ ] Framework: React, Angular, Vue, Svelte, Next.js, Nuxt
- [ ] State management: Redux, Zustand, Pinia, NgRx, Context API
- [ ] Styling: Tailwind, Styled Components, SCSS, CSS Modules
- [ ] Build tool: Vite, Webpack, Turbopack

### 2.3 Infraestructura

**Detectar:**
- [ ] Contenedores: Docker, Kubernetes, Docker Compose
- [ ] Cloud: AWS, GCP, Azure (buscar en configs y CI/CD)
- [ ] CI/CD: GitHub Actions, GitLab CI, Jenkins, CircleCI
- [ ] Monitoring: Prometheus, Grafana, Datadog, New Relic

### 2.4 Testing

**Detectar:**
- [ ] Framework: JUnit 5, TestNG, Jest, Pytest, Vitest, Go testing
- [ ] Mocking: Mockito, MockK, Jest mocks, unittest.mock
- [ ] Assertions: AssertJ, Hamcrest, Chai, expect
- [ ] Coverage: JaCoCo, Istanbul, Coverage.py
- [ ] E2E: Cypress, Playwright, Selenium

---

## FASE 3: ANÁLISIS DE ARQUITECTURA

### 3.1 Patrones Arquitectónicos

**Buscar evidencia de:**

```bash
# Controllers/Endpoints
find . -name "*Controller*" -o -name "*Resource*" -o -name "*Handler*" | head -10

# Services
find . -name "*Service*" -o -name "*UseCase*" | head -10

# Repositories
find . -name "*Repository*" -o -name "*Dao*" -o -name "*Gateway*" | head -10

# DTOs
find . -name "*Dto*" -o -name "*Request*" -o -name "*Response*" | head -10

# Strategies
find . -name "*Strategy*" | head -10

# Validators
find . -name "*Validator*" | head -10

# Mappers
find . -name "*Mapper*" -o -name "*Converter*" | head -10

# Events
find . -name "*Event*" -o -name "*Listener*" | head -10

# Exceptions
find . -name "*Exception*" | head -10
```

### 3.2 Convenciones de Naming

**Detectar patrones:**
- Controllers: `{Feature}Controller` vs `{Feature}Resource` vs `{Feature}Handler`
- Services: `{Feature}Service` + `{Feature}ServiceImpl` vs solo clase
- DTOs: `{Feature}Dto` vs `{Feature}Request/{Feature}Response` vs Records
- Entities: `{Feature}Entity` vs `{Feature}`
- Tests: `{Feature}Test` vs `{Feature}Spec` vs `{Feature}Tests`
- Paquetes: `com.company.module.layer` vs `com.company.feature`

---

## FASE 4: ANÁLISIS DE CÓDIGO EXISTENTE

### 4.1 Leer Archivos Representativos

Seleccionar 2-3 archivos de cada capa y detectar:

- [ ] Uso de Lombok: `@Data`, `@Builder`, `@RequiredArgsConstructor`, `@Value`
- [ ] Uso de Records vs Classes para DTOs
- [ ] Estilo de inyección: constructor (recomendado) vs field (`@Autowired`)
- [ ] Uso de Optional vs null checks
- [ ] Uso de Streams vs loops tradicionales
- [ ] Manejo de excepciones: custom exceptions, GlobalExceptionHandler
- [ ] Logging: SLF4J, Log4j, nivel de detalle
- [ ] Validaciones: Jakarta Validation (@NotNull, @NotBlank), custom validators
- [ ] Transaccionalidad: @Transactional, readOnly
- [ ] Uso de `final` en variables y parámetros

### 4.2 Convenciones de Testing

**Detectar:**
- [ ] Estructura: Given/When/Then vs Arrange/Act/Assert
- [ ] Naming: `should_DoSomething_When_Condition` vs `testMethodName`
- [ ] Object Mothers / Test Builders / Fixtures
- [ ] Uso de `@Nested` para agrupar tests
- [ ] Uso de `@DisplayName` para descripciones
- [ ] Uso de BDDMockito (given/then) vs Mockito (when/verify)
- [ ] Assertions: AssertJ (assertThat) vs JUnit (assertEquals)
- [ ] Anotación base: `@ExtendWith(MockitoExtension.class)` vs `@SpringBootTest`
- [ ] Convención de nombres de clases: `{Clase}Test` vs `{Clase}Tests` vs `{Clase}Spec`
- [ ] Ubicación de test data: dentro del test vs archivos externos (JSON, CSV)
- [ ] Uso de `assertAll` para múltiples verificaciones agrupadas
- [ ] Integration tests: Testcontainers, H2 in-memory, WireMock

### 4.3 Convenciones de Arquitectura

**Detectar:**
- [ ] Inyección: `@RequiredArgsConstructor` vs `@Autowired` vs constructor manual
- [ ] DTOs: Records vs Classes con Lombok (@Data, @Value)
- [ ] Mappers: Manual vs MapStruct vs ModelMapper
- [ ] Validación: Jakarta Validation en DTOs vs Validators custom en Service
- [ ] Eventos: ApplicationEventPublisher vs Kafka vs colas internas
- [ ] Configuración: @ConfigurationProperties vs @Value
- [ ] Seguridad: Spring Security config style, OAuth2, JWT

---

## FASE 5: GENERACIÓN DEL CLAUDE.md

### 5.1 Estructura del Archivo

```markdown
# {Nombre del Proyecto}

## Descripción
{Breve descripción del proyecto y su propósito}

## Stack Tecnológico

| Categoría | Tecnología | Versión |
|-----------|------------|---------|
| Lenguaje | {lang} | {version} |
| Framework | {framework} | {version} |
| Base de datos | {db} | {version} |
| Testing | {test framework} | {version} |
| Build | {build tool} | {version} |
| CI/CD | {herramienta} | - |

## Arquitectura

{Descripción de la arquitectura: capas, módulos, comunicación}

## Estructura del Proyecto

```
{Árbol de directorios relevante}
```

## Convenciones de Código

### Naming
- Controllers: `{patrón detectado}`
- Services: `{patrón detectado}`
- Repositories: `{patrón detectado}`
- DTOs: `{patrón detectado}`
- Tests: `{patrón detectado}`

### Estilo de Código
- {Convención 1 detectada}
- {Convención 2 detectada}

### Patrones Utilizados
- {Patrón 1}: {Dónde se usa}
- {Patrón 2}: {Dónde se usa}

## Testing

- Framework: {framework}
- Estructura: {Given/When/Then o similar}
- Ubicación: {path}
- Convenciones: {descripción}

## Comandos Útiles

```bash
# Build
{comando}

# Test
{comando}

# Run
{comando}
```

## Variables de Entorno
{Lista de variables necesarias, sin valores sensibles}

## Reglas de Desarrollo

1. {Regla 1 inferida del código}
2. {Regla 2 inferida del código}
```

---

## FASE 6: VALIDACIÓN Y GUARDADO

### 6.1 Checklist de Completitud

Antes de guardar, verificar:

- [ ] ¿Se identificó el lenguaje y versión?
- [ ] ¿Se identificó el framework y versión?
- [ ] ¿Se documentó la estructura de carpetas?
- [ ] ¿Se detectaron las convenciones de naming?
- [ ] ¿Se identificaron los patrones de código usados?
- [ ] ¿Se documentaron las convenciones de testing?
- [ ] ¿Se incluyeron los comandos de build/test/run?
- [ ] ¿Se listaron las variables de entorno?

### 6.2 Verificar Si Ya Existe

Si existe `CLAUDE.md`, preguntar al usuario:
1. **Sobrescribir** → Reemplazar completamente
2. **Merge** → Combinar con información existente
3. **Cancelar** → No hacer cambios

---

## OUTPUT ESPERADO

1. **Análisis completo** mostrado al usuario con:
   - Stack tecnológico detectado
   - Arquitectura identificada
   - Convenciones extraídas

2. **CLAUDE.md generado** en la raíz del proyecto

3. **Resumen** de lo detectado para confirmación

---

## PRINCIPIOS DE ANÁLISIS

- **NO inventar** información que no esté en el código
- **Preguntar** si hay ambigüedad en las convenciones
- **Priorizar** las convenciones más usadas si hay inconsistencias
- **Incluir** solo información verificable del repositorio
- Si hay **README.md existente**, usar esa información como base
- **Verificar** versiones de Java en pom.xml (`<java.version>`, `<maven.compiler.source>`)
- **Comparar** convenciones en archivos recientes vs antiguos (priorizar recientes)
- **Detectar** si el proyecto usa Spring Profiles (application-{profile}.yml)
- **Identificar** si existe GlobalExceptionHandler para manejo centralizado de errores

---

## DETECCIÓN DE SEGURIDAD

### Identificar configuración de seguridad existente:

```bash
# Spring Security configs
find . -name "*Security*" -o -name "*Auth*" -o -name "*Jwt*" | head -10

# Configuración de CORS
grep -r "CorsConfiguration\|@CrossOrigin\|cors" --include="*.java" -l 2>/dev/null | head -5

# Encriptación y passwords
grep -r "BCrypt\|PasswordEncoder\|encrypt\|@Value.*key\|@Value.*secret" --include="*.java" -l 2>/dev/null | head -5
```

**Documentar:**
- [ ] ¿Existe Spring Security configurado?
- [ ] ¿Qué tipo de autenticación usa? (JWT, Session, OAuth2)
- [ ] ¿Existen anotaciones @PreAuthorize, @Secured, @RolesAllowed?
- [ ] ¿Hay un GlobalExceptionHandler para errores de seguridad?
- [ ] ¿CORS configurado? ¿Restrictivo o permisivo?

---

## SIGUIENTE PASO

Una vez generado el CLAUDE.md, el usuario puede:
1. Ejecutar `/architect` para diseñar una nueva funcionalidad
2. Ejecutar `/developer` si ya existe un plan aprobado
3. Revisar y ajustar manualmente el CLAUDE.md generado
