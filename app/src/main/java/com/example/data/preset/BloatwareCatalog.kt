package com.example.data.preset

import com.example.data.model.BloatCategory
import com.example.data.model.BloatPackage
import com.example.data.model.DeviceManufacturer
import com.example.data.model.SafetyLevel

object BloatwareCatalog {

    val curatedPackages: List<BloatPackage> = listOf(
        // === TELEMETRY & ADVERTISING (SAFE) ===
        BloatPackage(
            packageName = "com.miui.msa.global",
            displayName = "MIUI System Ads (MSA)",
            category = BloatCategory.TELEMETRY,
            safetyLevel = SafetyLevel.SAFE,
            targetManufacturer = DeviceManufacturer.XIAOMI,
            description = "Main advertising daemon responsible for serving push ads and sponsored banners inside Xiaomi system apps.",
            ramSavingMb = 45,
            riskNote = "Safe to disable. Completely removes ad injections in system apps."
        ),
        BloatPackage(
            packageName = "com.miui.analytics",
            displayName = "Xiaomi Analytics Agent",
            category = BloatCategory.TELEMETRY,
            safetyLevel = SafetyLevel.SAFE,
            targetManufacturer = DeviceManufacturer.XIAOMI,
            description = "Logs app usage metrics, system telemetry, and background tracking.",
            ramSavingMb = 55,
            riskNote = "Safe to disable. Drastically cuts down telemetry network calls."
        ),
        BloatPackage(
            packageName = "com.samsung.android.knox.analytics.uploader",
            displayName = "Samsung Knox Analytics",
            category = BloatCategory.TELEMETRY,
            safetyLevel = SafetyLevel.SAFE,
            targetManufacturer = DeviceManufacturer.SAMSUNG,
            description = "Background analytics collector uploading diagnostic data from Knox security framework.",
            ramSavingMb = 40,
            riskNote = "Safe to disable unless operating in managed enterprise environment."
        ),
        BloatPackage(
            packageName = "com.sec.spp.push",
            displayName = "Samsung Push Service",
            category = BloatCategory.TELEMETRY,
            safetyLevel = SafetyLevel.SAFE,
            targetManufacturer = DeviceManufacturer.SAMSUNG,
            description = "Sends promotional marketing push notifications and Galaxy Store alerts in the background.",
            ramSavingMb = 38,
            riskNote = "Safe to disable. Eliminates Samsung promotional pop-ups."
        ),
        BloatPackage(
            packageName = "com.oplus.statistics.rom",
            displayName = "ColorOS / Oplus Analytics",
            category = BloatCategory.TELEMETRY,
            safetyLevel = SafetyLevel.SAFE,
            targetManufacturer = DeviceManufacturer.ONEPLUS_OPPO,
            description = "Collects system behavior metrics and crash statistics for Oppo and OnePlus devices.",
            ramSavingMb = 50,
            riskNote = "Safe to disable. Frees background CPU wakelocks."
        ),
        BloatPackage(
            packageName = "com.google.android.feedback",
            displayName = "Google Feedback Agent",
            category = BloatCategory.TELEMETRY,
            safetyLevel = SafetyLevel.SAFE,
            targetManufacturer = DeviceManufacturer.GOOGLE,
            description = "Transmits diagnostic feedback logs when system apps crash or encounter errors.",
            ramSavingMb = 25,
            riskNote = "Safe to disable. Non-essential service."
        ),

        // === SOCIAL & SPONSORED PRELOADS (SAFE) ===
        BloatPackage(
            packageName = "com.facebook.services",
            displayName = "Facebook Services",
            category = BloatCategory.SOCIAL_PRELOAD,
            safetyLevel = SafetyLevel.SAFE,
            targetManufacturer = DeviceManufacturer.CARRIER_UNIVERSAL,
            description = "Silent background telemetry daemon bundled on Samsung and carrier-locked Android devices.",
            ramSavingMb = 65,
            riskNote = "100% safe to disable. Huge battery and privacy improvement."
        ),
        BloatPackage(
            packageName = "com.facebook.system",
            displayName = "Facebook App Installer",
            category = BloatCategory.SOCIAL_PRELOAD,
            safetyLevel = SafetyLevel.SAFE,
            targetManufacturer = DeviceManufacturer.CARRIER_UNIVERSAL,
            description = "Allows silent background installation of Facebook partner updates bypassing Play Store.",
            ramSavingMb = 30,
            riskNote = "100% safe to disable."
        ),
        BloatPackage(
            packageName = "com.facebook.appmanager",
            displayName = "Facebook App Manager",
            category = BloatCategory.SOCIAL_PRELOAD,
            safetyLevel = SafetyLevel.SAFE,
            targetManufacturer = DeviceManufacturer.CARRIER_UNIVERSAL,
            description = "Monitors system events to manage Facebook app dependencies.",
            ramSavingMb = 45,
            riskNote = "Safe to disable. Recommended for all privacy-focused users."
        ),
        BloatPackage(
            packageName = "com.netflix.activation",
            displayName = "Netflix Partner Activation",
            category = BloatCategory.SOCIAL_PRELOAD,
            safetyLevel = SafetyLevel.SAFE,
            targetManufacturer = DeviceManufacturer.CARRIER_UNIVERSAL,
            description = "Preloaded activation stub for Netflix bundled by OEMs.",
            ramSavingMb = 20,
            riskNote = "Safe to disable. Does not hinder regular Netflix from Play Store."
        ),
        BloatPackage(
            packageName = "com.microsoft.appmanager",
            displayName = "Link to Windows Service",
            category = BloatCategory.OEM_EXTRA,
            safetyLevel = SafetyLevel.SAFE,
            targetManufacturer = DeviceManufacturer.SAMSUNG,
            description = "Microsoft Phone Link companion service bundled into Samsung firmware.",
            ramSavingMb = 55,
            riskNote = "Safe to disable if you do not sync phone notifications to a Windows PC."
        ),

        // === SAMSUNG ONE UI SERVICES (SAFE & CAUTION) ===
        BloatPackage(
            packageName = "com.samsung.android.bixby.agent",
            displayName = "Bixby Voice Agent",
            category = BloatCategory.OEM_EXTRA,
            safetyLevel = SafetyLevel.SAFE,
            targetManufacturer = DeviceManufacturer.SAMSUNG,
            description = "Samsung voice assistant daemon constantly running hotword listening threads.",
            ramSavingMb = 85,
            riskNote = "Safe to disable if using Google Assistant or no voice assistant."
        ),
        BloatPackage(
            packageName = "com.samsung.android.app.spage",
            displayName = "Samsung Free / Bixby Home",
            category = BloatCategory.OEM_EXTRA,
            safetyLevel = SafetyLevel.SAFE,
            targetManufacturer = DeviceManufacturer.SAMSUNG,
            description = "Minus-one feed screen displaying sponsored news and tab games.",
            ramSavingMb = 70,
            riskNote = "Safe to disable. Cleans up stock home screen swipes."
        ),
        BloatPackage(
            packageName = "com.samsung.android.game.gos",
            displayName = "Game Optimizing Service (GOS)",
            category = BloatCategory.BACKGROUND_DAEMON,
            safetyLevel = SafetyLevel.SAFE,
            targetManufacturer = DeviceManufacturer.SAMSUNG,
            description = "Controversial thermal throttling service that limits CPU/GPU speeds across thousands of apps.",
            ramSavingMb = 60,
            riskNote = "Safe to disable. May increase gaming benchmark performance and prevent throttling."
        ),
        BloatPackage(
            packageName = "com.samsung.android.kidsinstaller",
            displayName = "Samsung Kids Mode Stub",
            category = BloatCategory.OEM_EXTRA,
            safetyLevel = SafetyLevel.SAFE,
            targetManufacturer = DeviceManufacturer.SAMSUNG,
            description = "Installer stub for Samsung Kids sandboxed environment.",
            ramSavingMb = 25,
            riskNote = "Safe to disable if Kids Mode is not utilized."
        ),
        BloatPackage(
            packageName = "com.samsung.android.arzone",
            displayName = "AR Zone / AR Emoji",
            category = BloatCategory.MEDIA_GAMES,
            safetyLevel = SafetyLevel.SAFE,
            targetManufacturer = DeviceManufacturer.SAMSUNG,
            description = "Augmented reality camera stickers, AR Doodle, and avatar rendering engines.",
            ramSavingMb = 75,
            riskNote = "Safe to disable. Reduces camera mode bloat."
        ),
        BloatPackage(
            packageName = "com.samsung.android.voc",
            displayName = "Samsung Members",
            category = BloatCategory.OEM_EXTRA,
            safetyLevel = SafetyLevel.CAUTION,
            targetManufacturer = DeviceManufacturer.SAMSUNG,
            description = "Diagnostic tools, community forum, and beta firmware enrollment portal.",
            ramSavingMb = 40,
            riskNote = "Keep if you actively participate in One UI beta programs."
        ),
        BloatPackage(
            packageName = "com.samsung.android.scloud",
            displayName = "Samsung Cloud Sync",
            category = BloatCategory.CLOUD_SYNC,
            safetyLevel = SafetyLevel.CAUTION,
            targetManufacturer = DeviceManufacturer.SAMSUNG,
            description = "Syncs Samsung Gallery, Notes, and settings to Samsung Cloud accounts.",
            ramSavingMb = 50,
            riskNote = "Caution: Disabling will halt automatic Samsung account backups."
        ),

        // === XIAOMI MIUI / HYPEROS (SAFE & CAUTION) ===
        BloatPackage(
            packageName = "com.miui.daemon",
            displayName = "MIUI Daemon Service",
            category = BloatCategory.BACKGROUND_DAEMON,
            safetyLevel = SafetyLevel.SAFE,
            targetManufacturer = DeviceManufacturer.XIAOMI,
            description = "Monitors device battery states and app launches; sends telemetry reports.",
            ramSavingMb = 60,
            riskNote = "Safe to disable. Known to cause battery drain on some MIUI builds."
        ),
        BloatPackage(
            packageName = "com.xiaomi.mipicks",
            displayName = "GetApps (Mi App Store)",
            category = BloatCategory.MEDIA_GAMES,
            safetyLevel = SafetyLevel.SAFE,
            targetManufacturer = DeviceManufacturer.XIAOMI,
            description = "Xiaomi alternative app repository with frequent notifications and app suggestions.",
            ramSavingMb = 75,
            riskNote = "Safe to disable if using Google Play Store."
        ),
        BloatPackage(
            packageName = "com.miui.videoplayer",
            displayName = "Mi Video",
            category = BloatCategory.MEDIA_GAMES,
            safetyLevel = SafetyLevel.SAFE,
            targetManufacturer = DeviceManufacturer.XIAOMI,
            description = "Stock media player bloated with streaming feed, ads, and web trackers.",
            ramSavingMb = 80,
            riskNote = "Safe to disable. Recommend using VLC or MX Player."
        ),
        BloatPackage(
            packageName = "com.miui.yellowpage",
            displayName = "Mi Yellow Pages",
            category = BloatCategory.OEM_EXTRA,
            safetyLevel = SafetyLevel.SAFE,
            targetManufacturer = DeviceManufacturer.XIAOMI,
            description = "Caller ID and business directory lookup service tailored for specific Asian regions.",
            ramSavingMb = 35,
            riskNote = "Safe to disable."
        ),
        BloatPackage(
            packageName = "com.xiaomi.joyose",
            displayName = "Joyose Game Performance Daemon",
            category = BloatCategory.BACKGROUND_DAEMON,
            safetyLevel = SafetyLevel.CAUTION,
            targetManufacturer = DeviceManufacturer.XIAOMI,
            description = "Controls thermal behavior and SMS verification rate limits.",
            ramSavingMb = 40,
            riskNote = "Caution: Disabling may alter FPS caps or charging curve throttling."
        ),

        // === GOOGLE & AOSP ECOSYSTEM (SAFE & CAUTION) ===
        BloatPackage(
            packageName = "com.google.android.apps.wellbeing",
            displayName = "Digital Wellbeing",
            category = BloatCategory.BACKGROUND_DAEMON,
            safetyLevel = SafetyLevel.SAFE,
            targetManufacturer = DeviceManufacturer.GOOGLE,
            description = "Continuously logs every single screen interaction, app opening, and unlock count.",
            ramSavingMb = 90,
            riskNote = "Safe to disable. Noticeably improves low-end device responsiveness."
        ),
        BloatPackage(
            packageName = "com.google.android.projection.gearhead",
            displayName = "Android Auto",
            category = BloatCategory.OEM_EXTRA,
            safetyLevel = SafetyLevel.CAUTION,
            targetManufacturer = DeviceManufacturer.GOOGLE,
            description = "Vehicle dashboard mirror protocol daemon.",
            ramSavingMb = 65,
            riskNote = "Caution: Disabling will prevent connecting device to car infotainment."
        ),
        BloatPackage(
            packageName = "com.google.ar.core",
            displayName = "Google Play Services for AR",
            category = BloatCategory.MEDIA_GAMES,
            safetyLevel = SafetyLevel.SAFE,
            targetManufacturer = DeviceManufacturer.GOOGLE,
            description = "ARCore augmented reality tracking library.",
            ramSavingMb = 50,
            riskNote = "Safe to disable if you do not use 3D animal view or AR measurement tools."
        ),
        BloatPackage(
            packageName = "com.google.android.printservice.recommendation",
            displayName = "Print Service Recommendation",
            category = BloatCategory.BACKGROUND_DAEMON,
            safetyLevel = SafetyLevel.SAFE,
            targetManufacturer = DeviceManufacturer.GOOGLE,
            description = "Scans local Wi-Fi networks for printer hardware beacons.",
            ramSavingMb = 25,
            riskNote = "Safe to disable if you do not print directly from your smartphone."
        ),
        BloatPackage(
            packageName = "com.google.android.apps.magazines",
            displayName = "Google News Stub",
            category = BloatCategory.MEDIA_GAMES,
            safetyLevel = SafetyLevel.SAFE,
            targetManufacturer = DeviceManufacturer.GOOGLE,
            description = "Preloaded Google News aggregator background cache daemon.",
            ramSavingMb = 55,
            riskNote = "Safe to disable."
        ),
        BloatPackage(
            packageName = "com.google.android.videos",
            displayName = "Google TV (Play Movies)",
            category = BloatCategory.MEDIA_GAMES,
            safetyLevel = SafetyLevel.SAFE,
            targetManufacturer = DeviceManufacturer.GOOGLE,
            description = "Pre-installed rental and movie browsing store.",
            ramSavingMb = 40,
            riskNote = "Safe to disable."
        ),

        // === CARRIER DAEMONS (SAFE) ===
        BloatPackage(
            packageName = "com.LogiaGroup.LogiaDeck",
            displayName = "DT Ignite / AppSelect",
            category = BloatCategory.CARRIER,
            safetyLevel = SafetyLevel.SAFE,
            targetManufacturer = DeviceManufacturer.CARRIER_UNIVERSAL,
            description = "Infamous carrier bloatware installer that silently pushes sponsored games onto phone without user consent.",
            ramSavingMb = 70,
            riskNote = "CRITICAL DEBLOAT: Highly recommended to disable immediately."
        ),
        BloatPackage(
            packageName = "com.aura.oobe.att",
            displayName = "AT&T Out-of-Box Setup Agent",
            category = BloatCategory.CARRIER,
            safetyLevel = SafetyLevel.SAFE,
            targetManufacturer = DeviceManufacturer.CARRIER_UNIVERSAL,
            description = "Carrier initial provisioning tool promoting extra subscription services.",
            ramSavingMb = 35,
            riskNote = "Safe to disable after phone initial setup."
        ),

        // === SYSTEM ESSENTIALS (CRITICAL - DO NOT DISABLE) ===
        BloatPackage(
            packageName = "com.android.systemui",
            displayName = "System UI",
            category = BloatCategory.SYSTEM_CORE,
            safetyLevel = SafetyLevel.ESSENTIAL,
            targetManufacturer = DeviceManufacturer.GOOGLE,
            description = "Renders navigation bar, status bar icons, quick settings tiles, and lockscreen.",
            ramSavingMb = 0,
            riskNote = "DANGEROUS: Disabling will cause immediate black screen / boot crash!"
        ),
        BloatPackage(
            packageName = "com.android.settings",
            displayName = "Settings",
            category = BloatCategory.SYSTEM_CORE,
            safetyLevel = SafetyLevel.ESSENTIAL,
            targetManufacturer = DeviceManufacturer.GOOGLE,
            description = "Android system settings and preference configuration provider.",
            ramSavingMb = 0,
            riskNote = "DANGEROUS: System essential component."
        ),
        BloatPackage(
            packageName = "com.google.android.packageinstaller",
            displayName = "Package Installer",
            category = BloatCategory.SYSTEM_CORE,
            safetyLevel = SafetyLevel.ESSENTIAL,
            targetManufacturer = DeviceManufacturer.GOOGLE,
            description = "Required to verify, parse, and install or uninstall Android applications.",
            ramSavingMb = 0,
            riskNote = "DANGEROUS: Disabling prevents installing or updating any app."
        ),
        BloatPackage(
            packageName = "com.android.phone",
            displayName = "Telephony Phone Services",
            category = BloatCategory.SYSTEM_CORE,
            safetyLevel = SafetyLevel.ESSENTIAL,
            targetManufacturer = DeviceManufacturer.GOOGLE,
            description = "Modem driver interface, cellular calls, and emergency 911/112 routing.",
            ramSavingMb = 0,
            riskNote = "DANGEROUS: Required for cellular communication."
        )
    )

    fun getRecommendedPresets(): Map<String, List<String>> {
        return mapOf(
            "Ultra Safe (Zero Risk)" to curatedPackages.filter { it.safetyLevel == SafetyLevel.SAFE }.map { it.packageName },
            "Anti-Telemetry & Privacy" to curatedPackages.filter { it.category == BloatCategory.TELEMETRY || it.category == BloatCategory.SOCIAL_PRELOAD }.map { it.packageName },
            "Carrier Bloat Purge" to curatedPackages.filter { it.category == BloatCategory.CARRIER || it.packageName.contains("facebook") || it.packageName.contains("aura") }.map { it.packageName },
            "Max Performance (Safe + Daemons)" to curatedPackages.filter { it.safetyLevel == SafetyLevel.SAFE || it.category == BloatCategory.BACKGROUND_DAEMON }.map { it.packageName }
        )
    }
}
