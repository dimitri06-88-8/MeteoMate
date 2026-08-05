# Contributing to MeteoMate

Thank you for your interest in the project.

## Before you start

1. Create a dedicated branch for your change.
2. Do not commit API keys, passwords, signing keys, APK files, or local IDE settings.
3. Follow the existing Kotlin and Compose style.

## Validation

Before submitting a change, run:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug --no-parallel
```

Briefly describe what changed and how you verified it.
