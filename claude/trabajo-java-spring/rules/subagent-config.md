# Configuracion de Subagentes

## Delegacion obligatoria

NUNCA editar `.java` directamente (enforcement por hook).
Delegar: `src/main` -> developer, `src/test` -> tester.
Archivos no-Java (.yml, .properties, .xml, .md) pueden editarse directamente.

## Skills, Rules y MCP por subagente

| Subagente | Skill | Rules a leer | MCP |
|-----------|-------|-------------|-----|
| developer | java-spring-boot (SKILL.md + references/) | hooks/references/architecture-principles.md | - |
| tester | junit-mockito (SKILL.md + references/) | - | - |
| architect | - | hooks/references/architecture-principles.md | - |
| security-auditor | - | - | - |
| postman-generator | - | - | Postman MCP |

## Protocolo de lanzamiento

- Informar: "Lanzando {subagente} con skill: {skill}" o "sin skills"
- Subagente confirma: "[Skill cargado: {skill}]" o "[Sin skills asignados]"
- NUNCA usar MCP Postman desde conversacion principal -> delegar a postman-generator
- Skills exclusivos de conversacion principal: skill-creator
