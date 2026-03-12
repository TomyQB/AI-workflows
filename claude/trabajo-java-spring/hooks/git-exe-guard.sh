#!/bin/bash
# Bloquea uso de 'git' (Linux) forzando 'git.exe' (Windows).
INPUT=$(cat)
COMMAND=$(echo "$INPUT" | jq -r '.tool_input.command // ""')

if echo "$COMMAND" | grep -qP '(?<!\w)git\s' && ! echo "$COMMAND" | grep -q 'git\.exe'; then
  echo "BLOQUEADO: Usar git.exe en vez de git. Linux git causa problemas de CRLF/LF." >&2
  exit 2
fi
