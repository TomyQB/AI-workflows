# Configuración del Entorno

## Entorno WSL

Trabajamos desde WSL (Ubuntu). La configuración de Git (user.name, user.email, credenciales) está SOLO en Windows, NO en WSL. No instalar ni configurar credenciales de Git en WSL.

## Operaciones Git

### Comandos con autenticación

Las operaciones que necesitan credenciales de Bitbucket DEBEN ejecutarse usando `git.exe` (el Git de Windows) en lugar de `git` (Linux):

- `git.exe pull`
- `git.exe push`  
- `git.exe fetch` (desde remoto)

Gracias a WSL Interop, `git.exe` está disponible desde WSL y usa las credenciales configuradas en Windows.

### Comandos locales

Las operaciones que NO requieren autenticación SI se pueden ejecutar con `git` (Linux) desde WSL:

- `git checkout`
- `git branch`
- `git commit`
- `git log`
- `git diff`
- `git status`
- `git stash`
- `git merge`
- `git rebase`

### Operaciones permitidas

Cualquier operación git que no esté en la siguiente lista está terminantemente prohibido usarla:
`git checkout`, `git branch`, `git commit`, `git log`, `git diff`, `git status`, `git stash`, `git pull`, `git push`, `git fetch`.
También estarán permitidos los comandos de solo lectura, como buscar el historial de cambios, commits…

- Para `git pull`, `git push`, `git fetch`: usar siempre `git.exe` (Windows).
- Para el resto: usar `git` (Linux).
