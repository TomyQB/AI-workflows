# Subagent Skill Access Control

## Regla estricta

Cada subagente SOLO puede usar los skills asignados en su lista. Usar un skill no autorizado esta **terminantemente prohibido**. Si un subagente no tiene skills asignados, no debe leer ni referenciar ningun archivo de skill.

Cuando se lance un subagente con acceso a skills, incluir en el prompt del Task la instruccion de leer los archivos del skill correspondiente antes de trabajar.

## Asignacion de skills por subagente

| Subagente | Skills permitidos | Instruccion para el prompt |
|-----------|-------------------|----------------------------|
| `developer` | `java-spring-boot` | Leer `/home/local/.claude/skills/java-spring-boot/SKILL.md` y sus `references/` antes de escribir codigo Java |
| `architect` | ninguno | — |
| `tester` | `junit-mockito` | Leer `/home/local/.claude/skills/junit-mockito/SKILL.md` y sus `references/` antes de escribir tests |
| `security-auditor` | ninguno | — |
| `flow-generator` | ninguno | — |

## Skills exclusivos de conversacion principal

Los siguientes skills solo se usan en la conversacion principal, nunca en subagentes:

- `skill-creator`
