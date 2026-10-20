package com.example.data.security

/**
 * Security validation utility to prevent command injection, directory traversal,
 * and malicious input vectors when handling Android package names, snapshots, and terminal commands.
 */
object PackageSecurityValidator {

    /**
     * Strict Android package name specification:
     * - Must start with a letter (uppercase or lowercase) or underscore.
     * - May contain letters, digits, and underscores.
     * - Must contain at least one dot separator.
     * - Each period-separated segment must begin with a letter or underscore.
     * - Strictly prohibits shell metacharacters: ; & | ` $ \ " ' < > ( ) \n \r \t
     */
    private val PACKAGE_NAME_REGEX = Regex("^[a-zA-Z_][a-zA-Z0-9_]*(\\.[a-zA-Z_][a-zA-Z0-9_]*)+$")

    /**
     * Checks if a package name strictly conforms to Android's package naming format.
     */
    fun isValidPackageName(packageName: String?): Boolean {
        if (packageName.isNullOrBlank() || packageName.length > 255) {
            return false
        }
        return PACKAGE_NAME_REGEX.matches(packageName.trim())
    }

    /**
     * Asserts that a package name is safe, throwing an [IllegalArgumentException] if invalid.
     */
    fun requireValidPackageName(packageName: String): String {
        require(isValidPackageName(packageName)) {
            "Security Exception: Untrusted or invalid package identifier '$packageName'."
        }
        return packageName.trim()
    }

    /**
     * Sanitizes plain text fields (e.g. user-supplied snapshot names or notes)
     * by stripping ANSI escape sequences, control characters, and limiting length.
     */
    fun sanitizeText(input: String?, maxLength: Int = 128): String {
        if (input.isNullOrBlank()) return ""
        return input
            .replace(Regex("\u001B\\[[;?0-9]*[a-zA-Z]"), "")
            .replace(Regex("[\\p{Cntrl}&&[^\r\n\t]]"), "")
            .trim()
            .take(maxLength)
    }

    /**
     * Validates that a shell command string matches strictly whitelisted safe debloat operations
     * before execution in a root or local shell.
     */
    fun isSafeCommand(command: String): Boolean {
        val trimmed = command.trim()
        if (trimmed.isBlank() || trimmed.length > 1024) return false

        // Check for shell metacharacters that enable arbitrary command execution
        if (trimmed.contains("`") || trimmed.contains("$(") || trimmed.contains(">") ||
            trimmed.contains("<") || trimmed.contains("\n") || trimmed.contains("\r")
        ) {
            return false
        }

        // Commands may use standard chaining (||, &&, ;) only if each individual segment is a valid debloat command
        val subCommands = if (trimmed.contains("||")) {
            trimmed.split("||").map { it.trim() }
        } else if (trimmed.contains("&&")) {
            trimmed.split("&&").map { it.trim() }
        } else if (trimmed.contains(";")) {
            trimmed.split(";").map { it.trim() }
        } else {
            listOf(trimmed)
        }

        for (subCmd in subCommands) {
            if (!isValidDebloatSubCommand(subCmd)) {
                return false
            }
        }
        return true
    }

    private fun isValidDebloatSubCommand(subCmd: String): Boolean {
        val parts = subCmd.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (parts.isEmpty()) return false

        return when {
            // "pm disable-user --user 0 <pkg>"
            parts.size == 5 && parts[0] == "pm" && parts[1] == "disable-user" && parts[2] == "--user" && parts[3] == "0" -> {
                isValidPackageName(parts[4])
            }
            // "pm uninstall -k --user 0 <pkg>"
            parts.size == 6 && parts[0] == "pm" && parts[1] == "uninstall" && parts[2] == "-k" && parts[3] == "--user" && parts[4] == "0" -> {
                isValidPackageName(parts[5])
            }
            // "pm enable <pkg>"
            parts.size == 3 && parts[0] == "pm" && parts[1] == "enable" -> {
                isValidPackageName(parts[2])
            }
            // "cmd package install-existing <pkg>"
            parts.size == 4 && parts[0] == "cmd" && parts[1] == "package" && parts[2] == "install-existing" -> {
                isValidPackageName(parts[3])
            }
            // "adb shell <validSubCommand>"
            parts.size >= 3 && parts[0] == "adb" && parts[1] == "shell" -> {
                val sub = parts.drop(2).joinToString(" ")
                isValidDebloatSubCommand(sub)
            }
            else -> false
        }
    }
}
