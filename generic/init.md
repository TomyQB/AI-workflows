# /init - Project Context Analyzer

## Objetivo

Analizar el repositorio completo, detectar tecnologías, arquitectura, patrones y convenciones para generar un archivo `GEMINI.md` con el contexto específico del proyecto.

Este archivo será usado por todos los workflows (`/architect`, `/developer`, `/tester`, `/security-auditor`) para mantener coherencia con el proyecto.

---

## PREREQUISITO

Verificar que estamos en la raíz de un proyecto de código:

- ¿Existe algún archivo de dependencias (`package.json`, `pom.xml`, `requirements.txt`, etc.)?
- ¿Existe estructura de código fuente (`src/`, `app/`, `lib/`)?
- Si NO existe → Preguntar si es el directorio correcto

---

## FASE 1: IDENTIFICACIÓN DEL STACK

### 1.1 Detectar Archivos de Configuración

Buscar archivos que indican el stack:

| Archivo                                         | Indica                            |
| ----------------------------------------------- | --------------------------------- |
| `package.json`                                  | Node.js / JavaScript / TypeScript |
| `pom.xml`                                       | Java / Maven                      |
| `build.gradle` / `build.gradle.kts`             | Java / Kotlin / Gradle            |
| `requirements.txt`, `pyproject.toml`, `Pipfile` | Python                            |
| `go.mod`                                        | Go                                |
| `Cargo.toml`                                    | Rust                              |
| `Gemfile`                                       | Ruby                              |
| `composer.json`                                 | PHP                               |
| `*.csproj`, `*.sln`                             | .NET / C#                         |
| `pubspec.yaml`                                  | Dart / Flutter                    |
| `Podfile`                                       | iOS / Swift                       |
| `CMakeLists.txt`                                | C / C++                           |
| `mix.exs`                                       | Elixir                            |
| `Makefile`, `justfile`                          | Build scripts                     |

### 1.2 Detectar Tipo de Proyecto

**Por estructura de directorios:**

- `src/`, `app/`, `lib/` → Código fuente
- `test/`, `tests/`, `__tests__/`, `spec/` → Tests
- `public/`, `static/`, `assets/` → Frontend/estáticos
- `api/`, `routes/`, `controllers/` → Backend
- `components/`, `views/`, `pages/` → Frontend
- `docker-compose.yml`, `Dockerfile` → Containerizado
- `.github/workflows/`, `.gitlab-ci.yml`, `Jenkinsfile` → CI/CD
- `packages/`, `apps/`, `modules/` → Monorepo
- `migrations/`, `db/` → Base de datos con migraciones
- `proto/`, `graphql/` → API schemas

**Categorías:**

- [ ] Backend API (REST, GraphQL, gRPC)
- [ ] Frontend SPA (React, Vue, Angular, Svelte)
- [ ] Fullstack (Next.js, Nuxt, SvelteKit)
- [ ] Mobile (React Native, Flutter, nativo)
- [ ] CLI (herramienta de línea de comandos)
- [ ] Librería / Package
- [ ] Microservicio
- [ ] Monolito
- [ ] Serverless (Lambda, Cloud Functions)

### 1.3 Detectar Frameworks

**Backend:**

- Express, Fastify, NestJS, Hono, Koa (Node.js)
- Django, FastAPI, Flask, Starlette (Python)
- Spring Boot, Quarkus, Micronaut (Java)
- Gin, Echo, Fiber, Chi (Go)
- Actix, Axum, Rocket (Rust)
- Rails, Sinatra, Hanami (Ruby)
- Laravel, Symfony, Slim (PHP)
- ASP.NET, Minimal API (.NET)
- Phoenix, Plug (Elixir)

**Frontend:**

- React, Next.js, Remix, Gatsby
- Vue, Nuxt
- Angular
- Svelte, SvelteKit
- Solid, Qwik, Astro

**Mobile:**

- React Native, Expo
- Flutter
- Swift/SwiftUI
- Kotlin/Jetpack Compose

**Desktop:**

- Electron, Tauri
- .NET MAUI, WPF

### 1.4 Detectar Herramientas de Build y Desarrollo

| Categoría       | Herramientas                                         |
| --------------- | ---------------------------------------------------- |
| Build JS/TS     | Vite, Webpack, Turbopack, esbuild, Rollup, Parcel    |
| Build Java      | Maven, Gradle                                        |
| Build Python    | setuptools, Poetry, PDM, Hatch                       |
| Formatters      | Prettier, Black, gofmt, rustfmt, scalafmt            |
| Linters         | ESLint, Pylint, Ruff, golangci-lint, Clippy, RuboCop |
| Type checking   | TypeScript, mypy, Pyright                            |
| Package manager | npm, yarn, pnpm, pip, Poetry, Cargo                  |

---

## FASE 2: ANÁLISIS DE ESTRUCTURA

### 2.1 Estructura de Carpetas

```bash
# Listar estructura de directorios (excluyendo builds y dependencies)
find . -maxdepth 3 -type d \
    -not -path "*/node_modules/*" \
    -not -path "*/target/*" \
    -not -path "*/build/*" \
    -not -path "*/.git/*" \
    -not -path "*/dist/*" \
    -not -path "*/venv/*" \
    -not -path "*/__pycache__/*" \
    -not -path "*/.next/*" \
    -not -path "*/.nuxt/*" \
    2>/dev/null | head -60
```

**Identificar:**

- ¿Organización por capas? (controller/service/repository)
- ¿Organización por features? (users/, orders/, products/)
- ¿Monorepo? (packages/, apps/, libs/)
- ¿Separación front/back?
- ¿Módulos independientes?
- ¿Domain-driven design? (bounded contexts)

### 2.2 Archivos Clave a Leer

| Tipo   | Archivos                                                  | Información                      |
| ------ | --------------------------------------------------------- | -------------------------------- |
| Config | `package.json`, `pom.xml`, etc.                           | Dependencias, versiones, scripts |
| Build  | `webpack.config.js`, `vite.config.ts`, `rollup.config.js` | Configuración de build           |
| CI/CD  | `.github/workflows/`, `.gitlab-ci.yml`                    | Pipelines, checks, deploys       |
| Docs   | `README.md`, `CONTRIBUTING.md`                            | Documentación existente          |
| Estilo | `.prettierrc`, `.eslintrc`, `.editorconfig`, `ruff.toml`  | Convenciones de formato          |
| Tests  | `jest.config.js`, `vitest.config.ts`, `pytest.ini`        | Framework de tests               |
| Types  | `tsconfig.json`                                           | Configuración TypeScript         |
| Env    | `.env.example`, `.env.template`                           | Variables de entorno necesarias  |
| Docker | `Dockerfile`, `docker-compose.yml`                        | Servicios, runtime               |
| Lint   | `.eslintrc`, `pylintrc`, `.golangci.yml`, `.rubocop.yml`  | Reglas de linting                |
| API    | `openapi.yaml`, `swagger.json`, `*.proto`                 | Especificaciones de API          |

### 2.3 Detectar Base de Datos y Migraciones

| Herramienta  | Archivos                     | Stack   |
| ------------ | ---------------------------- | ------- |
| Flyway       | `db/migration/V*.sql`        | Java    |
| Liquibase    | `db/changelog/*.xml`         | Java    |
| Prisma       | `prisma/schema.prisma`       | Node.js |
| TypeORM      | `src/migrations/*.ts`        | Node.js |
| Django       | `*/migrations/*.py`          | Python  |
| Alembic      | `alembic/versions/*.py`      | Python  |
| ActiveRecord | `db/migrate/*.rb`            | Ruby    |
| Ecto         | `priv/repo/migrations/*.exs` | Elixir  |

---

## FASE 3: ANÁLISIS DE CÓDIGO

### 3.1 Convenciones de Naming

Leer 2-3 archivos de cada tipo para detectar:

**Archivos:**

- `camelCase.js` vs `kebab-case.js` vs `PascalCase.js` vs `snake_case.py`
- Sufijos: `*.controller.ts`, `*.service.ts`, `*_handler.go`

**Código:**

- Variables: camelCase, snake_case, PascalCase
- Funciones/métodos: verbos, estilo
- Clases/tipos/interfaces: PascalCase, prefijos (I para interfaces)
- Constantes: UPPER_SNAKE_CASE
- Booleanos: is/has/can prefixes
- Enums: singular vs plural, UPPER_CASE vs PascalCase

### 3.2 Patrones de Código

Detectar:

- [ ] Estilo de imports (absolute vs relative, orden, agrupación)
- [ ] Manejo de errores (try/catch, Result types, error callbacks, Either)
- [ ] Async patterns (async/await, Promises, callbacks, channels, futures)
- [ ] Inyección de dependencias (manual, framework, container)
- [ ] State management (si es frontend)
- [ ] ORM/acceso a datos (si es backend)
- [ ] Logging (framework, nivel de detalle, formato)
- [ ] Configuración (env vars, config files, feature flags)
- [ ] Autenticación/autorización (JWT, sessions, OAuth)
- [ ] Serialización (JSON, protobuf, msgpack)

### 3.3 Convenciones de Testing

Detectar:

- [ ] Framework: Jest, Vitest, Pytest, JUnit, Go testing, RSpec, PHPUnit, ExUnit
- [ ] Ubicación: junto al código (`*.test.ts`), carpeta separada (`tests/`)
- [ ] Naming: `*.test.ts`, `*_test.py`, `*Test.java`, `*_spec.rb`, `*_test.go`
- [ ] Estructura: describe/it, Given/When/Then, Arrange/Act/Assert
- [ ] Mocking: librerías usadas, patrones
- [ ] Fixtures/factories/Object Mothers
- [ ] Coverage: herramienta y configuración
- [ ] E2E: Cypress, Playwright, Selenium

---

## FASE 4: GENERACIÓN DEL GEMINI.md

### Estructura del Archivo

```markdown
# {Nombre del Proyecto}

## Descripción

{Breve descripción del proyecto y su propósito}

## Stack Tecnológico

| Categoría       | Tecnología       | Versión   |
| --------------- | ---------------- | --------- |
| Lenguaje        | {lang}           | {version} |
| Framework       | {framework}      | {version} |
| Base de datos   | {db}             | {version} |
| Testing         | {test framework} | {version} |
| Linting         | {linter}         | {version} |
| CI/CD           | {herramienta}    | -         |
| Package Manager | {tool}           | {version} |

## Tipo de Proyecto

{Backend API / Frontend SPA / Fullstack / etc.}

## Arquitectura

{Descripción: capas, módulos, comunicación, organización}

## Estructura del Proyecto
```

{Árbol de directorios relevante}

````

## Convenciones de Código

### Naming
- Archivos: `{patrón detectado}`
- Variables: `{patrón detectado}`
- Funciones: `{patrón detectado}`
- Clases/Tipos: `{patrón detectado}`

### Estilo
- {Convención 1 detectada}
- {Convención 2 detectada}

### Patrones
- {Patrón 1}: {Dónde/cómo se usa}
- {Patrón 2}: {Dónde/cómo se usa}

## Testing
- Framework: {framework}
- Ubicación: {path}
- Naming: `{patrón}`
- Estructura: {Given/When/Then o similar}
- Mocking: {librerías}
- Coverage: {herramienta y target}

## Comandos

```bash
# Instalar dependencias
{comando}

# Ejecutar en desarrollo
{comando}

# Ejecutar tests
{comando}

# Build para producción
{comando}

# Linting
{comando}

# Type checking (si aplica)
{comando}
````

## Variables de Entorno

{Lista de variables necesarias, sin valores sensibles}

## Notas Adicionales

{Cualquier información relevante detectada}

```

---

## FASE 5: VALIDACIÓN

### Checklist de Completitud

- [ ] ¿Se identificó el lenguaje y versión?
- [ ] ¿Se identificó el framework y versión?
- [ ] ¿Se identificó el tipo de proyecto?
- [ ] ¿Se documentó la estructura de carpetas?
- [ ] ¿Se detectaron las convenciones de naming?
- [ ] ¿Se identificaron los patrones de código usados?
- [ ] ¿Se documentaron las convenciones de testing?
- [ ] ¿Se incluyeron los comandos principales?
- [ ] ¿Se listaron las variables de entorno?
- [ ] ¿Se detectó la base de datos y migraciones?

### Verificar Existencia Previa

Si ya existe `GEMINI.md`:
1. **Sobrescribir** → Reemplazar completamente
2. **Merge** → Combinar con información existente
3. **Cancelar** → No hacer cambios

---

## OUTPUT ESPERADO

1. **Análisis mostrado al usuario:**
   - Stack detectado con versiones
   - Tipo de proyecto
   - Estructura identificada
   - Convenciones extraídas
   - Patrones de código detectados

2. **GEMINI.md generado** en .agent/rules

3. **Resumen** para confirmación del usuario

---

## PRINCIPIOS

- **NO inventar** información que no esté en el código
- **Preguntar** si hay ambigüedad entre diferentes convenciones encontradas
- **Priorizar** convenciones más usadas si hay inconsistencias
- **Incluir** solo información verificable y contrastada con el código
- Usar **README.md existente** como referencia si existe
- **Reportar** inconsistencias detectadas al usuario con ejemplos concretos
- **Comparar** package.json scripts, Makefile targets, o CI/CD steps para comandos
- **Verificar** que las versiones detectadas son correctas mirando lockfiles

---

## SIGUIENTE PASO

Una vez generado el GEMINI.md:
1. Ejecutar `/architect` para diseñar una nueva funcionalidad
2. O revisar/ajustar manualmente el GEMINI.md generado
```
