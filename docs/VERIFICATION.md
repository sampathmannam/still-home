# Verification

## Version 2.0

The new guided setup is checked with the release APK on an isolated Android 17
emulator. Public System-setting access is granted through Android's own UI, not
through an ADB permission grant. Shell commands seed/read emulator values for tests.

Verified:

- Signed APK builds and requests only ACCESS_NETWORK_STATE and WRITE_SETTINGS.
- A fresh launch shows the dark welcome screen and six-step setup. Navigation
  from the first through the third step was exercised on the emulator.
- Declining special settings access leaves the two seeded Android values unchanged.
- Granting access applies the one-minute timeout and disables password previews;
  values are read back from Android to verify the result.
- Undo preserves a newer 30-second timeout and restores the prior password-preview value.
- Reapplying defaults keeps an existing 30-second timeout unchanged.
- Foreground text/action colors pass a 4.5:1 contrast check in both themes.
- Pure Java regression checks cover tighter timeouts, missing readings, and undo
  preserving newer user choices.

The signed update was also installed in place on a Motorola Edge 60 Fusion running
Android 17. Its setup screen correctly read the existing one-minute timeout and
disabled password previews; its home screen reported the active VPN. These checks
do not establish that every guided action works on every phone.

The emulator UI-automation service became unreliable during the remaining flow
checks; those checks were stopped. Complete restart/resume, large-font and wide-screen
interaction, special-access revocation, and every external settings destination
remain unverified for 2.0. The physical phone disconnected before further configuration
checks. These are functional checks, not an independent security audit or evidence
of GrapheneOS-equivalent security.

## Earlier release

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
