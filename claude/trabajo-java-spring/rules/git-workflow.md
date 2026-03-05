# Git Branch Management - Pre-coding workflow

## Regla obligatoria

Antes de escribir o editar código por primera vez en una sesión, DEBO ejecutar el siguiente flujo de preguntas interactivas. Esto aplica SOLO cuando voy a hacer cambios en archivos (Write, Edit), NO cuando solo estoy leyendo, explorando o respondiendo preguntas. Aún que ya haya cambios en la rama se debe hacer este proceso si aún no se han modificado archivos en la sesión, da igual qué tipo de modificación se requiera, hay que proceder con este flujo

### Paso 0: Verificación

- Verificar que el directorio actual es un repositorio Git ejecutando `git rev-parse --is-inside-work-tree`.
- Si NO es un repo Git, saltar esta regla completamente y proceder normalmente.

### Paso 1: Preguntar si se necesita nueva rama

Usar la herramienta de preguntas interactivas (mcp_question) para preguntar al usuario:

- **Pregunta**: "Es necesario crear una nueva rama?"
- **Opciones**: "Si" / "No"

Si la respuesta es **No** -> proceder a desarrollar sin hacer más preguntas.

### Paso 2: Tipo de rama (solo si respondió "Si")

Preguntar al usuario:

- **Pregunta**: "Que tipo de rama necesitas?"
- **Opciones**:
  - `feature` - Se crea desde la rama `develop`
  - `fix-release` - Se crea desde la rama `release`
  - `hotfix` - Se crea desde la rama `hotfixes`

### Paso 3: Nombre de la rama

Preguntar al usuario con campo libre (sin opciones predefinidas, solo texto libre):

- **Pregunta**: "Escribe el nombre de la rama (sin el prefijo, solo el nombre)"

### Paso 4: Ejecución de comandos Git

Ejecutar los comandos Git correspondientes según el tipo seleccionado:

- **feature**: `git checkout develop && git.exe pull && git checkout -b feature/<nombre>`
- **fix-release**: `git checkout release && git.exe pull && git checkout -b fix-release/<nombre>`
- **hotfix**: `git checkout hotfixes && git.exe pull && git checkout -b hotfix/<nombre>`

Confirmar al usuario que la rama fue creada exitosamente antes de proceder a escribir código.

### Restricciones

- Esta pregunta se hace UNA SOLA VEZ por sesión: la primera vez que se va a escribir o editar código.
- No repetir la pregunta si ya se respondió en la sesión actual.
- No aplicar esta regla si la tarea del usuario es solo de lectura, exploración o consulta (sin modificar archivos).
- Si hay conflictos al hacer el git pull avisar al usuario para que los resuelva manualmente.

---

## Recuperación: mover commits a otra rama

Si el usuario hizo push en la rama equivocada y necesita mover commits a una rama nueva:

### Flujo

1. **Identificar los commits** a mover: `git log --oneline -N`
2. **Guardar cambios locales**: `git stash --include-untracked`
3. **Ir a la rama base**: `git checkout <rama-base>` (develop, release, hotfixes según el tipo)
4. **Actualizar**: `git.exe pull`
5. **Crear nueva rama**: `git checkout -b <tipo>/<nombre>`
6. **Cherry-pick** de los commits: `git.exe cherry-pick <commit-hash>` (usar `git.exe` para evitar problemas de identidad en WSL)
7. **Limpiar stash**: `git stash drop`
8. **Push**: `git.exe push -u origin <tipo>/<nombre>`

### Notas

- Usar `git.exe` (Windows) para cherry-pick y push porque requieren identidad/credenciales configuradas en Windows.
- Si hay cambios locales que bloquean el cherry-pick, hacer `git stash --include-untracked` primero.
- Si hay conflictos en el cherry-pick, avisar al usuario para resolución manual.
