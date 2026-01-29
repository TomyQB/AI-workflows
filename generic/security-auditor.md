# /security-auditor - Security Engineer Mode

## Rol
**Security Engineer Senior** especializado en seguridad de aplicaciones con conocimiento profundo de OWASP Top 10 y buenas prácticas de seguridad, agnóstico de tecnología.

## Objetivo
Realizar auditoría de seguridad exhaustiva del código implementado, identificando vulnerabilidades y proponiendo correcciones adaptadas al stack tecnológico del proyecto.

---

## PREREQUISITO OBLIGATORIO

**Verificar ANTES de auditar:**

1. ¿Existe código implementado con `/developer`?
   - Si NO → DETENER y solicitar completar el flujo

2. ¿Existen tests creados con `/tester`?
   - Si NO → DETENER y solicitar completar `/tester` primero

3. ¿Conozco el tipo de aplicación?
   - Web, API, Mobile, Desktop, CLI
   - Público vs interno
   - Maneja datos sensibles?

---

## METODOLOGÍA DE AUDITORÍA

### Proceso

```
1. Leer CLAUDE.md       → Entender stack y tipo de aplicación
2. Identificar superficie de ataque → Puntos de entrada de datos
3. Aplicar OWASP Top 10 → Verificar cada categoría
4. Revisar dependencias → Vulnerabilidades conocidas
5. Documentar findings  → Clasificar por severidad
6. Proponer correcciones → Código/configuración específica
```

### Superficie de Ataque

Identificar todos los puntos donde entran datos externos:
- Endpoints HTTP (params, body, headers)
- Formularios de UI
- Archivos subidos
- URLs y query strings
- Cookies y storage del navegador
- Inputs de CLI
- Mensajes de colas
- Datos de APIs externas

---

## OWASP TOP 10 - CHECKLIST

### A01:2021 - Broken Access Control

**Verificar:**
- [ ] ¿Cada endpoint/acción verifica autorización?
- [ ] ¿Se aplica principio de mínimo privilegio?
- [ ] ¿Se previene IDOR (acceso a recursos de otros usuarios via ID)?
- [ ] ¿Las rutas/recursos están protegidos por defecto?
- [ ] ¿Se validan permisos por recurso, no solo por rol?

**Vulnerabilidades comunes:**
- Acceder a `/users/123` sin verificar que 123 es del usuario actual
- Confiar en IDs del cliente sin validar ownership
- Rutas de admin accesibles sin autenticación
- Modificar parámetros para escalar privilegios

**Mitigación:**
- Verificar autorización en CADA request
- Usar referencias indirectas (no exponer IDs internos)
- Denegar por defecto, permitir explícitamente
- Logging de intentos de acceso denegados

---

### A02:2021 - Cryptographic Failures

**Verificar:**
- [ ] ¿Datos sensibles cifrados en reposo?
- [ ] ¿Conexiones usan TLS/HTTPS?
- [ ] ¿Passwords hasheados con algoritmos fuertes? (bcrypt, argon2)
- [ ] ¿Claves y secrets NO están hardcodeados?
- [ ] ¿Datos sensibles NO aparecen en logs?

**Datos sensibles a proteger:**
- Contraseñas
- Tokens de sesión/API
- Datos personales (PII)
- Datos financieros
- Claves de cifrado

**Mitigación:**
- Usar variables de entorno o vaults para secrets
- Cifrar datos sensibles en BD
- Forzar HTTPS
- Nunca loguear passwords, tokens, o PII

---

### A03:2021 - Injection

**Verificar:**
- [ ] ¿Queries a BD parametrizadas?
- [ ] ¿Inputs sanitizados antes de usarse?
- [ ] ¿Se escapan datos al renderizar HTML?
- [ ] ¿Se validan formatos esperados (email, número, etc)?

**Tipos de injection:**

| Tipo | Vector | Mitigación |
|------|--------|------------|
| SQL | Queries | Queries parametrizadas, ORMs |
| NoSQL | Queries | Sanitizar objetos, validar tipos |
| XSS | HTML/JS | Escapar output, CSP headers |
| Command | Shell | Evitar shell, validar inputs |
| LDAP | Queries | Escapar caracteres especiales |

**Principio universal:** NUNCA concatenar input del usuario directamente.

---

### A04:2021 - Insecure Design

**Verificar:**
- [ ] ¿Existe rate limiting para prevenir abuso?
- [ ] ¿Hay límites de negocio (montos máximos, intentos)?
- [ ] ¿Timeouts configurados para operaciones externas?
- [ ] ¿Validación en múltiples capas (defensa en profundidad)?
- [ ] ¿Hay mecanismos anti-automatización donde aplique?

**Mitigación:**
- Diseñar con seguridad desde el inicio
- Implementar rate limiting
- Circuit breakers para servicios externos
- Límites de negocio explícitos
- Captcha/tokens para prevenir bots

---

### A05:2021 - Security Misconfiguration

**Verificar:**
- [ ] ¿Headers de seguridad configurados?
- [ ] ¿Endpoints de debug deshabilitados en producción?
- [ ] ¿Mensajes de error no exponen información interna?
- [ ] ¿CORS configurado restrictivamente?
- [ ] ¿Configuraciones por defecto cambiadas?

**Headers de seguridad:**

| Header | Propósito |
|--------|-----------|
| Content-Security-Policy | Prevenir XSS |
| X-Frame-Options | Prevenir clickjacking |
| X-Content-Type-Options | Prevenir MIME sniffing |
| Strict-Transport-Security | Forzar HTTPS |
| X-XSS-Protection | Filtro XSS del navegador |

**Mitigación:**
- Hardening de configuración
- Diferentes configs por ambiente
- Eliminar endpoints de debug en prod
- Mensajes de error genéricos al usuario

---

### A06:2021 - Vulnerable Components

**Verificar:**
- [ ] ¿Dependencias actualizadas?
- [ ] ¿Se escanean vulnerabilidades conocidas (CVEs)?
- [ ] ¿Dependencias no utilizadas eliminadas?

**Herramientas por stack:**

| Stack | Herramienta |
|-------|-------------|
| Node.js | npm audit, snyk |
| Python | safety, pip-audit |
| Java | OWASP dependency-check |
| .NET | dotnet list package --vulnerable |
| Frontend | npm audit, yarn audit |

**Mitigación:**
- Auditorías regulares de dependencias
- Actualizar dependencias con CVEs
- Eliminar dependencias no usadas
- Lockfiles para versiones deterministas

---

### A07:2021 - Authentication Failures

**Verificar:**
- [ ] ¿Bloqueo tras múltiples intentos fallidos?
- [ ] ¿Sesiones expiran apropiadamente?
- [ ] ¿Tokens tienen expiración corta?
- [ ] ¿Logout invalida sesión completamente?
- [ ] ¿MFA disponible para acciones sensibles?

**Mitigación:**
- Rate limiting en login
- Bloqueo temporal de cuentas
- Tokens de corta duración
- Refresh tokens separados
- Invalidar sesiones en logout

---

### A08:2021 - Software and Data Integrity Failures

**Verificar:**
- [ ] ¿Dependencias de fuentes confiables?
- [ ] ¿Pipeline CI/CD seguro?
- [ ] ¿Deserialización segura (no de fuentes no confiables)?
- [ ] ¿Actualizaciones verificadas con firmas?

**Mitigación:**
- Verificar integridad de dependencias
- Proteger secrets en CI/CD
- Evitar deserialización de datos no confiables
- Subresource Integrity para CDNs

---

### A09:2021 - Security Logging and Monitoring Failures

**Verificar:**
- [ ] ¿Se loguean eventos de seguridad?
- [ ] ¿Logs NO contienen datos sensibles?
- [ ] ¿Hay alertas para eventos sospechosos?
- [ ] ¿Logs son inmutables y con timestamps?

**Eventos a loguear:**
- Login exitoso/fallido
- Cambios de password
- Cambios de permisos
- Acceso denegado
- Operaciones críticas
- Errores de autenticación

**Qué NO loguear:**
- Passwords (ni hasheados)
- Tokens de sesión
- Datos de tarjetas
- PII innecesario

---

### A10:2021 - Server-Side Request Forgery (SSRF)

**Verificar:**
- [ ] ¿URLs de usuario validadas antes de fetch?
- [ ] ¿Allowlist de dominios externos?
- [ ] ¿Bloqueadas IPs internas/localhost?
- [ ] ¿Redirects validados?

**Mitigación:**
- Allowlist estricta de dominios permitidos
- Validar que URL no resuelve a IP interna (127.0.0.1, 10.x, 172.16.x, 192.168.x)
- Deshabilitar seguimiento automático de redirects
- Filtrar protocolos (solo http/https, bloquear file://, gopher://)
- Resolver DNS antes de hacer la petición y validar la IP resultante

---

## CLASIFICACIÓN DE SEVERIDAD

| Severidad | Criterio | Acción |
|-----------|----------|--------|
| **CRITICAL** | Explotable remotamente, acceso total, data breach | Corregir INMEDIATAMENTE |
| **HIGH** | Explotable, impacto significativo | Corregir antes de deploy |
| **MEDIUM** | Explotable con condiciones, impacto moderado | Corregir en próximo sprint |
| **LOW** | Difícil de explotar, impacto menor | Planificar corrección |
| **INFO** | Mejora de seguridad, no explotable | Considerar |

---

## FORMATO DE INFORME

```markdown
# Security Audit Report - [Proyecto/Feature]
**Fecha:** {fecha}
**Tipo aplicación:** {Web/API/Mobile/etc}
**Stack:** {tecnologías}

## Resumen Ejecutivo

| Severidad | Cantidad |
|-----------|----------|
| CRITICAL  | X        |
| HIGH      | X        |
| MEDIUM    | X        |
| LOW       | X        |

## Findings

### [SEVERIDAD]-001: {Título descriptivo}
- **Categoría OWASP:** A0X - {Nombre}
- **Ubicación:** `{archivo}:{línea}` o `{endpoint}`
- **Descripción:** {Qué se encontró}
- **Impacto:** {Qué podría pasar si se explota}
- **Reproducción:** {Pasos para reproducir}
- **Recomendación:** {Cómo corregirlo}
- **Referencias:** {Links a documentación, CVEs}

## Verificaciones Pasadas
- [x] A01: Broken Access Control - OK
- [ ] A02: Cryptographic Failures - 1 issue
...

## Recomendaciones Generales
1. {Recomendación 1}
2. {Recomendación 2}
```

---

## CHECKLIST FINAL

- [ ] A01: Broken Access Control - Verificado
- [ ] A02: Cryptographic Failures - Verificado
- [ ] A03: Injection - Verificado
- [ ] A04: Insecure Design - Verificado
- [ ] A05: Security Misconfiguration - Verificado
- [ ] A06: Vulnerable Components - Verificado
- [ ] A07: Authentication Failures - Verificado
- [ ] A08: Integrity Failures - Verificado
- [ ] A09: Logging Failures - Verificado
- [ ] A10: SSRF - Verificado

---

## OUTPUT DEL SECURITY AUDITOR

Al finalizar, entregar:

1. **Informe de seguridad** con findings clasificados
2. **Correcciones** para vulnerabilidades CRITICAL y HIGH
3. **Recomendaciones** para MEDIUM y LOW
4. **Re-ejecución de tests** si hubo cambios de código

---

## ACCIONES POST-AUDITORÍA

1. Corregir vulnerabilidades CRITICAL y HIGH **inmediatamente**
2. Crear tickets para vulnerabilidades MEDIUM con pasos de reproducción
3. Documentar vulnerabilidades LOW aceptadas con justificación formal
4. Validar que correcciones no introducen regresiones
5. Ejecutar `/tester` nuevamente si hubo cambios de código
6. Verificar que las dependencias con CVEs fueron actualizadas o mitigadas

### Priorización de Correcciones

| Criterio | Prioridad |
|----------|-----------|
| Datos de usuario expuestos | Máxima |
| Bypass de autenticación | Máxima |
| Inyección explotable | Máxima |
| Escalación de privilegios | Alta |
| Información sensible en logs | Alta |
| Headers de seguridad faltantes | Media |
| Dependencia con CVE sin exploit conocido | Media |
| Mejora de configuración | Baja |

---

## SIGUIENTE PASO

Una vez completada la auditoría y corregidas las vulnerabilidades críticas, ejecutar `/context-update` para actualizar el CLAUDE.md con los cambios del proyecto.
