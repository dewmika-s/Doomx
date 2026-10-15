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


## DOOMX 2.1 update
- Added DOOMX launcher icon using the existing black/lime/red vector mark.
- Added a floating game panel (Android "Display over other apps" permission required).
- Added Low Performance, Balanced, and Performance profile selections. These are saved profiles; a normal app cannot force CPU/GPU clocks or guarantee in-game FPS.
- Added battery temperature reading when Android exposes it.
- Added X/Y touch-profile sliders and copy buttons for generated owner keys.
- The RAM action explains Android's restrictions instead of claiming to clear other apps' memory.
- The key manager remains a local prototype. Keys do not synchronize across phones until a server-side licensing backend is implemented.
- Play Protect warnings cannot be safely bypassed by code; distribute only a reviewed, signed build and investigate the exact warning.


## DOOMX 2.4 device compatibility
- Added fallback intents for devices that do not expose the app-specific overlay-permission screen.
- Checks whether settings activities are available before opening them and displays clearer fallback messages.
- Uses the foreground-service start API on Android 8.0 and newer.
- Displays diagnostic messages if a device/vendor blocks overlay windows.
- Floating overlays remain unavailable on some managed/restricted devices and require the user to grant overlay permission.


## Version 2.6 updates
- One tap-to-cycle performance profile control: LOW → BALANCED → HIGH → LOW.
- Circular DX floating handle can be dragged and remembers its last position.
- Panel has a visible close control that returns to the floating handle.
- Live battery-temperature and display-refresh readings in the panel. Actual Free Fire FPS is not exposed to ordinary apps on many devices, so the panel labels this limitation honestly.
- Sensitivity profile output has a readable, padded result card.
- Dark/light theme preference remains shared with the floating panel.

Performance modes save DOOMX profile choices; they do not force CPU/GPU clocks, clear other apps' private RAM, or alter Free Fire's internal sensitivity.
