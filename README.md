# DOOMX FF Optimizer v2.0

Dark military/arena-inspired dashboard with lime telemetry, red DOOMX identity, local key UI, owner key manager, sensitivity profile suggestions, device metrics, network status, storage shortcuts, support links, and a 3-second BOOST & PLAY animation.

## Build APK
1. Open this folder in Android Studio.
2. Install Android SDK Platform 35 and JDK 17 if prompted.
3. Build > Build Bundle(s) / APK(s) > Build APK(s).
4. APK output: `app/build/outputs/apk/debug/app-debug.apk`.

GitHub Actions workflow is included at `.github/workflows/build-apk.yml`. Upload all project files including `.github` to the repository, then Actions > Build DOOMX APK > Run workflow. The workflow intentionally does not use `android-actions/setup-android`, avoiding its failing `tools` package setup.

## Prototype key system warning
The owner code `doomadmin` and keys are stored locally in SharedPreferences. Device binding is local and can be bypassed by modifying/reinstalling the APK. Do not use this as production licensing. Secure owner authentication, central key database, expiry enforcement and one-device binding require a backend with server-side authorization.

## Safe Android behavior
- Reads RAM availability, battery level, storage filesystem size, network transport and display refresh rate.
- Opens Free Fire / Free Fire MAX if installed after the 3-second progress animation.
- Provides system settings shortcuts for storage/network.
- Does not silently force-stop other apps, clear other apps' private caches, unlock CPU/GPU clocks, or claim actual in-game FPS.
- Network speed and ping are not measured in this starter because that requires a reliable test endpoint.
