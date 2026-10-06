# Contributing

Keep changes focused and explain the user-visible behavior and how it was checked.
Build with the documented SDK and verify affected screens on Android. Preserve
readable dark/light themes, system-bar insets, accessibility labels, and offline
operation. Do not commit APKs, signing keys, credentials, or personal phone records.

The source is plain Java and Android resources with no third-party app dependencies.
Do not add an external library or a new permission without explaining the need.
The AI shortcuts must not disable existing assistants or silently change permissions.

For setup changes, run tests/run.py and verify permission refusal, Apply, and Undo
on Android. Preserve shorter timeouts and newer owner choices. Keep manual review
notes distinct from live checks; a completed tour is not a security certification.
