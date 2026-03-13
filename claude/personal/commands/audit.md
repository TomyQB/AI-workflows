Realiza una auditoría de seguridad exhaustiva de todos los contratos Solidity en `src/`.

## Proceso
1. Lee cada contrato en `src/` y analiza línea por línea
2. Lee cada test en `test/` para verificar cobertura de seguridad
3. Revisa contra TODAS las categorías de vulnerabilidades del checklist completo
4. Ejecuta `forge test -vvv` para verificar que los tests cubren los escenarios de seguridad
5. Genera reporte con hallazgos y resumen

---

## Checklist Completo de Seguridad

### A. Reentrancy & Interacciones Externas
- **SWC-107 Reentrancy**: ¿Se sigue Checks-Effects-Interactions (CEI) en TODAS las funciones? ¿Se actualiza el estado antes de llamadas externas?
- **SWC-112 Delegatecall a contratos no confiables**: ¿Se usa `delegatecall`? ¿El target es trusted?
- **SWC-113 DoS con llamada externa fallida**: ¿Puede una llamada externa fallar y bloquear toda la función? ¿Se usa pull over push para pagos?
- **SWC-134 Message call con gas hardcodeado**: ¿Se usa `.transfer()` o `.send()` con límite de 2300 gas? Puede fallar post-EIP-1884.
- **Cross-function reentrancy**: ¿Se puede reentrar por otra función que comparte el mismo estado?
- **Read-only reentrancy**: ¿Funciones `view` leen estado inconsistente durante una llamada externa en progreso?

### B. Control de Acceso & Autorización
- **SWC-105 Funciones sin protección**: ¿Todas las funciones que modifican estado sensible tienen access control?
- **SWC-115 Uso de tx.origin**: ¿Se usa `tx.origin` para autorización? (NUNCA debe usarse)
- **SWC-106 Función selfdestruct sin proteger**: ¿Existe `selfdestruct` accesible por cualquiera?
- **Centralización excesiva**: ¿Un solo address tiene poder absoluto? ¿Se necesita multisig o timelock?
- **Modifier bypass**: ¿Los modifiers cubren TODOS los paths? ¿Se pueden bypassear con inputs edge case?
- **Default visibility**: ¿Todas las funciones tienen visibilidad explícita?
- **Missing zero-address check**: ¿Se validan addresses contra `address(0)` en constructors y setters?

### C. Aritmética & Tipos de Datos
- **SWC-101 Integer overflow/underflow**: ¿Se usa Solidity >=0.8.0 con checked arithmetic? ¿Se usa `unchecked` de forma segura?
- **SWC-129 Tipografía con type(uint).max**: ¿Se depende de valores máximos/mínimos de tipo sin validar?
- **Truncamiento en división**: ¿La división entera puede causar pérdida de precisión significativa?
- **Casting inseguro**: ¿Hay downcast de uint256 a uint128/uint64 sin validar rango? (data loss)
- **Signed/unsigned confusion**: ¿Se mezclan tipos `int` y `uint` sin cuidado?
- **SWC-101 Negación de int256.min**: ¿Se niega `type(int256).min`? (overflow porque `|min| > max`)

### D. Datos & Storage
- **SWC-109 Variable no inicializada**: ¿Hay variables de storage sin inicializar que asumen valor zero?
- **SWC-124 Write to arbitrary storage**: ¿Se puede escribir en slots de storage arbitrarios?
- **SWC-119 Variable shadowing**: ¿Variables locales ocultan state variables del mismo nombre?
- **Storage collision en proxies**: ¿Se sigue EIP-1967 para slots de storage en contratos upgradeables?
- **Transient storage misuse**: ¿Se usa `tstore`/`tload` de forma segura (EIP-1153)?
- **Datos sensibles on-chain**: ¿Se almacenan passwords, claves privadas, o datos confidenciales? (todo storage es público)
- **Struct packing**: ¿Se optimizan structs para minimizar slots de storage?

### E. Validación de Inputs & Returns
- **SWC-104 Unchecked call return value**: ¿Se verifican return values de `.call()`, `.send()`, `.transfer()`?
- **SWC-126 Insufficient gas griefing**: ¿Se puede hacer grief limitando gas en subcalls?
- **Missing input validation**: ¿Se validan TODOS los inputs en los boundaries del sistema?
- **Array out of bounds**: ¿Se acceden arrays con índices sin validar?
- **Empty bytes/string**: ¿Se manejan correctamente inputs vacíos?
- **ABI encoding edge cases**: ¿Se usa `abi.encodePacked` con tipos dinámicos? (riesgo de collision, usar `abi.encode`)

### F. Compilador & Pragma
- **SWC-103 Floating pragma**: ¿El pragma está anclado a una versión específica (`^0.8.28` o `0.8.28`)?
- **SWC-102 Outdated compiler**: ¿Se usa una versión de solc con bugs conocidos?
- **Compiler optimizations**: ¿Los optimizer runs son apropiados para el uso del contrato?
- **Known solc bugs**: Verificar contra la lista de bugs del compilador para la versión usada.

### G. Criptografía & Aleatoriedad
- **SWC-120 Weak randomness**: ¿Se usa `block.timestamp`, `block.number`, `blockhash` para aleatoriedad?
- **SWC-117 Signature malleability**: ¿Se acepta la misma firma con `s` alto y bajo? ¿Se usa `ecrecover` sin OpenZeppelin ECDSA?
- **SWC-122 Falta de protección contra replay**: ¿Las firmas incluyen nonce, chainId, y contract address?
- **Hash collision con encodePacked**: ¿Se usa `abi.encodePacked` con múltiples tipos dinámicos?
- **Preimage attacks**: ¿Se depende de que un hash no sea reversible para seguridad crítica?

### H. Denegación de Servicio (DoS)
- **SWC-128 DoS con gas limit**: ¿Hay loops sin upper bound fijo? ¿Pueden crecer indefinidamente?
- **SWC-113 DoS por dependencia externa**: ¿Una llamada externa fallida puede bloquear funciones críticas?
- **Block stuffing**: ¿Se depende de que una transacción se ejecute en un bloque específico?
- **Self-destruct DoS**: ¿Se puede forzar ETH al contrato via `selfdestruct` y romper invariantes de balance?
- **Unbounded return data**: ¿Se copia return data sin límite de tamaño?

### I. Front-running & MEV
- **Transaction ordering dependence**: ¿El resultado depende del orden de transacciones?
- **Sandwich attacks**: ¿Operaciones de swap/price son vulnerables a sandwich?
- **Commit-reveal missing**: ¿Hay subastas, votaciones o juegos sin esquema commit-reveal?
- **Slippage protection**: ¿Hay slippage checks en operaciones con precios?
- **Deadline missing**: ¿Transacciones pueden ejecutarse mucho después de ser enviadas?

### J. Patrones de Tokens (si aplica)
- **ERC-20 compliance**: ¿Se siguen todos los requisitos del estándar? ¿`approve` race condition?
- **SafeERC20**: ¿Se usa SafeERC20 para interactuar con tokens externos? (tokens que no retornan bool)
- **ERC-721/1155 callbacks**: ¿Se implementan `onERC721Received`/`onERC1155Received` donde es necesario?
- **Rebasing/fee-on-transfer tokens**: ¿Se manejan tokens con balance dinámico?
- **Infinite approval risks**: ¿Se permite `approve(type(uint256).max)` sin control?

### K. Gobernanza & Upgradeability (si aplica)
- **Proxy initialization**: ¿Se llama `initialize()` en el mismo tx que el deploy? ¿Se protege contra re-initialization?
- **Storage layout compatibility**: ¿Las upgrades mantienen el layout de storage compatible?
- **Function selector clashing**: ¿Hay colisión de selectors entre proxy y implementation?
- **Timelock on critical changes**: ¿Cambios críticos tienen delay para dar tiempo de reacción?
- **Emergency pause**: ¿Existe mecanismo de pausa para emergencias?

### L. Lógica de Negocio
- **Invariantes rotas**: ¿Se mantienen las invariantes del contrato en todos los paths de ejecución?
- **Race conditions entre funciones**: ¿El orden de llamadas puede dejar el contrato en estado inconsistente?
- **Edge cases con zero values**: ¿Las funciones manejan correctamente amount=0, address(0), arrays vacíos?
- **Precision loss accumulation**: ¿La pérdida de precisión se acumula en operaciones repetidas?
- **Flash loan attacks**: ¿Se puede manipular estado en una sola transacción con flash loans?

### M. Gas & Optimización (impacto en seguridad)
- **Out of gas en loops**: ¿Loops pueden exceder el gas limit del bloque?
- **Returnbomb attack**: ¿Se copia return data de llamadas externas sin limitar tamaño?
- **Memory expansion**: ¿Se aloca memoria de forma descontrolada?

---

## Formato de Reporte

Para cada hallazgo:
- **ID**: AUD-XXX
- **Severidad**: Critical / High / Medium / Low / Informational / Gas
- **Categoría**: (sección del checklist: A-M)
- **Ubicación**: `archivo:línea`
- **Descripción**: Qué es el problema y por qué es un riesgo
- **Prueba de concepto**: Cómo se podría explotar (si aplica)
- **Recomendación**: Código o patrón para solucionarlo
- **Referencia**: SWC-ID o fuente externa

## Resumen Final
1. Tabla con conteo de hallazgos por severidad
2. Puntuación general de seguridad: Secure / Mostly Secure / Needs Attention / Critical Issues
3. Lista de tests de seguridad faltantes
4. Recomendaciones generales de mejora
