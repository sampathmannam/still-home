# Privacy-oriented app replacements

Use maintained apps, install from the developer's official distribution channels,
and keep a working way to receive updates. Open-source licensing makes inspection
possible; it does not automatically establish security. Dark appearance is a visual
preference, not a privacy protection. This is a selection guide, not an automatic
installer or a claim that these apps are installed on any particular device.

| Everyday function | Open-source option | Setup consideration |
| --- | --- | --- |
| Home screen | Still Home | Retain the stock launcher for rollback and device-specific integrations. |
| App discovery | [F-Droid](https://f-droid.org/) | Update cadence and signing keys differ by distribution. |
| Keyboard | [HeliBoard](https://github.com/HeliBorg/HeliBoard) | Offline keyboard; retain any required voice-input option separately. |
| Web browsing | [Firefox](https://www.mozilla.org/firefox/browsers/mobile/android/) | Review telemetry and tracking settings; keep updated. |
| More anonymous browsing | [Tor Browser](https://www.torproject.org/download/) | Avoid extra extensions; personal logins can identify you. |
| VPN | [Proton VPN](https://protonvpn.com/free-vpn/android) | Configure always-on and block-without-VPN; verify connectivity. |
| Photos, files, contacts, calendar | [Fossify](https://www.fossify.org/apps/) | Local apps; Android accounts may still sync contacts/calendar. |
| Calculator, notes, clock, recorder | [Fossify](https://www.fossify.org/apps/) | Some are labelled beta; test reminders/alarms before depending on them. |
| Local music | [Fossify Music Player](https://www.fossify.org/apps/) | Plays existing local files; it does not replace a streaming subscription. |
| Email | [Thunderbird for Android](https://www.thunderbird.net/en-US/mobile/) | Requires the owner's account login; email-provider privacy still applies. |
| Offline navigation | [Organic Maps](https://organicmaps.app/) | Download a map for the needed region; coverage and features vary. |
| Local AI | [PocketPal AI](https://github.com/a-ghorbani/pocketpal-ai) | Download a suitable model; check its license and optional online features. |
| Calls | [Fossify Phone](https://github.com/FossifyOrg/Phone) | Test incoming/outgoing calls and Bluetooth before making it the default. |
| SMS/MMS | [Fossify Messages](https://github.com/FossifyOrg/Messages) | SMS/MMS is not end-to-end encrypted; switching can lose encrypted RCS. |

Keep existing cloud AI if needed. An offline model can supplement it, but a launcher
cannot make remote AI services process prompts only on the device. Motorola camera
processing, AI integrations, firmware, and system updates may require proprietary
components. Keep these when needed for functionality and device security.

## Apply and verify

1. Confirm the intended phone; record Android/security-patch and current default apps.
2. Install a replacement from a verified official source. Check requested permissions.
3. Enable system dark mode and choose System or Dark in the replacement app.
4. Grant only permissions needed for the chosen function. Let the owner handle logins.
5. Confirm access to existing data without deleting, duplicating, or bulk migrating it.
6. Change the corresponding default where Android supports it. Some categories have
   no system-wide default; choose the preferred app through normal open-with prompts.
7. Test the actual function. Check alarms/reminders, media access, keyboard input,
   browser opening, and VPN blocking as appropriate. Retain originals until validated.
8. Keep app updates, Play Protect, the locked bootloader, and official system updates.

Do not disable system providers, WebView, Play services, or update components in bulk.
Do not downgrade encrypted messaging merely to increase the open-source app count.
Google documents the encryption distinction here:
[Google Messages encryption](https://support.google.com/messages/answer/10262381?hl=en).
Do not configure two Android VPN-service apps in the same user at once; the second
replaces the first. Device-level Advanced Protection can restrict F-Droid and other
sideloaded app installs/updates, so assess that tradeoff before enabling it.

This approach can reduce data collection. It cannot turn Motorola firmware into
GrapheneOS, make every component open source, or guarantee invisibility online.
