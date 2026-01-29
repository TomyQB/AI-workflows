# Claude Code Development Workflow

<div align="center">

![Claude Code](https://img.shields.io/badge/Claude-Code-blueviolet?style=for-the-badge&logo=anthropic)
![License](https://img.shields.io/badge/License-MIT-green?style=for-the-badge)
![Version](https://img.shields.io/badge/Version-1.0.0-blue?style=for-the-badge)

**Professional slash commands for structured software development with Claude Code**

[English](#english) · [Español](#español)

</div>

---

## English

### Overview

A comprehensive set of slash commands that transform Claude Code into a structured development workflow. These commands enforce best practices, clean architecture, and professional-grade code quality through a sequential development process.

### Why Use This?

- **Structured Workflow**: Enforces a disciplined approach from architecture to security
- **Consistent Quality**: Every feature follows the same rigorous process
- **Best Practices Built-in**: SOLID principles, Clean Architecture, OWASP Top 10
- **Two Variants**: Technology-agnostic or Java/Spring-specific commands
- **Production Ready**: Designed for enterprise-grade applications

### Available Variants

| Folder | Description | Best For |
|--------|-------------|----------|
| `generic/` | Technology-agnostic commands | Any stack (Node.js, Python, Go, etc.) |
| `java + spring/` | Java 21 + Spring Boot 3.2+ specific | Java enterprise applications |

### The Workflow

```
┌─────────────────────────────────────────────────────────────────────┐
│                     DEVELOPMENT WORKFLOW                            │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│   /init              → Analyzes project, generates CLAUDE.md        │
│        ↓                                                            │
│   /architect         → Architecture design & implementation plan    │
│        ↓                                                            │
│   /developer         → Production code implementation               │
│        ↓                                                            │
│   /tester            → Unit tests with 90-100% coverage             │
│        ↓                                                            │
│   /security-auditor  → OWASP Top 10 security audit                  │
│        ↓                                                            │
│   /context-update    → Updates CLAUDE.md with changes               │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### Commands Description

| Command | Role | Responsibility |
|---------|------|----------------|
| `/init` | Project Analyzer | Scans codebase, detects stack, generates context file |
| `/architect` | Software Architect | Designs architecture, defines components, creates implementation plan |
| `/developer` | Senior Developer | Implements production-ready code following the plan |
| `/tester` | QA Engineer | Creates comprehensive tests with high coverage |
| `/security-auditor` | Security Engineer | Performs OWASP Top 10 audit, identifies vulnerabilities |
| `/context-update` | Context Maintainer | Keeps CLAUDE.md synchronized with project changes |

### Installation

#### Option 1: Global Installation (All Projects)

```bash
# Clone to Claude Code commands directory
git clone https://github.com/YOUR_USERNAME/claude-code-workflow.git ~/.claude/commands/

# Or copy specific variant
cp -r generic/ ~/.claude/commands/
cp -r "java + spring/" ~/.claude/commands/
```

#### Option 2: Project-Specific Installation

```bash
# Copy to your project's .claude/commands folder
mkdir -p .claude/commands
cp -r generic/* .claude/commands/

# Or for Java projects
cp -r "java + spring/"* .claude/commands/
```

### Usage

1. **Initialize your project** (first time only):
   ```
   /init
   ```

2. **For each new feature/task**, follow the workflow:
   ```
   /architect    # Design the solution
   /developer    # Implement the code
   /tester       # Write the tests
   /security-auditor  # Security review
   ```

3. **After significant changes**:
   ```
   /context-update
   ```

### Requirements

- [Claude Code CLI](https://github.com/anthropics/claude-code) installed
- For `java + spring/` variant:
  - Java 21+
  - Spring Boot 3.2+
  - Maven or Gradle

---

## Español

### Descripción

Un conjunto completo de slash commands que transforman Claude Code en un flujo de trabajo de desarrollo estructurado. Estos comandos aplican buenas prácticas, arquitectura limpia y calidad de código profesional a través de un proceso de desarrollo secuencial.

### ¿Por Qué Usarlo?

- **Flujo Estructurado**: Impone un enfoque disciplinado desde la arquitectura hasta la seguridad
- **Calidad Consistente**: Cada feature sigue el mismo proceso riguroso
- **Buenas Prácticas Integradas**: Principios SOLID, Clean Architecture, OWASP Top 10
- **Dos Variantes**: Comandos agnósticos de tecnología o específicos para Java/Spring
- **Listo para Producción**: Diseñado para aplicaciones empresariales

### Variantes Disponibles

| Carpeta | Descripción | Ideal Para |
|---------|-------------|------------|
| `generic/` | Comandos agnósticos de tecnología | Cualquier stack (Node.js, Python, Go, etc.) |
| `java + spring/` | Específico para Java 21 + Spring Boot 3.2+ | Aplicaciones empresariales Java |

### El Flujo de Trabajo

```
┌─────────────────────────────────────────────────────────────────────┐
│                     FLUJO DE DESARROLLO                             │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│   /init              → Analiza proyecto, genera CLAUDE.md           │
│        ↓                                                            │
│   /architect         → Diseño de arquitectura y plan de implementación│
│        ↓                                                            │
│   /developer         → Implementación de código de producción       │
│        ↓                                                            │
│   /tester            → Tests unitarios con cobertura 90-100%        │
│        ↓                                                            │
│   /security-auditor  → Auditoría de seguridad OWASP Top 10          │
│        ↓                                                            │
│   /context-update    → Actualiza CLAUDE.md con los cambios          │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### Descripción de Comandos

| Comando | Rol | Responsabilidad |
|---------|-----|-----------------|
| `/init` | Analizador de Proyecto | Escanea el código, detecta stack, genera archivo de contexto |
| `/architect` | Arquitecto de Software | Diseña arquitectura, define componentes, crea plan de implementación |
| `/developer` | Desarrollador Senior | Implementa código de producción siguiendo el plan |
| `/tester` | Ingeniero QA | Crea tests completos con alta cobertura |
| `/security-auditor` | Ingeniero de Seguridad | Realiza auditoría OWASP Top 10, identifica vulnerabilidades |
| `/context-update` | Mantenedor de Contexto | Mantiene CLAUDE.md sincronizado con cambios del proyecto |

### Instalación

#### Opción 1: Instalación Global (Todos los Proyectos)

```bash
# Clonar en el directorio de comandos de Claude Code
git clone https://github.com/YOUR_USERNAME/claude-code-workflow.git ~/.claude/commands/

# O copiar variante específica
cp -r generic/ ~/.claude/commands/
cp -r "java + spring/" ~/.claude/commands/
```

#### Opción 2: Instalación por Proyecto

```bash
# Copiar a la carpeta .claude/commands de tu proyecto
mkdir -p .claude/commands
cp -r generic/* .claude/commands/

# O para proyectos Java
cp -r "java + spring/"* .claude/commands/
```

### Uso

1. **Inicializar tu proyecto** (solo la primera vez):
   ```
   /init
   ```

2. **Para cada nueva feature/tarea**, seguir el flujo:
   ```
   /architect    # Diseñar la solución
   /developer    # Implementar el código
   /tester       # Escribir los tests
   /security-auditor  # Revisión de seguridad
   ```

3. **Después de cambios significativos**:
   ```
   /context-update
   ```

### Requisitos

- [Claude Code CLI](https://github.com/anthropics/claude-code) instalado
- Para la variante `java + spring/`:
  - Java 21+
  - Spring Boot 3.2+
  - Maven o Gradle

---

## Features by Variant

### Generic Variant

- Technology-agnostic SOLID principles
- Adaptable Clean Architecture patterns
- Framework-independent testing strategies
- Universal security checklist (OWASP Top 10)
- Supports: JavaScript/TypeScript, Python, Go, Rust, Ruby, PHP, etc.

### Java + Spring Variant

- Java 21 modern features (Records, Sealed Classes, Pattern Matching, Virtual Threads)
- Spring Boot 3.2+ specific patterns
- JUnit 5 + Mockito + AssertJ testing
- Spring Security integration
- Jakarta Validation
- Lombok annotations
- BDDMockito style (given/when/then)
- Object Mothers for test data

---

## Project Structure

```
.
├── README.md
├── generic/
│   ├── CLAUDE.md            # Global guidelines
│   ├── init.md              # /init command
│   ├── architect.md         # /architect command
│   ├── developer.md         # /developer command
│   ├── tester.md            # /tester command
│   ├── security-auditor.md  # /security-auditor command
│   └── context-update.md    # /context-update command
│
└── java + spring/
    ├── CLAUDE.md            # Java/Spring guidelines
    ├── init.md              # /init command (Java-specific)
    ├── architect.md         # /architect command (Spring patterns)
    ├── developer.md         # /developer command (Java 21 features)
    ├── tester.md            # /tester command (JUnit 5 + Mockito)
    ├── security-auditor.md  # /security-auditor command (Spring Security)
    └── context-update.md    # /context-update command (Maven/Gradle)
```

---

## Best Practices Enforced

### Architecture
- Clean Architecture / Hexagonal Architecture
- Layered separation (Controller → Service → Repository)
- Domain-Driven Design concepts
- SOLID principles

### Code Quality
- Single Responsibility per class
- Dependency Injection (interfaces, not implementations)
- Immutability by default
- Meaningful naming conventions
- Maximum method length: 20 lines
- Maximum class length: 200 lines

### Testing
- BDD structure (Given/When/Then)
- 90-100% coverage target
- Object Mothers / Test Data Builders
- Isolated unit tests (no Spring context when possible)
- Edge cases and error scenarios

### Security
- OWASP Top 10 compliance
- Input validation at boundaries
- No sensitive data in logs
- Parameterized queries
- Security headers configuration

---

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request. For major changes, please open an issue first to discuss what you would like to change.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## Acknowledgments

- Built for use with [Claude Code](https://github.com/anthropics/claude-code) by Anthropic
- Inspired by Clean Architecture principles by Robert C. Martin
- Security guidelines based on [OWASP Top 10](https://owasp.org/www-project-top-ten/)

---

<div align="center">

**Made with ❤️ for developers who care about code quality**

[⬆ Back to top](#claude-code-development-workflow)

</div>
