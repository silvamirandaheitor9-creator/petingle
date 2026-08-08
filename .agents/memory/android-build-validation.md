---
name: Android build validation
description: Local and CI constraints for validating the Android application.
---

The local Replit environment may provide Java and Gradle without an Android SDK. In that case, Gradle configuration can be validated locally, but APK compilation must run through the GitHub Actions workflow, which provisions the Android build environment.

**Why:** The project targets Android SDK 35 and the local environment currently has no `ANDROID_HOME`, `ANDROID_SDK_ROOT`, `sdkmanager`, or `adb`.

**How to apply:** Keep the GitHub workflow as the authoritative APK build check after Gradle or Android source changes.