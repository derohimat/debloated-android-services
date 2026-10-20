# Security Policy 🛡️

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 1.0.x   | :white_check_mark: |

---

## Security Architecture & Hardening

DebloatMaster is built with zero-trust local execution principles to safeguard user devices:

### 1. Zero Network Leakage & Telemetry
- **No Third-Party Analytics / Tracking**: DebloatMaster contains zero tracking SDKs, ad networks, or telemetry agents.
- **Strict Network Policy**: All cleartext HTTP traffic is disabled globally (`android:usesCleartextTraffic="false"`) with explicit system certificate authority anchoring (`network_security_config.xml`).
- **Local Storage Exclusivity**: Snapshots and audit history reside strictly in private local application storage (`Context.MODE_PRIVATE`).

### 2. Command Injection & Metacharacter Defense
- All system package identifiers are strictly validated against the official Android package specification (`^[a-zA-Z_][a-zA-Z0-9_]*(\.[a-zA-Z_][a-zA-Z0-9_]*)+$`) before generating or executing any ADB shell commands.
- Shell metacharacters (`;`, `&`, `|`, `` ` ``, `$()`, `>`, `<`, newlines) and ANSI escape sequences are blocked and stripped to eliminate command injection risks via imported snapshots or input fields.
- Crucial system packages (`com.android.systemui`, `com.android.settings`, `com.android.permissioncontroller`, etc.) are classified as `ESSENTIAL` and shielded from debloat actions.

### 3. Bytecode Obfuscation & Binary Hardening
- Release builds utilize Android R8 / ProGuard minification (`isMinifyEnabled = true`, `isShrinkResources = true`).
- Sensitive debug logs (`android.util.Log.d`, `Log.v`) are automatically stripped in production binaries.

### 4. Git & Secret Protection
- Key material (`*.jks`, `*.keystore`), signing configs, `.env` files, and local properties are strictly excluded from source control via `.gitignore`.
- Release signing credentials are read exclusively from environment variables (`STORE_PASSWORD`, `KEY_PASSWORD`, `KEYSTORE_PATH`) without hardcoding.

---

## Reporting a Vulnerability

If you discover a security vulnerability or exploit vector within DebloatMaster:

1. **Do not open a public issue.**
2. Send an email to the repository maintainer detailing:
   - Nature of the vulnerability (e.g. bypass of safety checks, command parsing edge case)
   - Steps to reproduce or proof-of-concept payload
   - Impact assessment
3. You will receive an acknowledgment within 48 hours and updates as patches are verified and released.
