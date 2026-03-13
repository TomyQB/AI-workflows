#!/bin/bash
# Safety net para PreToolUse (segundo cinturon)
# Los permisos estaticos se gestionan en settings.json (allow/ask/deny)
# Este hook captura patrones DINAMICOS que no se expresan con globs
#
# Flujo: hook exit 0 sin JSON → settings.json decide (deny → ask → allow)
#        hook exit 0 con JSON permissionDecision → BYPASA el sistema de permisos

set -euo pipefail

INPUT=$(cat)
TOOL_NAME=$(echo "$INPUT" | jq -r '.tool_name // empty')

[ -z "$TOOL_NAME" ] && exit 0

# Funcion helper: emitir ask con jq (escapado seguro)
emit_ask() {
  jq -n --arg reason "$1" '{
    hookSpecificOutput: {
      hookEventName: "PreToolUse",
      permissionDecision: "ask",
      permissionDecisionReason: $reason
    }
  }'
}

case "$TOOL_NAME" in
  Bash)
    CMD=$(echo "$INPUT" | jq -r '.tool_input.command // empty')
    [ -z "$CMD" ] && exit 0

    # SQL destructivo — sin \b (no portable en grep -E / macOS ERE)
    if echo "$CMD" | grep -qiE '(^|[^[:alnum:]_])(DROP[[:space:]]+(DATABASE|TABLE|VIEW|INDEX)|TRUNCATE[[:space:]]+TABLE|DELETE[[:space:]]+FROM|ALTER[[:space:]]+TABLE.*DROP|GRANT[[:space:]]+ALL|REVOKE)($|[^[:alnum:]_])'; then
      emit_ask "Operacion SQL destructiva detectada"
      exit 0
    fi

    # Pipe a shell — incluye variantes con env, xargs
    if echo "$CMD" | grep -qE '\|[[:space:]]*(env[[:space:]]+)?(sh|bash|zsh|dash)([[:space:]]|$)'; then
      emit_ask "Pipe a shell detectado"
      exit 0
    fi
    if echo "$CMD" | grep -qE '\|[[:space:]]*xargs[[:space:]].*((sh|bash|zsh)[[:space:]]+-c)'; then
      emit_ask "Pipe a xargs sh -c detectado"
      exit 0
    fi

    # Ejecucion indirecta: sh -c, bash -c, bash -lc
    if echo "$CMD" | grep -qE '(^|[^[:alnum:]_])(bash|sh|zsh)[[:space:]]+-[lc]+[[:space:]]'; then
      emit_ask "Ejecucion indirecta via sh -c"
      exit 0
    fi

    # Process substitution con descarga remota: source <(curl/wget ...)
    if echo "$CMD" | grep -qE '(<\(|source[[:space:]]+/dev/fd)'; then
      emit_ask "Process substitution detectado"
      exit 0
    fi
    ;;

  Write|Edit)
    FILE_PATH=$(echo "$INPUT" | jq -r '.tool_input.file_path // empty')
    [ -z "$FILE_PATH" ] && exit 0

    # Rutas de sistema (Linux + macOS)
    if echo "$FILE_PATH" | grep -qE '^/(etc|bin|sbin|usr/(bin|sbin|local/bin)|opt|var|lib|System|Library)/'; then
      emit_ask "Escritura a ruta de sistema: $FILE_PATH"
      exit 0
    fi

    # Archivos de credenciales/secretos por nombre
    if echo "$FILE_PATH" | grep -qiE '(\.env(\.[a-z]+)?|\.npmrc|\.pypirc|\.netrc|\.pgpass|credentials|\.htpasswd)$'; then
      emit_ask "Escritura a archivo de credenciales: $FILE_PATH"
      exit 0
    fi

    # Git internals
    if echo "$FILE_PATH" | grep -qE '/\.git/(hooks|config|objects|refs)/'; then
      emit_ask "Escritura a internals de git: $FILE_PATH"
      exit 0
    fi

    # CI/CD y deployment (patrones corregidos)
    if echo "$FILE_PATH" | grep -qiE '(\.github/workflows/|[Jj]enkinsfile|\.gitlab-ci\.ya?ml|[Dd]ockerfile|docker-compose(\.ya?ml)?|\.circleci/|terraform/|\.tf$|\.tfvars$|\.hcl$)'; then
      emit_ask "Escritura a archivo de CI/CD o infra: $FILE_PATH"
      exit 0
    fi
    ;;
esac

# Sin decision: dejar que settings.json maneje via allow/ask/deny
exit 0