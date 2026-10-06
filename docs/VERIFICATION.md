# Verification

Version 1.2 was built and signed locally using Android SDK 36 and build tools 36.1.0.
The signed APK's manifest requests only ACCESS_NETWORK_STATE; no INTERNET permission
is present. These are functional checks, not an independent security audit.

On a separate Android 17 emulator:

- Fresh installation opens in dark mode with readable navigation and system bars.
- Home, Privacy, app drawer, search field, and Appearance dialog render correctly.
- System appearance follows Android's dark/light setting.
- The System selection persists across process stop and restart.
- Selecting Dark again applies to the app drawer and search field.

Earlier versions were checked on a Motorola device for app launching, home-app
selection, VPN settings shortcuts, and Qira discovery. Those checks do not establish
support for every Motorola model or Android release. Minimum Android 11 is declared;
that oldest supported version has not yet been tested.

CI builds with a disposable signing key, verifies the APK, and checks its permission
manifest. The published release uses a separate retained key. CI cannot prove the
absence of vulnerabilities or verify a user's phone configuration.
