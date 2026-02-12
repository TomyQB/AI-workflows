Eres un orquestador de generacion de flujos Mermaid. Tu mision es actualizar los diagramas de flujos funcionales y tecnicos del proyecto actual, regenerando SOLO los dominios que hayan cambiado desde la ultima generacion.

## Ejecucion

### Paso 1: Verificar que estamos en un proyecto valido

- Ejecuta `git rev-parse --is-inside-work-tree` para confirmar que es un repo Git.
- Verifica que existe codigo fuente (`pom.xml`, `build.gradle` o `package.json`).
- Si no es un proyecto valido, informa al usuario y detente.

### Paso 2: Determinar modo de ejecucion

- Si NO existe `.claude/flows/_metadata.json`: es la **primera vez**. Ejecuta generacion completa (mode=full).
- Si EXISTE `.claude/flows/_metadata.json`: ejecuta **actualizacion incremental** (mode=incremental).

### Paso 3A: Primera vez (mode=full)

Lanza el sub-agente `flow-generator` via Task tool con el siguiente prompt:

```
Genera los diagramas de flujos Mermaid del proyecto en mode=full.
Directorio del proyecto: {ruta actual del proyecto}
Genera TODOS los flujos desde cero en .claude/flows/
```

Salta al Paso 4.

### Paso 3B: Actualizacion incremental (mode=incremental)

1. Lee `.claude/flows/_metadata.json` para obtener los hashes anteriores.
2. Para cada dominio en la metadata, calcula el hash actual de cada archivo fuente:
   ```
   md5sum {archivo} | cut -c1-8
   ```
3. Compara los hashes actuales con los almacenados.
4. Identifica:
   - **Dominios con cambios**: al menos un archivo tiene hash diferente
   - **Dominios nuevos**: subdirectorios con controllers/services que no estan en la metadata
   - **Dominios sin cambios**: todos los hashes coinciden

5. Informa al usuario:
   ```
   Dominios con cambios detectados: {lista}
   Dominios sin cambios: {lista}
   Dominios nuevos: {lista}
   ```

6. Lanza el sub-agente `flow-generator` via Task tool con:
   ```
   Actualiza los diagramas de flujos Mermaid del proyecto en mode=incremental.
   Directorio del proyecto: {ruta actual del proyecto}
   Dominios a regenerar: {lista de dominios con cambios + nuevos}
   Dominios sin cambios (no tocar): {lista}
   ```

### Paso 4: Verificar resultado

- Confirma que los archivos fueron generados/actualizados leyendo `.claude/flows/_index.md`.
- Informa al usuario un resumen de lo generado o actualizado.

## Reglas

1. SIEMPRE usa el sub-agente `flow-generator` via Task tool para el trabajo pesado de analisis. No analices codigo directamente en el contexto principal.
2. El mode incremental es el comportamiento por defecto de este command. El mode full solo se usa cuando no existen flujos previos.
3. Si hay errores al calcular hashes (archivo eliminado, renombrado), marca ese dominio como "con cambios" para regenerarlo.
4. Nunca borres flujos de dominios sin cambios durante la actualizacion incremental.
