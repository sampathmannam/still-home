package org.stillhome.launcher;

/** Curated official destinations, opened in the user's store or browser. No downloads here. */
final class AppCatalog {
  final String pkg, name, purpose, detail, url;
  final boolean fdroid;

  AppCatalog(String pkg, String name, String purpose, String detail, String url, boolean fdroid) {
    this.pkg = pkg;
    this.name = name;
    this.purpose = purpose;
    this.detail = detail;
    this.url = url;
    this.fdroid = fdroid;
  }

  static AppCatalog foss(String pkg, String name, String purpose, String detail) {
    return new AppCatalog(
        pkg, name, purpose, detail, "https://f-droid.org/packages/" + pkg + "/", true);
  }

  static final AppCatalog[] ALL = {
    new AppCatalog(
        "org.fdroid.fdroid",
        "F-Droid",
        "App updates",
        "Find open-source apps and keep their updates available.",
        "https://f-droid.org/",
        false),
    new AppCatalog(
        "org.mozilla.firefox",
        "Firefox",
        "Everyday browsing",
        "Review tracking protection and data collection after installation.",
        "https://www.mozilla.org/firefox/browsers/mobile/android/",
        false),
    new AppCatalog(
        "org.torproject.torbrowser",
        "Tor Browser",
        "More private browsing",
        "Helps resist browser fingerprinting. Personal logins can still identify you.",
        "https://www.torproject.org/download/",
        false),
    new AppCatalog(
        "ch.protonvpn.android",
        "Proton VPN",
        "Network privacy",
        "Free service available. Configure always-on and blocking in Android.",
        "https://protonvpn.com/free-vpn/android",
        false),
    foss(
        "helium314.keyboard",
        "HeliBoard",
        "Offline typing",
        "Enable it in Android, then select it as your keyboard."),
    foss(
        "org.fossify.gallery",
        "Fossify Gallery",
        "Photos",
        "Browse local photos. Existing cloud backup settings are separate."),
    foss(
        "org.fossify.filemanager",
        "Fossify File Manager",
        "Files",
        "Manage local files. Allow storage access only if you need it."),
    foss(
        "org.fossify.contacts",
        "Fossify Contacts",
        "Contacts",
        "Android accounts may still sync your address book."),
    foss(
        "org.fossify.calendar",
        "Fossify Calendar",
        "Calendar",
        "Test reminders. Android accounts may still sync events."),
    foss(
        "org.fossify.math",
        "Fossify Calculator",
        "Calculator",
        "Offline calculations. Currently labelled beta by the project."),
    foss(
        "org.fossify.notes",
        "Fossify Notes",
        "Notes",
        "Local notes; review backup settings. Currently labelled beta."),
    foss(
        "org.fossify.musicplayer",
        "Fossify Music Player",
        "Local music",
        "Play audio stored on your phone."),
    foss(
        "org.fossify.documents",
        "Fossify Documents",
        "Documents",
        "Open local documents. Currently labelled beta."),
    foss(
        "org.fossify.voicerecorder",
        "Fossify Voice Recorder",
        "Recordings",
        "Microphone access is needed to record. Currently labelled beta."),
    foss(
        "org.fossify.clock",
        "Fossify Clock",
        "Alarms and timers",
        "Test alarms before relying on them. Existing alarms do not transfer."),
    new AppCatalog(
        "net.thunderbird.android",
        "Thunderbird",
        "Email",
        "Sign in directly in the app. Your email provider still processes mail.",
        "https://www.thunderbird.net/mobile/",
        false),
    new AppCatalog(
        "app.organicmaps",
        "Organic Maps",
        "Offline maps",
        "Download a map for your region before going offline.",
        "https://organicmaps.app/",
        false),
    new AppCatalog(
        "com.pocketpalai",
        "PocketPal AI",
        "On-device AI",
        "Download a suitable model. Keep your existing assistants available.",
        "https://github.com/a-ghorbani/pocketpal-ai",
        false)
  };

  static AppCatalog get(String pkg) {
    for (AppCatalog app : ALL) if (app.pkg.equals(pkg)) return app;
    return null;
  }
}
