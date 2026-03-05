# Subagent Skill Access Control

## Regla estricta

Cada subagente SOLO puede usar los skills asignados en su lista. Usar un skill no autorizado esta **terminantemente prohibido**. Si un subagente no tiene skills asignados, no debe leer ni referenciar ningun archivo de skill.

Cuando se lance un subagente con acceso a skills, incluir en el prompt del Task la instruccion de leer los archivos del skill correspondiente antes de trabajar.

## Visibilidad de carga

### En la conversacion principal (al lanzar el subagente)

SIEMPRE informar al usuario que skills se van a cargar ANTES de lanzar el Task.

- Con skills: `Lanzando {subagente} con skill: {nombre_skill}`
- Sin skills: `Lanzando {subagente} sin skills`

### En el prompt del subagente

Incluir la instruccion de que el subagente confirme al inicio de su respuesta que leyo el skill.

- Con skills: el subagente debe iniciar su trabajo con `[Skill cargado: {nombre_skill}]`
- Sin skills: el subagente debe iniciar su trabajo con `[Sin skills asignados]`

## Asignacion de skills por subagente

| Subagente | Skills permitidos | Instruccion para el prompt |
|-----------|-------------------|----------------------------|
| `developer` | `java-spring-boot` | Leer `/home/local/.claude/skills/java-spring-boot/SKILL.md` y sus `references/` antes de escribir codigo Java |
| `architect` | ninguno | — |
| `tester` | `junit-mockito` | Leer `/home/local/.claude/skills/junit-mockito/SKILL.md` y sus `references/` antes de escribir tests |
| `security-auditor` | ninguno | — |
| `flow-generator` | ninguno | — |

## Rules compartidas por subagente

Ademas de los skills, algunos subagentes deben leer rules especificas. Incluir la instruccion de lectura en el prompt del Task.

| Subagente | Rules a leer |
|-----------|-------------|
| `architect` | `/home/local/.claude/rules/architecture-principles.md` |
| `developer` | `/home/local/.claude/rules/architecture-principles.md` |

## Skills exclusivos de conversacion principal

Los siguientes skills solo se usan en la conversacion principal, nunca en subagentes:

- `skill-creator`
