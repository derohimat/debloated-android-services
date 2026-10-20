# DebloatMaster 🚀
> **Android System App Debloater, ADB Shell Script Generator & Safe Snapshot Restoration**

DebloatMaster is a modern, material-designed Android utility engineered to identify, catalog, and safely debloat pre-installed carrier bloat, OEM background services, telemetry trackers, and unneeded system apps. It operates with a **zero-risk philosophy**, featuring built-in safety levels, automated snapshot backups, and one-tap generation of non-destructive ADB commands that run over standard Android Debug Bridge without requiring root access.

---

## 📲 Quick Start: Try Without Building (Download APK)

You do **not** need to set up Android Studio, the Android SDK, or Gradle on your computer to try DebloatMaster. You can download and install the pre-compiled APK directly:

### 1. Download the APK
- Download the generated application package:
  - **APK File Location**: `.build-outputs/app-release.apk` (or `app-debug.apk`)
  - In AI Studio, you can export the APK directly from the upper settings/download menu or from the project artifacts panel.

### 2. Sideload on Your Android Device
1. Transfer the `.apk` file to your phone via USB, Google Drive, or local file transfer.
2. Tap the APK file in your device's file manager.
3. If prompted, toggle **Allow installation from this source** (Settings > Security > Install unknown apps).
4. Tap **Install** and open **DebloatMaster**.

*Alternatively, install via computer terminal with ADB:*
```bash
adb install -r .build-outputs/app-release.apk
# or
adb install -r .build-outputs/app-debug.apk
```

---

## ⚡ Key Features

### 🗄️ 1. Room Database & Reactive Package Catalog
- Automatic on-device scanning of system packages and OEM system services.
- Real-time persistence using **Android Jetpack Room** (`SystemApplicationEntity`, `DebloatSnapshotEntity`, `DebloatAuditLogEntity`).
- Tracks installed state, enabled/disabled flags, package UID, version, and categorization.

### 🛡️ 2. Color-Coded Risk Indicators
Every detected package is classified with strict safety analysis:
- 🟢 **Safe to Remove**: Standalone carrier apps, promotional games, OEM duplicate apps, and telemetry. Safe to disable without impacting core device stability.
- 🟡 **Caution**: Auxiliary components (e.g., weather widgets, cloud print, voice assistant stubs, custom launcher accessories). Disabling may remove specific conveniences.
- 🔴 **Critical System (Essential)**: Core Android frameworks, Package Manager, System UI, telephony, and cryptographic services. **Permanently protected and locked** from accidental bulk removal to eliminate bootloop risks.

### 🔘 3. Bulk Selection & Intelligent Toggles
- **Master Tri-State Toggle (`bulk_selection_bar`)**: Quickly preview selection states (All Selected, Partially Selected, None).
- **One-Tap "Select All"**: Automatically selects all safe and cautionary candidate apps in the filtered view while safely excluding essential system components.
- **One-Tap "Deselect All"**: Instantly resets selection without losing your filter criteria.
- **Search & Filter Controls**: Real-time fuzzy search by app name or package name, filter by risk level, and toggle installed-only view.

### 💻 4. Interactive ADB Shell Script Generator
- DebloatMaster respects Android security permissions and generates clean, non-destructive ADB commands:
  - **Disable (Recommended)**: `adb shell pm disable-user --user 0 <package>`
  - **Uninstall for User 0**: `adb shell pm uninstall -k --user 0 <package>`
  - **Re-enable / Restore**: `adb shell pm enable <package>` or `adb shell cmd package install-existing <package>`
- **Copy Script to Clipboard**: Ready to paste directly into your terminal.
- **Shell File Export**: Generate ready-to-run `.sh` (macOS/Linux) and `.bat` (Windows Command Prompt) debloat scripts.

### 🔄 5. Snapshot Backup & Instant Restore
- Capture point-in-time snapshots of package states before applying any modifications.
- If an OEM feature or service is needed later, generate an instant restoration script from your snapshot with one tap.

### 📝 6. Real-Time Audit Logs
- Every state change, selection toggle, snapshot creation, and script export is logged with a human-readable timestamp in the Room database for full transparency.

---

## 🖥️ How to Use with ADB (Step-by-Step)

Debloating system apps via ADB does **not** void your warranty or trip security flags (like Samsung Knox).

### Step 1: Enable USB Debugging on Your Phone
1. Go to **Settings** > **About Phone**.
2. Tap **Build Number** 7 times rapidly until "You are now a developer!" appears.
3. Go back to **Settings** > **System** > **Developer Options**.
4. Enable **USB Debugging** (and on Xiaomi/MIUI/HyperOS devices, enable *USB Debugging (Security settings)*).

### Step 2: Connect to Your Computer
1. Connect your phone to your computer via USB cable.
2. In your computer terminal or Command Prompt, verify the connection:
   ```bash
   adb devices
   ```
3. Accept the RSA fingerprint authorization prompt on your phone screen.

### Step 3: Run the DebloatMaster Script
1. In the DebloatMaster app, select the apps you want to debloat.
2. Switch to the **ADB & Actions** tab.
3. Tap **Copy Disable Script** or export the file.
4. Paste and execute the commands into your computer terminal:
   ```bash
   # Example command executed on computer:
   adb shell pm disable-user --user 0 com.facebook.system
   ```

---

## 🏗️ Building from Source

If you want to compile the project locally using Gradle:

### Prerequisites
- JDK 17 or higher
- Android SDK Platform 36 (or API 34+)
- Gradle 8.13+

### Build Commands
```bash
# Clone the repository
git clone <repository_url>
cd <repository_directory>

# Build Debug APK
gradle assembleDebug

# Build Release APK
gradle assembleRelease

# Run Robolectric Unit & Integration Tests
gradle :app:testDebugUnitTest
```

Compiled APKs will be output to:
- `app/build/outputs/apk/debug/app-debug.apk`
- `app/build/outputs/apk/release/app-release.apk`
- Copied to root: `.build-outputs/`

---

## 🔒 Safety & Privacy Guarantee
- **Zero Internet Permissions Required for Core Debloating**: DebloatMaster operates completely offline on your device.
- **Non-Destructive**: Apps disabled with `pm disable-user --user 0` remain in the system partition and can be re-enabled at any time without a factory reset.
