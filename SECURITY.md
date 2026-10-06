# Security

This early project has not had an independent security audit. Keep Android, Still
Home, and other apps updated. Report vulnerabilities through this repository's
private vulnerability reporting page. Do not post keys or personal phone records
in public issues. No guaranteed security support or response time is offered.

## Permission boundary

ACCESS_NETWORK_STATE is used for local network status. WRITE_SETTINGS is optional
special access, requested through Android only after Apply/Undo. The only allowed
writes are SCREEN_OFF_TIMEOUT and TEXT_SHOW_PASSWORD. Undo records are private,
written before changes, and used only when current values still match applied values.
Newer user choices must be preserved. Unavailable values must not be labelled verified.

No Internet, WRITE_SECURE_SETTINGS, accessibility service, device administration,
installer, boot receiver, or VPN service is included. Changes to that boundary require
explicit design discussion, tests, and privacy-policy updates.

## Reporting and testing

Declining access must not mutate settings. Partial failures must be reported without
claiming complete success. Review notes are not system verification. Test Apply, Undo,
permission revocation, restart/resume, and preservation of tighter timeouts when changing
setup behavior. Test keyboard and home selection through Android's own UI.

A successful build or permission check is not evidence that every phone is secured.
OEM restrictions and future Android changes can affect supported settings. System
settings persist after uninstall; undo must be used first when restoration is desired.
