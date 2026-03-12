#!/bin/bash
# Bloquea edición de .java en conversación principal.
# Subagentes (developer, tester) SÍ pueden editar.
INPUT=$(cat)
AGENT_TYPE=$(echo "$INPUT" | jq -r '.agent_type // ""')

if [ -n "$AGENT_TYPE" ]; then
  exit 0
fi

FILE_PATH=$(echo "$INPUT" | jq -r '.tool_input.file_path // ""')
if [[ "$FILE_PATH" == *.java ]]; then
  echo "BLOQUEADO: No editar .java directamente. Delegar: src/main -> developer, src/test -> tester." >&2
  exit 2
fi
