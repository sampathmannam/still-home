package org.stillhome.launcher;

import android.app.*;
import android.app.role.RoleManager;
import android.content.*;
import android.content.pm.*;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.graphics.drawable.*;
import android.net.*;
import android.os.*;
import android.provider.Settings;
import android.provider.Telephony;
import android.telecom.TelecomManager;
import android.text.*;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.text.*;
import java.util.*;

/** Offline launcher and consent-based setup. No VPN, admin service, or silent installer. */
public final class HomeActivity extends Activity {
  private int background, surface, ink, muted, accent, onAccent, line, tone;
  private boolean dark, wide, callbackRegistered;
  private LinearLayout root, content, nav;
  private ScrollView scroll;
  private String page = "Home", notice = "";
  private int step;
  private PrivacySetup setup;
  private ConnectivityManager connectivity;
  private TextView connectionLabel;
  private final Handler handler = new Handler(Looper.getMainLooper());
  private final ArrayList<App> apps = new ArrayList<>();
  private final ConnectivityManager.NetworkCallback networkCallback =
      new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(Network n) {
          refreshNetwork();
        }

        @Override
        public void onLost(Network n) {
          refreshNetwork();
        }

        @Override
        public void onCapabilitiesChanged(Network n, NetworkCapabilities c) {
          refreshNetwork();
        }
      };
  private static final String[] STEPS = {
    "Quick defaults",
    "Your connection",
    "Private typing",
    "Everyday apps",
    "Android controls",
    "AI and your home"
  };

  private static final class App {
    String label, pkg;
    ComponentName component;
    Drawable icon;
  }

  private String themeMode() {
    return getPreferences(MODE_PRIVATE).getString("theme", "Dark");
  }

  private void configureTheme() {
    dark =
        themeMode().equals("Dark")
            || (themeMode().equals("System")
                && (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                    == Configuration.UI_MODE_NIGHT_YES);
    setTheme(dark ? R.style.AppTheme : R.style.AppThemeLight);
    background = dark ? 0xff10181e : 0xffeceff0;
    surface = dark ? 0xff1b2831 : 0xffffffff;
    ink = dark ? 0xffe4edf2 : 0xff15232e;
    muted = dark ? 0xffa8b8c1 : 0xff596c74;
    accent = dark ? 0xff8bd0cc : 0xff29616e;
    onAccent = dark ? 0xff10181e : 0xffffffff;
    line = dark ? 0xff354750 : 0xffcdd6da;
    tone = dark ? 0xff203b42 : 0xffdbe8e8;
  }

  @Override
  public void onCreate(Bundle state) {
    configureTheme();
    super.onCreate(state);
    setup = new PrivacySetup(this);
    step = Math.max(0, Math.min(5, setup.state.getInt("step", 0)));
    page =
        state != null
            ? state.getString("page", "Home")
            : (setup.state.getBoolean("welcomed", false) ? "Home" : "Welcome");
    if (page.equals("Privacy")) page = "Setup";
    connectivity = getSystemService(ConnectivityManager.class);
    if (Build.VERSION.SDK_INT >= 33) setRecentsScreenshotEnabled(false);
    wide = getResources().getConfiguration().screenWidthDp >= 600;
    getWindow().setDecorFitsSystemWindows(false);
    root = new LinearLayout(this);
    root.setOrientation(wide ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
    root.setBackgroundColor(background);
    root.setOnApplyWindowInsetsListener(
        (v, insets) -> {
          android.graphics.Insets bars =
              insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
          v.setPadding(
              bars.left,
              bars.top,
              bars.right,
              Math.max(bars.bottom, insets.getInsets(WindowInsets.Type.ime()).bottom));
          return insets;
        });
    nav = new LinearLayout(this);
    nav.setOrientation(wide ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
    nav.setPadding(dp(8), dp(8), dp(8), dp(8));
    scroll = new ScrollView(this);
    scroll.setFillViewport(true);
    scroll.setClipToPadding(false);
    LinearLayout center = new LinearLayout(this);
    center.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
    content = column();
    content.setPadding(dp(24), dp(24), dp(24), dp(28));
    center.addView(
        content,
        new LinearLayout.LayoutParams(
            wide ? dp(Math.min(640, getResources().getConfiguration().screenWidthDp - 100)) : -1,
            -2));
    scroll.addView(center);
    if (wide) {
      root.addView(nav, new LinearLayout.LayoutParams(dp(100), -1));
      root.addView(scroll, new LinearLayout.LayoutParams(0, -1, 1));
    } else {
      root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
      root.addView(nav);
    }
    setContentView(root);
    getWindow()
        .getInsetsController()
        .setSystemBarsAppearance(
            dark
                ? 0
                : WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
    if (Build.VERSION.SDK_INT >= 33)
      getOnBackInvokedDispatcher().registerOnBackInvokedCallback(0, this::goBack);
  }

  @Override
  protected void onResume() {
    super.onResume();
    loadApps();
    String pending = setup.state.getString("pending", "");
    if (!pending.isEmpty()) {
      setup.state.edit().remove("pending").commit();
      notice =
          setup.canWrite()
              ? (pending.equals("undo") ? setup.restore() : setup.apply())
              : "No settings changed. You can allow the two changes later or use Android settings"
                  + " yourself.";
    }
    show(page);
    if (!callbackRegistered)
      try {
        connectivity.registerDefaultNetworkCallback(networkCallback);
        callbackRegistered = true;
      } catch (RuntimeException ignored) {
      }
  }

  @Override
  protected void onPause() {
    if (callbackRegistered) {
      try {
        connectivity.unregisterNetworkCallback(networkCallback);
      } catch (RuntimeException ignored) {
      }
      callbackRegistered = false;
    }
    super.onPause();
  }

  @Override
  protected void onNewIntent(Intent i) {
    super.onNewIntent(i);
    show(setup.state.getBoolean("welcomed", false) ? "Home" : "Welcome");
  }

  @Override
  protected void onSaveInstanceState(Bundle b) {
    b.putString("page", page);
    super.onSaveInstanceState(b);
  }

  @Override
  public void onBackPressed() {
    goBack();
  }

  private void goBack() {
    WindowInsets insets = root.getRootWindowInsets();
    if (insets != null && insets.isVisible(WindowInsets.Type.ime())) {
      ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
          .hideSoftInputFromWindow(root.getWindowToken(), 0);
      return;
    }
    if (page.equals("Wizard")) {
      if (step > 0) openStep(step - 1);
      else show("Setup");
    } else if (page.equals("Catalog") || page.equals("Controls") || page.equals("Summary"))
      show("Setup");
    else if (page.equals("Home")) {
      if (!isHome()) finish();
    } else {
      setup.state.edit().putBoolean("welcomed", true).apply();
      show("Home");
    }
  }

  private void show(String next) {
    page = next;
    connectionLabel = null;
    content.removeAllViews();
    if (next.equals("Welcome")) welcome();
    else if (next.equals("Home")) home();
    else if (next.equals("Apps")) drawer();
    else if (next.equals("Setup")) overview();
    else if (next.equals("Wizard")) wizard();
    else if (next.equals("Catalog")) catalog();
    else if (next.equals("Controls")) controls();
    else if (next.equals("Summary")) summary();
    else ai();
    nav.removeAllViews();
    nav.setVisibility(next.equals("Welcome") ? View.GONE : View.VISIBLE);
    String selected =
        Arrays.asList("Wizard", "Catalog", "Controls", "Summary").contains(next) ? "Setup" : next;
    for (String tab : new String[] {"Home", "Apps", "Setup", "AI"}) {
      boolean active = selected.equals(tab);
      TextView t = text(tab, 14, active ? onAccent : muted, true);
      t.setGravity(Gravity.CENTER);
      t.setMinHeight(dp(52));
      t.setPadding(dp(4), dp(8), dp(4), dp(8));
      t.setBackground(interactive(active ? accent : background, 20));
      t.setSelected(active);
      t.setFocusable(true);
      t.setContentDescription(tab + " tab");
      t.setOnClickListener(
          v -> {
            hideKeyboard();
            show(tab);
          });
      LinearLayout.LayoutParams lp =
          wide ? new LinearLayout.LayoutParams(-1, -2) : new LinearLayout.LayoutParams(0, -2, 1);
      lp.setMargins(dp(4), dp(4), dp(4), dp(4));
      nav.addView(t, lp);
    }
    scroll.post(() -> scroll.scrollTo(0, 0));
  }

  private void welcome() {
    brand();
    space(28);
    title("A little setup.\nMore privacy.", 36);
    body("Make useful changes in a few guided steps. Keep your AI and the apps you depend on.");
    space(24);
    feature(
        "Start with your phone",
        "Still checks available settings locally. Your setup progress stays here.");
    feature(
        "Let Android stay in control",
        "Approve two simple changes, then follow clear prompts for the rest.");
    feature(
        "Pick better everyday apps",
        "Get open-source options from their official stores and websites.");
    space(20);
    button(
        content,
        "Start my setup",
        true,
        () -> {
          setup.state.edit().putBoolean("welcomed", true).putBoolean("started", true).apply();
          openStep(0);
        });
    button(
        content,
        "Explore the home screen",
        false,
        () -> {
          setup.state.edit().putBoolean("welcomed", true).apply();
          show("Home");
        });
    foot("No account. No ads. No Internet access inside Still Home.");
    link("How setup works", this::about);
  }

  private void home() {
    brand();
    TextClock clock = new TextClock(this);
    clock.setFormat12Hour("h:mm");
    clock.setFormat24Hour("HH:mm");
    clock.setTextSize(56);
    clock.setTextColor(ink);
    clock.setTypeface(Typeface.create("sans-serif-light", 0));
    add(clock, 14, 0);
    add(
        text(
            new SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(new Date()),
            15,
            muted,
            false),
        0,
        22);
    LinearLayout panel = panel();
    label(panel, setup.state.getBoolean("tour_finished", false) ? "YOUR SETUP" : "GET STARTED");
    panel.addView(
        text(
            setup.state.getBoolean("tour_finished", false)
                ? "Privacy, at your pace"
                : "Make this phone feel like yours",
            22,
            ink,
            true));
    TextView sub =
        text(
            setup.state.getBoolean("tour_finished", false)
                ? "Review current checks and revisit anything you left for later."
                : "A guided setup for your connection, typing, apps, and AI.",
            15,
            muted,
            false);
    sub.setPadding(0, dp(10), 0, dp(8));
    panel.addView(sub);
    button(
        panel,
        setup.state.getBoolean("tour_finished", false) ? "Review my setup" : "Continue setup",
        true,
        () -> {
          if (setup.state.getBoolean("tour_finished", false)) show("Setup");
          else openStep(step);
        });
    connectionLabel = text(networkLabel(), 14, muted, false);
    add(connectionLabel, 18, 0);
    section("Everyday");
    ArrayList<App> essentials = new ArrayList<>();
    String dialer = "com.google.android.dialer", sms = "com.google.android.apps.messaging";
    try {
      TelecomManager tm = getSystemService(TelecomManager.class);
      if (tm != null && tm.getDefaultDialerPackage() != null) dialer = tm.getDefaultDialerPackage();
      String selected = Telephony.Sms.getDefaultSmsPackage(this);
      if (selected != null) sms = selected;
    } catch (RuntimeException ignored) {
    }
    for (String pkg :
        new String[] {
          dialer,
          sms,
          find("org.fossify.camera") != null ? "org.fossify.camera" : "com.motorola.camera5",
          "org.fossify.gallery"
        }) {
      App a = find(pkg);
      if (a != null) essentials.add(a);
    }
    if (!essentials.isEmpty()) {
      LinearLayout grid = column();
      renderGrid(grid, essentials);
      content.addView(grid);
    }
    row(
        content,
        "Firefox",
        "Your everyday browser",
        () -> openCatalogApp(AppCatalog.get("org.mozilla.firefox")));
    row(
        content,
        "Tor Browser",
        "For browsing with stronger fingerprinting resistance",
        () -> openCatalogApp(AppCatalog.get("org.torproject.torbrowser")));
    row(content, "Your assistants", "Motorola AI and your existing AI apps", () -> show("AI"));
  }

  private void overview() {
    eyebrow("STILL, ON YOUR TERMS");
    title("Privacy setup", 32);
    body(
        "Useful changes, one step at a time. Android checks and your own reviews stay clearly"
            + " separate.");
    space(16);
    button(
        content,
        setup.state.getBoolean("started", false) ? "Resume guided setup" : "Start guided setup",
        true,
        () -> openStep(step));
    section("Your plan");
    for (int i = 0; i < STEPS.length; i++) {
      final int n = i;
      row(content, STEPS[i], stepSummary(i), () -> openStep(n));
    }
    section("Make it yours");
    row(content, "Appearance", themeMode() + " theme", this::chooseAppearance);
    row(
        content,
        "Manage automatic changes",
        setup.hasUndo()
            ? "Review or undo the two setup defaults"
            : "Choose which defaults to apply",
        () -> openStep(0));
    row(
        content,
        "About Still Home",
        "What it checks, what it changes, and what stays private",
        this::about);
  }

  private String stepSummary(int n) {
    if (n == 0)
      return setup.defaultsReady()
          ? "Short timeout and hidden password characters detected"
          : "Two optional settings; changes can be undone";
    if (n == 1) return networkLabel() + " · review always-on separately";
    if (n == 2)
      return heliSelected()
          ? "HeliBoard is your selected keyboard"
          : "Choose a keyboard that works offline";
    if (n == 3) return installedCore() + " of 5 starter apps installed · configuration is separate";
    if (n == 4) return reviewCount() + " of 4 controls reviewed by you";
    return isHome()
        ? "Still Home selected · existing AI stays available"
        : "Choose your home screen and AI options";
  }

  private void openStep(int n) {
    step = n;
    setup
        .state
        .edit()
        .putInt("step", n)
        .putBoolean("started", true)
        .putBoolean("welcomed", true)
        .apply();
    notice = "";
    show("Wizard");
  }

  private void wizard() {
    eyebrow("STEP " + (step + 1) + " OF 6");
    ProgressBar progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
    progress.setMax(6);
    progress.setProgress(step + 1);
    progress.setProgressTintList(ColorStateList.valueOf(accent));
    progress.setProgressBackgroundTintList(ColorStateList.valueOf(line));
    progress.setContentDescription("Setup step " + (step + 1) + " of 6");
    add(progress, 0, 24);
    if (step == 0) defaultsStep();
    else if (step == 1) connectionStep();
    else if (step == 2) keyboardStep();
    else if (step == 3) appsStep();
    else if (step == 4) reviewStep();
    else finishStep();
    space(24);
    button(
        content,
        step == 5 ? "Save setup and see summary" : "Continue",
        true,
        () -> {
          notice = "";
          if (step == 5) {
            setup.state.edit().putBoolean("tour_finished", true).apply();
            show("Summary");
          } else openStep(step + 1);
        });
    button(content, "Save and leave for now", false, () -> show("Setup"));
    foot("Continuing saves your place. It does not mark unverified settings as protected.");
  }

  private void defaultsStep() {
    title("Start with two\nsimple changes.", 32);
    body(
        "Reduce what stays visible when you put your phone down. Android asks for permission before"
            + " Still makes these changes.");
    space(16);
    fact(
        "Screen timeout",
        timeoutLabel(),
        PrivacyPolicy.hasShortTimeout(setup.read(PrivacySetup.TIMEOUT)));
    fact(
        "Password characters",
        passwordLabel(),
        Integer.valueOf(0).equals(setup.read(PrivacySetup.PASSWORD)));
    body(
        "Apply a timeout of one minute, keeping a shorter one if you already use it, and turn off"
            + " brief password-character previews in supported fields.");
    foot(
        "Screen timeout is separate from screen locking. Check your lock delay in Android's"
            + " security settings.");
    if (!notice.isEmpty()) message(notice);
    button(
        content,
        setup.defaultsReady() ? "Recheck recommended settings" : "Apply recommended settings",
        true,
        () -> requestChanges("apply"));
    if (setup.hasUndo())
      button(
          content,
          "Undo Still's automatic changes",
          false,
          () ->
              new AlertDialog.Builder(this)
                  .setTitle("Restore previous settings?")
                  .setMessage(
                      "Restore only settings Still changed that have not since been changed by you."
                          + " Your newer choices will be kept.")
                  .setNegativeButton("Keep settings", null)
                  .setPositiveButton("Restore", (d, w) -> requestChanges("undo"))
                  .show());
    link("Review screen timeout yourself", () -> settings(Settings.ACTION_DISPLAY_SETTINGS));
    link("Review password and lock settings", () -> settings(Settings.ACTION_SECURITY_SETTINGS));
    foot(
        "Android keeps these values if you uninstall Still. Use undo first if you want the previous"
            + " values back.");
    if (setup.canWrite())
      link(
          "Remove Still's settings permission",
          () ->
              tryStart(
                  new Intent(
                      Settings.ACTION_MANAGE_WRITE_SETTINGS,
                      Uri.parse("package:" + getPackageName()))));
  }

  private void requestChanges(String action) {
    if (setup.canWrite()) {
      notice = action.equals("undo") ? setup.restore() : setup.apply();
      show(page);
      return;
    }
    setup.state.edit().putString("pending", action).commit();
    if (!tryStart(
        new Intent(
            Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + getPackageName())))) {
      setup.state.edit().remove("pending").commit();
      notice =
          "This phone did not open the permission screen. Use the manual settings links instead.";
      show(page);
    }
  }

  private void connectionStep() {
    title("Protect your\nconnection.", 32);
    body("Use one trusted VPN, then ask Android to keep it connected.");
    connectionLabel = text(networkLabel(), 18, ink, true);
    add(connectionLabel, 22, 8);
    foot(
        "This checks Android's active connection. It does not test your public IP, DNS leaks, or"
            + " blocking when the VPN disconnects.");
    button(
        content,
        installed("ch.protonvpn.android") ? "Open Proton VPN" : "Get Proton VPN",
        true,
        () -> openCatalogApp(AppCatalog.get("ch.protonvpn.android")));
    section("In Android VPN settings");
    instruction("Connect your VPN first.");
    instruction("Open its settings and enable Always-on VPN.");
    instruction("Enable Block connections without VPN, then check that your connection works.");
    button(content, "Open VPN settings", false, () -> settings(Settings.ACTION_VPN_SETTINGS));
    reviewBox("vpn", "I reviewed always-on VPN and blocking");
    foot(
        "Your review is saved as your note. Still cannot verify these two switches. A second"
            + " VPN-style firewall can replace the first VPN.");
  }

  private void keyboardStep() {
    title("Keep your\ntyping local.", 32);
    body("HeliBoard is an open-source keyboard that works without Internet permission.");
    space(16);
    fact(
        "Selected keyboard",
        heliSelected() ? "HeliBoard is selected" : "HeliBoard selection not detected",
        heliSelected());
    button(
        content,
        installed("helium314.keyboard") ? "Open HeliBoard" : "Get HeliBoard",
        true,
        () -> openCatalogApp(AppCatalog.get("helium314.keyboard")));
    section("Choose it in Android");
    instruction("Install HeliBoard, then enable it in the keyboard list.");
    instruction("Choose HeliBoard as your current keyboard. Test typing in an app you use.");
    button(
        content,
        "Open keyboard list",
        false,
        () -> settings(Settings.ACTION_INPUT_METHOD_SETTINGS));
    button(
        content,
        "Choose current keyboard",
        false,
        () ->
            ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).showInputMethodPicker());
    foot(
        "Keep your existing keyboard available if you need its voice or swipe features. Still does"
            + " not turn off AI or change keyboard permissions.");
  }

  private void appsStep() {
    title("Choose apps\nthat ask for less.", 32);
    body(
        "Start with the essentials. Still checks what's installed and opens the official"
            + " destination for anything you want to add.");
    section("Starter apps");
    for (String pkg : core()) catalogRow(content, AppCatalog.get(pkg));
    button(content, "Browse all suggested apps", false, () -> show("Catalog"));
    button(
        content,
        "Review default apps",
        false,
        () -> settings(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS));
    foot(
        "Installation needs your confirmation in the store or Android installer. An installed app"
            + " may still need permissions and a default-app choice.");
    message(
        "Keep working encrypted messaging. Switching to an SMS-only app can remove end-to-end"
            + " encryption from conversations that had it.");
  }

  private void reviewStep() {
    title("The final Android\ncontrols.", 32);
    body(
        "A few protections live in Android settings. Read each short guide, make your choice, then"
            + " record your review.");
    space(12);
    fact(
        "Screen lock",
        secureLock() ? "A PIN, password, or pattern is set" : "Set a secure screen lock",
        secureLock());
    row(
        content,
        "Lock screen",
        "Hide sensitive notifications and review the delay before locking",
        () -> reviewGuide("lock"));
    row(
        content,
        "App permissions",
        "Review location, microphone, camera, and unused access",
        () -> reviewGuide("permissions"));
    row(
        content,
        "Advertising controls",
        "Review ad topics, app-suggested ads, and ad measurement",
        () -> reviewGuide("ads"));
    row(
        content,
        "Security updates",
        "Installed patch: " + Build.VERSION.SECURITY_PATCH,
        () -> reviewGuide("updates"));
    for (String[] r :
        new String[][] {
          {"lock", "I reviewed lock-screen privacy"},
          {"permissions", "I reviewed app permissions"},
          {"ads", "I reviewed advertising controls"},
          {"updates", "I checked for security updates"}
        }) reviewBox(r[0], r[1]);
    link("Turn on Android's dark theme", () -> settings(Settings.ACTION_DISPLAY_SETTINGS));
    foot(
        "Review marks are your notes, not verified switches. Keep Play Protect and official updates"
            + " enabled. System menus vary by phone.");
  }

  private void reviewGuide(String key) {
    String title, explanation, action;
    if (key.equals("lock")) {
      title = "Lock-screen privacy";
      explanation =
          "In lock-screen notification settings, hide sensitive notification content. In security"
              + " settings, use a strong screen lock and a short lock delay. A short screen timeout"
              + " alone does not guarantee that the phone locks.";
      action = Settings.ACTION_SETTINGS;
    } else if (key.equals("permissions")) {
      title = "App permissions";
      explanation =
          "Review location, camera, microphone, contacts, and files. Keep access that the app"
              + " needs; remove access you do not use. Review background location and unused-app"
              + " permissions. Keep access required by AI features you want to retain.";
      action = Settings.ACTION_PRIVACY_SETTINGS;
    } else if (key.equals("ads")) {
      title = "Advertising controls";
      explanation =
          "Review ad topics, app-suggested ads, and ad measurement. Turn them off if you do not"
              + " want those features. Also review your phone's usage and diagnostic sharing. These"
              + " controls do not remove all tracking from apps or websites.";
      action = "android.adservices.ui.SETTINGS";
    } else {
      title = "Security updates";
      explanation =
          "Check for the manufacturer's system update and Google Play system update where"
              + " available. Install available updates and keep app updates enabled. A patch date"
              + " shown in Still is the installed date, not proof that you have the latest"
              + " available update.";
      action = "android.settings.SYSTEM_UPDATE_SETTINGS";
    }
    final String target = action;
    new AlertDialog.Builder(this)
        .setTitle(title)
        .setMessage(explanation)
        .setNegativeButton("Close", null)
        .setPositiveButton("Open Android settings", (d, w) -> settings(target))
        .show();
  }

  private void finishStep() {
    title("Keep your AI.\nChoose your home.", 32);
    body(
        "Your existing assistants stay available. For sensitive drafts, an on-device model gives"
            + " you another option.");
    App moto = findMotorolaAI();
    if (moto != null) row(content, moto.label, "Your phone's built-in AI", () -> launch(moto));
    for (String[] pair :
        new String[][] {{"com.openai.chatgpt", "ChatGPT"}, {"ai.x.grok", "Grok"}}) {
      App a = find(pair[0]);
      if (a != null) row(content, pair[1], "Cloud assistant", () -> launch(a));
    }
    row(
        content,
        "PocketPal AI",
        "Optional on-device models; extra download and storage needed",
        () -> openCatalogApp(AppCatalog.get("com.pocketpalai")));
    foot(
        "Cloud assistants receive the content you send. A VPN does not hide prompts from the"
            + " service receiving them.");
    section("Your home screen");
    fact(
        "Default home",
        isHome() ? "Still Home is selected" : "Choose Still Home when you're ready",
        isHome());
    button(content, "Choose your home app", false, this::chooseHome);
    foot(
        "Your original launcher stays available. You can switch back in Android's Default apps"
            + " settings.");
  }

  private void summary() {
    eyebrow("SAVED ON THIS PHONE");
    title("Your setup,\nready to revisit.", 32);
    body(
        "Here is what Android reports now. Anything you left for later remains available in"
            + " Setup.");
    space(18);
    fact(
        "Quick defaults",
        setup.defaultsReady() ? "Both recommended values detected" : "Still needs attention",
        setup.defaultsReady());
    fact("Screen lock", secureLock() ? "A secure lock is set" : "Not detected", secureLock());
    fact("VPN connection", networkLabel(), vpnActive());
    fact(
        "Offline keyboard", heliSelected() ? "HeliBoard selected" : "Not detected", heliSelected());
    section("Your review notes");
    body(
        reviewCount()
            + " of 4 Android controls marked reviewed by you. VPN switches "
            + (reviewed("vpn") ? "marked reviewed" : "not yet marked reviewed")
            + ". These marks do not verify the settings.");
    foot(
        "App choices, account syncing, cloud AI, and manufacturer updates still need ongoing"
            + " attention. No setup can guarantee anonymity or prevent every attack.");
    button(content, "Go to my home", true, () -> show("Home"));
    button(content, "Review the setup", false, () -> show("Setup"));
  }

  private void catalog() {
    eyebrow("OPEN-SOURCE OPTIONS");
    title("Everyday apps", 32);
    body("Choose only what you need. Each suggestion opens its official store or project website.");
    for (AppCatalog app : AppCatalog.ALL) catalogRow(content, app);
    foot(
        "No Internet permission means the app does not request direct network access. Accounts,"
            + " Android backups, exports, and other apps can still move data off your phone. Open"
            + " source alone is not a security guarantee.");
    button(content, "Back to app setup", false, () -> openStep(3));
  }

  private void catalogRow(LinearLayout parent, AppCatalog app) {
    boolean present = installed(app.pkg);
    String detail = app.purpose + " · " + (present ? "Installed" : "Get from official source");
    if (present) {
      try {
        PackageInfo p = getPackageManager().getPackageInfo(app.pkg, PackageManager.GET_PERMISSIONS);
        boolean internet = false;
        if (p.requestedPermissions != null)
          for (String perm : p.requestedPermissions)
            if (perm.equals("android.permission.INTERNET")) internet = true;
        if (!internet) detail += " · No Internet permission requested";
      } catch (PackageManager.NameNotFoundException ignored) {
      }
    }
    final String status = detail;
    row(
        parent,
        app.name,
        status,
        () ->
            new AlertDialog.Builder(this)
                .setTitle(app.name)
                .setMessage(app.detail + "\n\n" + status)
                .setNegativeButton("Close", null)
                .setNeutralButton("Official source", (d, w) -> openSource(app))
                .setPositiveButton(present ? "Open app" : "Get app", (d, w) -> openCatalogApp(app))
                .show());
  }

  private void openCatalogApp(AppCatalog app) {
    if (app == null) return;
    App a = find(app.pkg);
    if (a != null) launch(a);
    else openSource(app);
  }

  private void openSource(AppCatalog app) {
    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(app.url));
    if (app.fdroid
        && installed("org.fdroid.fdroid")
        && tryStart(new Intent(i).setPackage("org.fdroid.fdroid"))) return;
    if (!tryStart(i))
      new AlertDialog.Builder(this)
          .setTitle("Open the official source")
          .setMessage("No browser or app store could open this link:\n\n" + app.url)
          .setPositiveButton("Done", null)
          .show();
  }

  private void ai() {
    eyebrow("AI, ON YOUR TERMS");
    title("Your assistants", 32);
    body(
        "Choose the tool that fits the conversation. Still keeps your existing AI features"
            + " available.");
    App moto = findMotorolaAI();
    if (moto != null) row(content, moto.label, "Built-in Motorola AI", () -> launch(moto));
    for (String[] pair :
        new String[][] {{"com.openai.chatgpt", "ChatGPT"}, {"ai.x.grok", "Grok"}}) {
      App a = find(pair[0]);
      if (a != null)
        row(content, pair[1], "Cloud assistant · prompts go to its service", () -> launch(a));
    }
    section("An option for sensitive drafts");
    row(
        content,
        "PocketPal AI",
        "Run downloaded models on your phone",
        () -> openCatalogApp(AppCatalog.get("com.pocketpalai")));
    body(
        "On-device models need storage and memory. Review the app's optional online features and"
            + " test the model before relying on it.");
    foot(
        "Still does not change assistant permissions, accessibility access, or accounts. Cloud"
            + " services can receive prompts even while you use a VPN.");
    button(
        content,
        "Review AI app permissions",
        false,
        () -> settings(Settings.ACTION_PRIVACY_SETTINGS));
  }

  private void controls() {
    reviewStep();
  }

  private void drawer() {
    eyebrow("YOUR PHONE");
    title("All apps", 32);
    body("Search stays here. Hold an app to see its Android settings.");
    space(16);
    EditText search = new EditText(this);
    search.setSingleLine(true);
    search.setTextSize(16);
    search.setTextColor(ink);
    search.setHintTextColor(muted);
    search.setHint("Find an app");
    search.setContentDescription("Search installed apps");
    search.setMinHeight(dp(56));
    search.setPadding(dp(18), dp(12), dp(18), dp(12));
    search.setBackground(shape(surface, 18));
    search.setInputType(
        android.text.InputType.TYPE_CLASS_TEXT
            | android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
    search.setImeOptions(
        android.view.inputmethod.EditorInfo.IME_ACTION_DONE
            | android.view.inputmethod.EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING);
    content.addView(search);
    LinearLayout grid = column();
    add(grid, 20, 0);
    renderGrid(grid, apps);
    search.addTextChangedListener(
        new TextWatcher() {
          public void beforeTextChanged(CharSequence s, int a, int c, int f) {}

          public void afterTextChanged(Editable e) {}

          public void onTextChanged(CharSequence s, int st, int before, int count) {
            ArrayList<App> filtered = new ArrayList<>();
            String q = s.toString().toLowerCase(Locale.ROOT);
            for (App app : apps)
              if (app.label.toLowerCase(Locale.ROOT).contains(q)) filtered.add(app);
            renderGrid(grid, filtered);
          }
        });
    content.setFocusableInTouchMode(true);
    content.requestFocus();
  }

  private void renderGrid(LinearLayout target, List<App> list) {
    target.removeAllViews();
    int columns = getResources().getConfiguration().fontScale > 1.3f ? 3 : 4;
    if (list.isEmpty()) {
      target.addView(text("No matching apps. Try a shorter name.", 16, muted, false));
      return;
    }
    for (int i = 0; i < list.size(); i += columns) {
      LinearLayout band = new LinearLayout(this);
      band.setBaselineAligned(false);
      for (int j = 0; j < columns; j++) {
        if (i + j >= list.size()) {
          band.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1));
          continue;
        }
        App a = list.get(i + j);
        LinearLayout tile = column();
        tile.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        tile.setPadding(dp(3), dp(12), dp(3), dp(12));
        tile.setBackground(interactive(background, 14));
        ImageView icon = new ImageView(this);
        icon.setImageDrawable(a.icon);
        tile.addView(icon, new LinearLayout.LayoutParams(dp(44), dp(44)));
        TextView label = text(a.label, 12, ink, false);
        label.setGravity(Gravity.CENTER);
        label.setMaxLines(2);
        label.setEllipsize(TextUtils.TruncateAt.END);
        label.setPadding(0, dp(8), 0, 0);
        tile.addView(label);
        tile.setContentDescription(a.label);
        tile.setFocusable(true);
        tile.setOnClickListener(v -> launch(a));
        tile.setOnLongClickListener(
            v -> {
              appInfo(a.pkg);
              return true;
            });
        band.addView(tile, new LinearLayout.LayoutParams(0, -1, 1));
      }
      target.addView(band, new LinearLayout.LayoutParams(-1, -2));
    }
  }

  private void loadApps() {
    apps.clear();
    Intent query = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
    HashSet<String> seen = new HashSet<>();
    for (ResolveInfo r : getPackageManager().queryIntentActivities(query, 0)) {
      if (r.activityInfo.packageName.equals(getPackageName())) continue;
      ComponentName c = new ComponentName(r.activityInfo.packageName, r.activityInfo.name);
      if (!seen.add(c.flattenToString())) continue;
      App a = new App();
      a.pkg = r.activityInfo.packageName;
      a.component = c;
      a.label = r.loadLabel(getPackageManager()).toString();
      a.icon = r.loadIcon(getPackageManager());
      apps.add(a);
    }
    apps.sort((a, b) -> Collator.getInstance().compare(a.label, b.label));
  }

  private App find(String pkg) {
    for (App a : apps) if (a.pkg.equals(pkg)) return a;
    return null;
  }

  private boolean installed(String pkg) {
    return find(pkg) != null;
  }

  private App findMotorolaAI() {
    for (String pkg : new String[] {"com.lenovo.qira", "com.motorola.uxcore"}) {
      App a = find(pkg);
      if (a != null) return a;
    }
    for (App a : apps) {
      String label = a.label.toLowerCase(Locale.ROOT).replace(" ", "");
      if (a.pkg.startsWith("com.motorola.") && (label.contains("qira") || label.contains("motoai")))
        return a;
    }
    return null;
  }

  private boolean secureLock() {
    return getSystemService(KeyguardManager.class).isDeviceSecure();
  }

  private boolean heliSelected() {
    try {
      String current =
          Settings.Secure.getString(getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
      return current != null && current.startsWith("helium314.keyboard/");
    } catch (SecurityException e) {
      return false;
    }
  }

  private boolean isHome() {
    RoleManager r = getSystemService(RoleManager.class);
    return r != null
        && r.isRoleAvailable(RoleManager.ROLE_HOME)
        && r.isRoleHeld(RoleManager.ROLE_HOME);
  }

  private NetworkCapabilities capabilities() {
    try {
      return connectivity.getNetworkCapabilities(connectivity.getActiveNetwork());
    } catch (RuntimeException e) {
      return null;
    }
  }

  private boolean vpnActive() {
    NetworkCapabilities c = capabilities();
    return c != null && c.hasTransport(NetworkCapabilities.TRANSPORT_VPN);
  }

  private String networkLabel() {
    NetworkCapabilities c = capabilities();
    return c == null
        ? "No active connection reported"
        : c.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
            ? "VPN connection detected"
            : "VPN connection not detected";
  }

  private void refreshNetwork() {
    handler.post(
        () -> {
          if (connectionLabel != null) connectionLabel.setText(networkLabel());
        });
  }

  private String timeoutLabel() {
    Integer t = setup.read(PrivacySetup.TIMEOUT);
    return t == null
        ? "Not available to this app"
        : t <= 0
            ? "Review in Android settings"
            : t < 60000
                ? (t / 1000) + " seconds detected"
                : t == 60000 ? "1 minute detected" : (t / 60000) + " minutes detected";
  }

  private String passwordLabel() {
    Integer v = setup.read(PrivacySetup.PASSWORD);
    return v == null
        ? "Not available to this app"
        : v == 0 ? "Brief character previews are off" : "Brief character previews are on";
  }

  private String[] core() {
    return new String[] {
      "org.fdroid.fdroid",
      "org.mozilla.firefox",
      "helium314.keyboard",
      "org.fossify.gallery",
      "org.fossify.filemanager"
    };
  }

  private int installedCore() {
    int count = 0;
    for (String p : core()) if (installed(p)) count++;
    return count;
  }

  private boolean reviewed(String key) {
    return setup.state.getBoolean("review_" + key, false);
  }

  private int reviewCount() {
    int count = 0;
    for (String key : new String[] {"lock", "permissions", "ads", "updates"})
      if (reviewed(key)) count++;
    return count;
  }

  private void reviewBox(String key, String label) {
    CheckBox box = new CheckBox(this);
    box.setText(label);
    box.setTextSize(14);
    box.setTextColor(ink);
    box.setMinHeight(dp(56));
    box.setButtonTintList(ColorStateList.valueOf(accent));
    box.setChecked(reviewed(key));
    box.setOnCheckedChangeListener(
        (b, checked) -> setup.state.edit().putBoolean("review_" + key, checked).apply());
    add(box, 8, 0);
  }

  private void chooseHome() {
    RoleManager r = getSystemService(RoleManager.class);
    if (r != null && r.isRoleAvailable(RoleManager.ROLE_HOME) && !isHome())
      startActivityForResult(r.createRequestRoleIntent(RoleManager.ROLE_HOME), 101);
    else settings(Settings.ACTION_HOME_SETTINGS);
  }

  private void chooseAppearance() {
    String[] choices = {"Dark", "Light", "System"};
    new AlertDialog.Builder(this)
        .setTitle("Appearance")
        .setSingleChoiceItems(
            choices,
            Arrays.asList(choices).indexOf(themeMode()),
            (dialog, which) -> {
              boolean changed = !choices[which].equals(themeMode());
              getPreferences(MODE_PRIVATE).edit().putString("theme", choices[which]).apply();
              dialog.dismiss();
              if (changed) recreate();
            })
        .setNegativeButton("Cancel", null)
        .show();
  }

  private void about() {
    new AlertDialog.Builder(this)
        .setTitle("Privacy, with clear limits")
        .setMessage(
            "Still Home 2.0 works without Internet access, ads, analytics, or an account. It stores"
                + " setup progress, review notes, theme choice, and undo values only on this phone."
                + " Android cloud backup is disabled.\n\n"
                + "Automatic setup begins after you open the app and choose Apply recommended"
                + " settings. Android asks for permission to change the screen timeout and"
                + " password-character previews. No other system settings are written. Undo"
                + " preserves newer changes you make yourself.\n\n"
                + "VPN, keyboard, default apps, permissions, and app installations need Android or"
                + " store confirmations. Review marks are your notes, not verified protections.\n\n"
                + "Existing AI stays available. Cloud assistants receive submitted prompts. A"
                + " launcher cannot add GrapheneOS protections, verify every network route, or"
                + " guarantee anonymity.\n\n"
                + "This is an early open-source project, not an independently audited security"
                + " product.")
        .setNegativeButton("Close", null)
        .setPositiveButton(
            "Source and updates",
            (d, w) ->
                tryStart(
                    new Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://github.com/sampathmannam/still-home"))))
        .show();
  }

  private void launch(App a) {
    if (!tryStart(
        new Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(a.component)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)))
      inform("This app could not open. Check its Android app settings.");
  }

  private void appInfo(String pkg) {
    tryStart(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + pkg)));
  }

  private void settings(String action) {
    if (!tryStart(new Intent(action))) {
      inform("This phone uses a different menu. Find the control in Android Settings.");
      tryStart(new Intent(Settings.ACTION_SETTINGS));
    }
  }

  private boolean tryStart(Intent i) {
    try {
      startActivity(i);
      return true;
    } catch (RuntimeException e) {
      return false;
    }
  }

  private void inform(String text) {
    new AlertDialog.Builder(this).setMessage(text).setPositiveButton("Done", null).show();
  }

  private void hideKeyboard() {
    ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
        .hideSoftInputFromWindow(root.getWindowToken(), 0);
  }

  private LinearLayout column() {
    LinearLayout v = new LinearLayout(this);
    v.setOrientation(LinearLayout.VERTICAL);
    return v;
  }

  private void brand() {
    LinearLayout row = new LinearLayout(this);
    row.setGravity(Gravity.CENTER_VERTICAL);
    ImageView mark = new ImageView(this);
    mark.setImageResource(R.drawable.icon);
    row.addView(mark, new LinearLayout.LayoutParams(dp(32), dp(32)));
    TextView name = text("Still Home", 16, ink, true);
    name.setPadding(dp(12), 0, 0, 0);
    row.addView(name);
    content.addView(row);
  }

  private LinearLayout panel() {
    LinearLayout v = column();
    v.setPadding(dp(20), dp(18), dp(20), dp(14));
    v.setBackground(shape(surface, 24));
    content.addView(v);
    return v;
  }

  private void label(LinearLayout p, String value) {
    TextView t = text(value, 11, accent, true);
    t.setLetterSpacing(.10f);
    t.setPadding(0, 0, 0, dp(10));
    p.addView(t);
  }

  private void eyebrow(String value) {
    label(content, value);
  }

  private void title(String value, int size) {
    TextView t = text(value, size, ink, true);
    t.setAccessibilityHeading(true);
    add(t, 0, 14);
  }

  private void body(String value) {
    add(text(value, 16, muted, false), 0, 12);
  }

  private void foot(String value) {
    add(text(value, 13, muted, false), 12, 8);
  }

  private void section(String value) {
    TextView t = text(value, 16, ink, true);
    t.setAccessibilityHeading(true);
    add(t, 26, 6);
  }

  private void feature(String title, String body) {
    add(text(title, 18, ink, true), 0, 6);
    add(text(body, 15, muted, false), 0, 22);
  }

  private void instruction(String value) {
    add(text(value, 15, ink, false), 5, 10);
  }

  private void message(String value) {
    TextView t = text(value, 14, ink, false);
    t.setPadding(dp(16), dp(14), dp(16), dp(14));
    t.setBackground(shape(tone, 16));
    t.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
    add(t, 12, 12);
  }

  private void fact(String title, String value, boolean ready) {
    LinearLayout p = column();
    p.setPadding(0, dp(12), 0, dp(12));
    p.addView(text(title, 14, muted, false));
    TextView t = text(value, 17, ready ? accent : ink, true);
    t.setPadding(0, dp(5), 0, 0);
    p.addView(t);
    content.addView(p);
    divider(content);
  }

  private void row(LinearLayout parent, String title, String subtitle, Runnable action) {
    LinearLayout r = new LinearLayout(this);
    r.setGravity(Gravity.CENTER_VERTICAL);
    r.setMinimumHeight(dp(80));
    r.setPadding(dp(2), dp(16), dp(2), dp(16));
    r.setBackground(interactive(background, 12));
    LinearLayout words = column();
    words.addView(text(title, 17, ink, true));
    TextView detail = text(subtitle, 14, muted, false);
    detail.setPadding(0, dp(5), dp(10), 0);
    words.addView(detail);
    r.addView(words, new LinearLayout.LayoutParams(0, -2, 1));
    r.addView(text("›", 26, accent, false));
    r.setOnClickListener(v -> action.run());
    r.setFocusable(true);
    r.setContentDescription(title + ". " + subtitle);
    parent.addView(r);
    divider(parent);
  }

  private void divider(LinearLayout parent) {
    View v = new View(this);
    v.setBackgroundColor(line);
    parent.addView(v, new LinearLayout.LayoutParams(-1, dp(1)));
  }

  private void button(LinearLayout parent, String value, boolean primary, Runnable action) {
    Button b = new Button(this);
    b.setText(value);
    b.setAllCaps(false);
    b.setTextSize(15);
    b.setTypeface(Typeface.create("sans-serif-medium", 0));
    b.setTextColor(primary ? onAccent : accent);
    b.setMinHeight(dp(52));
    b.setMinimumHeight(dp(52));
    b.setPadding(dp(16), dp(12), dp(16), dp(12));
    b.setBackground(interactive(primary ? accent : tone, 18));
    b.setOnClickListener(v -> action.run());
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
    lp.topMargin = dp(10);
    parent.addView(b, lp);
  }

  private void link(String value, Runnable action) {
    TextView t = text(value, 14, accent, true);
    t.setMinHeight(dp(48));
    t.setGravity(Gravity.CENTER_VERTICAL);
    t.setBackground(interactive(background, 10));
    t.setFocusable(true);
    t.setOnClickListener(v -> action.run());
    add(t, 6, 0);
  }

  private TextView text(String value, float size, int color, boolean medium) {
    TextView t = new TextView(this);
    t.setText(value);
    t.setTextSize(size);
    t.setTextColor(color);
    t.setTypeface(Typeface.create(medium ? "sans-serif-medium" : "sans-serif", 0));
    t.setLineSpacing(dp(3), 1);
    return t;
  }

  private void add(View v, int top, int bottom) {
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
    lp.setMargins(0, dp(top), 0, dp(bottom));
    content.addView(v, lp);
  }

  private void space(int height) {
    View v = new View(this);
    content.addView(v, new LinearLayout.LayoutParams(1, dp(height)));
  }

  private GradientDrawable shape(int color, int radius) {
    GradientDrawable d = new GradientDrawable();
    d.setColor(color);
    d.setCornerRadius(dp(radius));
    return d;
  }

  private Drawable interactive(int color, int radius) {
    int glow = dark ? 0x228bd0cc : 0x2229616e;
    StateListDrawable states = new StateListDrawable();
    GradientDrawable focus = shape(color, radius);
    focus.setStroke(dp(2), accent);
    states.addState(new int[] {android.R.attr.state_focused}, focus);
    states.addState(new int[] {}, shape(color, radius));
    return new RippleDrawable(ColorStateList.valueOf(glow), states, shape(0xffffffff, radius));
  }

  private int dp(float v) {
    return Math.round(v * getResources().getDisplayMetrics().density);
  }
}
