Realiza una revisión de código profesional de todos los contratos en `src/` y tests en `test/`.

## Proceso

### 1. Style Guide Compliance
Revisa contra el Solidity Style Guide oficial:
- Naming conventions (CapWords, mixedCase, UPPER_CASE, _prefix)
- Element ordering dentro del contrato (state vars → events → errors → modifiers → constructor → functions por visibilidad)
- Function declaration order (visibility → mutability → virtual → override → custom)
- Formatting (4 spaces, 120 chars max, double quotes)
- NatSpec en todas las funciones public/external

### 2. Security Patterns
- Checks-Effects-Interactions en cada función
- Access control explícito en funciones sensibles
- Custom errors en lugar de require con strings
- No tx.origin, no variable shadowing, no floating pragma

### 3. Gas Optimization
- `external` vs `public` (¿se llama internamente?)
- `immutable` para variables set en constructor que no cambian
- `constant` para valores conocidos en compilación
- Cache de storage reads en variables locales
- Named returns donde simplifique

### 4. Test Quality
- Cobertura: happy path + edge cases + reverts + events
- Naming convention: `test_function_scenario`
- Fuzz tests para funciones con inputs amplios
- Tests de access control para funciones restringidas

### 5. Ejecuta verificación
- `forge fmt --check`
- `forge build --sizes`
- `forge test`

### 6. Reporte
Para cada hallazgo:
- **Categoría**: Style / Security / Gas / Testing
- **Severidad**: Must Fix / Should Fix / Nice to Have
- **Ubicación**: archivo:línea
- **Problema**: descripción concisa
- **Sugerencia**: código o patrón correcto
