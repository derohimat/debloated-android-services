package com.example

import com.example.data.security.PackageSecurityValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `package name validator accepts legitimate android package names`() {
        assertTrue(PackageSecurityValidator.isValidPackageName("com.example.app"))
        assertTrue(PackageSecurityValidator.isValidPackageName("com.android.chrome"))
        assertTrue(PackageSecurityValidator.isValidPackageName("com.samsung.android.bixby.agent"))
        assertTrue(PackageSecurityValidator.isValidPackageName("com.miui.msa.global"))
        assertTrue(PackageSecurityValidator.isValidPackageName("org.chromium.webview_shell"))
    }

    @Test
    fun `package name validator strictly rejects command injection and malicious inputs`() {
        // Command injection vectors
        assertFalse(PackageSecurityValidator.isValidPackageName("com.android.chrome; rm -rf /"))
        assertFalse(PackageSecurityValidator.isValidPackageName("com.example.app && reboot"))
        assertFalse(PackageSecurityValidator.isValidPackageName("com.example.app | cat /etc/passwd"))
        assertFalse(PackageSecurityValidator.isValidPackageName("com.example.`id`"))
        assertFalse(PackageSecurityValidator.isValidPackageName("com.example.$(whoami)"))
        assertFalse(PackageSecurityValidator.isValidPackageName("com.example.app > /sdcard/pwn"))

        // Malformed inputs
        assertFalse(PackageSecurityValidator.isValidPackageName(""))
        assertFalse(PackageSecurityValidator.isValidPackageName("   "))
        assertFalse(PackageSecurityValidator.isValidPackageName(null))
        assertFalse(PackageSecurityValidator.isValidPackageName("singleword"))
        assertFalse(PackageSecurityValidator.isValidPackageName("com..double.dot"))
        assertFalse(PackageSecurityValidator.isValidPackageName("com.example.app\nnewline"))
        assertFalse(PackageSecurityValidator.isValidPackageName("com.example/traversal"))
    }

    @Test
    fun `isSafeCommand allows legitimate debloat commands and blocks dangerous shell commands`() {
        // Safe commands
        assertTrue(PackageSecurityValidator.isSafeCommand("pm disable-user --user 0 com.facebook.katana"))
        assertTrue(PackageSecurityValidator.isSafeCommand("pm uninstall -k --user 0 com.samsung.android.bixby"))
        assertTrue(PackageSecurityValidator.isSafeCommand("pm enable com.google.android.youtube"))
        assertTrue(PackageSecurityValidator.isSafeCommand("cmd package install-existing com.google.android.youtube"))
        assertTrue(PackageSecurityValidator.isSafeCommand("adb shell pm disable-user --user 0 com.facebook.katana"))
        assertTrue(PackageSecurityValidator.isSafeCommand("pm enable com.app || cmd package install-existing com.app"))

        // Blocked dangerous commands
        assertFalse(PackageSecurityValidator.isSafeCommand("rm -rf /data/data"))
        assertFalse(PackageSecurityValidator.isSafeCommand("pm disable-user --user 0 com.app; reboot"))
        assertFalse(PackageSecurityValidator.isSafeCommand("pm disable-user --user 0 com.app `whoami`"))
        assertFalse(PackageSecurityValidator.isSafeCommand("cat /system/build.prop > /sdcard/stolen.txt"))
        assertFalse(PackageSecurityValidator.isSafeCommand("nc -l -p 4444 -e /bin/sh"))
    }

    @Test
    fun `sanitizeText strips terminal escape codes and control characters`() {
        val dirty = "Normal Text\u001B[31mRed Text\u001B[0m\u0000\u0007"
        val clean = PackageSecurityValidator.sanitizeText(dirty, 100)
        assertEquals("Normal TextRed Text", clean)
    }
}
