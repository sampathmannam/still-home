# Privacy

Still Home has no Internet permission, accounts, ads, analytics, crash-upload service,
tracking SDKs, or backend. Android cloud backup is disabled for this application.

## Information used locally

- Launchable app names/icons and requested permission metadata for app discovery.
- Search text in memory; search is not saved.
- Android-reported VPN connection, secure-screen-lock status, installed security-patch
  date, selected keyboard, and home-app status.
- Screen timeout and the password-character-preview value.
- Theme choice, setup position, review checkboxes, and the previous/applied values
  needed to undo Still's automatic changes, in private app preferences.

The app does not read messages, photos, contact contents, account credentials,
browser history, or AI conversations. Uninstalling removes local app preferences,
including undo records; system settings already changed persist in Android.

## Optional automation permission

WRITE_SETTINGS is special Android access, requested only after an explicit Apply or
Undo action. Android's permission is broader than the two changes Still implements.
The code writes only SCREEN_OFF_TIMEOUT and TEXT_SHOW_PASSWORD in Settings.System.
It does not request WRITE_SECURE_SETTINGS, device administration, accessibility
control, package-install privileges, or a VPN service.

Changes are read back before success is reported. Before-values are saved before
writing. Undo preserves a newer value if it differs from Still's applied value.
Revoking special access prevents additional writes; it does not restore earlier
values. Manual settings links remain available without granting this access.

## Other apps and services

Opening a website or store listing hands control to the selected browser/store.
Installing, launching, or signing in to another app is governed by that app and its
service. Cloud AI receives submitted prompts. Account sync and Android backups can
move other apps' data off the phone even if a replacement has no Internet permission.

Still's review marks are notes from the user, not measurements of protected settings.
VPN detection checks the active Android network only; it does not verify traffic,
DNS, public IPs, lockdown, or other profiles.

On Android 13+, Still requests that its own screen not be used as a recent-app
preview. This does not block ordinary screenshots, screen sharing, or other apps.
