# Android automation boundaries

Research checked against Android's official API documentation on 6 October 2026.
The implementation uses public APIs and works without root or a managed-device setup.

## Changes an ordinary app can make

Android requires the WRITE_SETTINGS declaration and an explicit user grant through
ACTION_MANAGE_WRITE_SETTINGS before an app can write supported System settings.
Still checks canWrite(), stores an undo record, attempts the two allowlisted changes,
and reads back each result. A refused or unsupported operation is shown as needing
review, never as success. The permission can be removed after applying the defaults.

[Settings.System and canWrite](https://developer.android.com/reference/android/provider/Settings.System)

The screen-off timeout is an inactivity timeout, not the delay before the device
locks. The password-preview setting controls brief character display in text fields
that honor it. Newer Android documentation deprecates that setting at API 37.2;
future implementations may differ, so unavailable/failed reads and writes are handled.

[Timeout](https://developer.android.com/reference/android/provider/Settings.System#SCREEN_OFF_TIMEOUT)
· [Password previews](https://developer.android.com/reference/android/provider/Settings.System#TEXT_SHOW_PASSWORD)

## Controls kept in Android's hands

WRITE_SECURE_SETTINGS is not a normal third-party app permission. Still does not ask
for it or for an ADB grant. Always-on VPN administration is available to device/profile
owners under Android enterprise APIs, rather than to a normal launcher installation.
A normal app install is not device-owner provisioning. Still guides the existing
owner through Android settings and leaves the user's VPN in place.

[Permission reference](https://developer.android.com/reference/android/Manifest.permission#WRITE_SECURE_SETTINGS)
· [Always-on VPN administration](https://developer.android.com/reference/android/app/admin/DevicePolicyManager#setAlwaysOnVpnPackage(android.content.ComponentName,java.lang.String,boolean))

Package installation involves Android/store permission and confirmation rules. Still
opens curated official destinations and does not download APKs, add an installer,
or silently install packages. The selected store manages updates. Keyboard and home
selection use Android's public settings/picker/role APIs; the user makes the choice.

[PackageInstaller user action](https://developer.android.com/reference/android/content/pm/PackageInstaller.SessionParams#setRequireUserAction(int))
· [RoleManager](https://developer.android.com/reference/android/app/role/RoleManager)

## Status without inflated claims

VPN transport, secure lock, keyboard selection, home role, and the two System values
can be checked locally. A VPN transport does not establish public-IP/DNS safety or
lockdown. Ads, app permissions, lock-screen notification policy, update availability,
and VPN switches are guided reviews. A review checkbox is stored as the user's note.
The installed patch date is displayed without declaring that it is the latest update.

[NetworkCapabilities](https://developer.android.com/reference/android/net/NetworkCapabilities)
· [KeyguardManager](https://developer.android.com/reference/android/app/KeyguardManager)

Still disables its own recent-app screenshot on Android 13+. This API does not prevent
ordinary user screenshots or screenshots requested by assistant integrations.

[Recent-app preview API](https://developer.android.com/reference/android/app/Activity#setRecentsScreenshotEnabled(boolean))

## Why there is no "fully protected" button

This app can improve discoverability and apply two consented settings. It cannot
replace manufacturer firmware, add GrapheneOS security hardening, prevent every
exploit, hide identity after account login, or make remote AI run locally. The app
shows remaining actions rather than a security score or an anonymity guarantee.

[GrapheneOS features](https://grapheneos.org/features)
