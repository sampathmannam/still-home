package org.stillhome.launcher;

import android.app.*;
import android.app.role.RoleManager;
import android.content.*;
import android.content.pm.*;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.*;
import android.os.*;
import android.provider.Settings;
import android.text.*;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.text.*;
import java.util.*;

/** Local-only launcher. It never implements a browser, VPN, or privileged security service. */
public final class HomeActivity extends Activity {
    private static final int WHITE = 0xffffffff;
    private int PAPER, INK, MUTED, TEAL, SURFACE, LINE, CARD, SELECTED, ON_SELECTED;
    private boolean dark;

    private String themeMode() { return getPreferences(MODE_PRIVATE).getString("theme", "Dark"); }
    private void configureTheme() {
        String mode = themeMode();
        dark = mode.equals("Dark") || (mode.equals("System") &&
                (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES);
        setTheme(dark ? R.style.AppTheme : R.style.AppThemeLight);
        PAPER = dark ? 0xff10181e : 0xffeceff0;
        INK = dark ? 0xffe4edf2 : 0xff15232e;
        MUTED = dark ? 0xffa8b8c1 : 0xff596c74;
        TEAL = dark ? 0xff8bd0cc : 0xff29616e;
        SURFACE = dark ? 0xff1b2831 : WHITE;
        LINE = dark ? 0xff354750 : 0xffcdd6da;
        CARD = dark ? 0xff244b52 : 0xff29616e;
        SELECTED = dark ? 0xff8bd0cc : 0xff15232e;
        ON_SELECTED = dark ? 0xff10181e : WHITE;
    }
    private LinearLayout root, content, nav;
    private ScrollView scroll;
    private String page = "Home";
    private final ArrayList<App> apps = new ArrayList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView vpnTitle, vpnDetail;
    private ConnectivityManager connectivity;
    private boolean callbackRegistered;
    private final ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
        @Override public void onAvailable(Network n) { refreshNetwork(); }
        @Override public void onLost(Network n) { refreshNetwork(); }
        @Override public void onCapabilitiesChanged(Network n, NetworkCapabilities c) { refreshNetwork(); }
    };

    private static final class App {
        String label, pkg;
        ComponentName component;
        android.graphics.drawable.Drawable icon;
    }

    @Override public void onCreate(Bundle state) {
        configureTheme();
        super.onCreate(state);
        if (state != null) page = state.getString("page", "Home");
        connectivity = getSystemService(ConnectivityManager.class);
        getWindow().setDecorFitsSystemWindows(false);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(PAPER);
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            int keyboard = insets.getInsets(WindowInsets.Type.ime()).bottom;
            v.setPadding(bars.left, bars.top, bars.right, Math.max(bars.bottom, keyboard));
            return insets;
        });
        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(24), dp(24), dp(24), dp(24));
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        nav = new LinearLayout(this);
        nav.setPadding(dp(12), dp(8), dp(12), dp(8));
        root.addView(nav);
        setContentView(root);
        getWindow().getInsetsController().setSystemBarsAppearance(
            dark ? 0 : WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
        if (Build.VERSION.SDK_INT >= 33) getOnBackInvokedDispatcher().registerOnBackInvokedCallback(0, () -> show("Home"));
    }

    @Override protected void onResume() {
        super.onResume();
        loadApps();
        show(page);
        if (!callbackRegistered) {
            try { connectivity.registerDefaultNetworkCallback(networkCallback); callbackRegistered = true; }
            catch (RuntimeException ignored) { }
        }
    }

    @Override protected void onPause() {
        if (callbackRegistered) {
            try { connectivity.unregisterNetworkCallback(networkCallback); } catch (RuntimeException ignored) { }
            callbackRegistered = false;
        }
        super.onPause();
    }

    @Override protected void onNewIntent(Intent i) { super.onNewIntent(i); show("Home"); }
    @Override protected void onSaveInstanceState(Bundle b) { b.putString("page", page); super.onSaveInstanceState(b); }
    @Override public void onBackPressed() { show("Home"); }

    private void loadApps() {
        apps.clear();
        Intent i = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        HashSet<String> seen = new HashSet<>();
        for (ResolveInfo r : getPackageManager().queryIntentActivities(i, 0)) {
            if (r.activityInfo.packageName.equals(getPackageName())) continue;
            ComponentName component = new ComponentName(r.activityInfo.packageName, r.activityInfo.name);
            if (!seen.add(component.flattenToString())) continue;
            App a = new App(); a.pkg = r.activityInfo.packageName; a.component = component;
            a.label = r.loadLabel(getPackageManager()).toString(); a.icon = r.loadIcon(getPackageManager()); apps.add(a);
        }
        apps.sort((a, b) -> Collator.getInstance().compare(a.label, b.label));
    }

    private void show(String name) {
        page = name;
        View focus = getCurrentFocus();
        if (focus != null) ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(focus.getWindowToken(), 0);
        content.removeAllViews(); vpnTitle = null; vpnDetail = null;
        if (name.equals("Home")) home();
        else if (name.equals("Apps")) appDrawer();
        else if (name.equals("Privacy")) privacy();
        else ai();
        nav.removeAllViews();
        for (String tab : new String[]{"Home", "Apps", "Privacy", "AI"}) {
            TextView t = text(tab, 14, name.equals(tab) ? ON_SELECTED : MUTED, true);
            t.setGravity(Gravity.CENTER); t.setMinHeight(dp(50));
            t.setBackground(shape(name.equals(tab) ? SELECTED : PAPER, 18));
            t.setOnClickListener(v -> show(tab)); t.setContentDescription(tab + " tab");
            t.setSelected(name.equals(tab)); t.setFocusable(true);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1); lp.setMargins(dp(2), 0, dp(2), 0);
            nav.addView(t, lp);
        }
        scroll.post(() -> scroll.scrollTo(0, 0));
    }

    private void home() {
        eyebrow("STILL HOME");
        TextClock clock = new TextClock(this);
        clock.setFormat12Hour("h:mm"); clock.setFormat24Hour("HH:mm");
        clock.setTextSize(68); clock.setTextColor(INK); clock.setTypeface(Typeface.create("sans-serif-light", 0));
        content.addView(clock);
        add(text(new SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(new Date()), 17, MUTED, false), 0, 22);
        networkCard();
        section("Browsing");
        appRow("org.torproject.torbrowser", "Tor Browser", "For browsing with stronger fingerprinting resistance");
        appRow("org.mozilla.firefox", "Firefox", "Everyday browsing");
        section("Your essentials");
        LinearLayout grid = new LinearLayout(this); grid.setOrientation(LinearLayout.VERTICAL);
        ArrayList<App> essentials = new ArrayList<>();
        for (String p : new String[]{"com.google.android.dialer", "com.google.android.apps.messaging", "com.motorola.camera5", "org.fossify.gallery"}) {
            App a = find(p); if (a != null) essentials.add(a);
        }
        renderGrid(grid, essentials, 4); content.addView(grid);
        row(content, "AI, within reach", "Motorola AI and your assistants", "↗", () -> show("AI"));
        add(text("This home screen works offline. No ads, analytics, or account.", 13, MUTED, false), 18, 0);
    }

    private void networkCard() {
        LinearLayout card = new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(20), dp(18), dp(20), dp(18)); card.setBackground(shape(CARD, 22));
        TextView label = text("CONNECTION", 11, 0xffbedadb, true); label.setLetterSpacing(.14f); card.addView(label);
        vpnTitle = text("Checking connection", 23, WHITE, true); card.addView(vpnTitle);
        vpnDetail = text("", 13, WHITE, false); vpnDetail.setPadding(0, dp(8), 0, dp(14)); card.addView(vpnDetail);
        TextView button = text("Open VPN controls  ↗", 14, WHITE, true); button.setMinHeight(dp(48));
        button.setGravity(Gravity.CENTER_VERTICAL); button.setFocusable(true); button.setOnClickListener(v -> settings(Settings.ACTION_VPN_SETTINGS));
        card.addView(button); content.addView(card); updateNetwork();
    }

    private void refreshNetwork() { handler.post(this::updateNetwork); }
    private void updateNetwork() {
        if (vpnTitle == null) return;
        try {
            NetworkCapabilities c = connectivity.getNetworkCapabilities(connectivity.getActiveNetwork());
            if (c == null) { vpnTitle.setText("No active connection"); vpnDetail.setText("Connect to a network, then check your VPN."); }
            else if (c.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                vpnTitle.setText("VPN detected");
                vpnDetail.setText(page.equals("Home") ? "Reported by Android. Review always-on protection in VPN controls." : "Android reports a VPN for this connection. This does not verify your public IP or prevent fingerprinting.");
            } else { vpnTitle.setText("VPN not detected"); vpnDetail.setText("Check Proton VPN before browsing. This screen does not test for leaks."); }
        } catch (RuntimeException e) { vpnTitle.setText("Connection status unavailable"); vpnDetail.setText("Check your VPN in Android settings."); }
    }

    private void appDrawer() {
        eyebrow("YOUR PHONE"); title("All apps");
        add(text("Search stays on this phone. Hold an app for its settings.", 14, MUTED, false), 0, 18);
        EditText search = new EditText(this); search.setSingleLine(true); search.setTextSize(17);
        search.setTextColor(INK); search.setHintTextColor(MUTED); search.setHint("Find an app");
        search.setContentDescription("Search installed apps"); search.setPadding(dp(18), dp(12), dp(18), dp(12));
        search.setBackground(shape(SURFACE, 16));
        search.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        search.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE | android.view.inputmethod.EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING);
        content.addView(search);
        LinearLayout grid = new LinearLayout(this); grid.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams gl = new LinearLayout.LayoutParams(-1, -2); gl.topMargin = dp(18); content.addView(grid, gl);
        renderGrid(grid, apps, 4);
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int c, int f) { }
            public void onTextChanged(CharSequence s, int st, int before, int count) {
                String q = s.toString().toLowerCase(Locale.ROOT); ArrayList<App> filtered = new ArrayList<>();
                for (App app : apps) if (app.label.toLowerCase(Locale.ROOT).contains(q)) filtered.add(app);
                renderGrid(grid, filtered, 4);
            }
            public void afterTextChanged(Editable e) { }
        });
        content.setFocusableInTouchMode(true); content.requestFocus();
    }

    private void renderGrid(LinearLayout target, List<App> list, int columns) {
        target.removeAllViews();
        if (list.isEmpty()) { target.addView(text("No matching apps.", 16, MUTED, false)); return; }
        for (int i = 0; i < list.size(); i += columns) {
            LinearLayout line = new LinearLayout(this);
            for (int j = 0; j < columns; j++) {
                if (i+j >= list.size()) { line.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1)); continue; }
                App a = list.get(i+j); LinearLayout tile = new LinearLayout(this);
                tile.setOrientation(LinearLayout.VERTICAL); tile.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
                tile.setPadding(dp(2), dp(10), dp(2), dp(10));
                ImageView icon = new ImageView(this); icon.setImageDrawable(a.icon);
                tile.addView(icon, new LinearLayout.LayoutParams(dp(44), dp(44)));
                TextView label = text(a.label, 12, INK, false); label.setGravity(Gravity.CENTER); label.setMaxLines(2);
                label.setEllipsize(TextUtils.TruncateAt.END); label.setPadding(0, dp(8), 0, 0);
                tile.addView(label); tile.setContentDescription(a.label); tile.setFocusable(true);
                tile.setOnClickListener(v -> launch(a)); tile.setOnLongClickListener(v -> { appInfo(a.pkg); return true; });
                line.addView(tile, new LinearLayout.LayoutParams(0, dp(108), 1));
            }
            target.addView(line);
        }
    }

    private void privacy() {
        eyebrow("CLEAR CONTROLS"); title("Privacy");
        add(text("See what Android reports. Choose what your apps can access.", 16, MUTED, false), 0, 20);
        networkCard();
        section("Device protection");
        KeyguardManager keyguard = getSystemService(KeyguardManager.class);
        row(content, keyguard.isDeviceSecure() ? "Screen lock is set" : "Set a screen lock", "Manage your PIN, password, or fingerprint", "↗", () -> settings(Settings.ACTION_SECURITY_SETTINGS));
        row(content, "Security patch · " + Build.VERSION.SECURITY_PATCH, "Android " + Build.VERSION.RELEASE + " · check for system updates", "↗", () -> settings("android.settings.SYSTEM_UPDATE_SETTINGS"));
        row(content, "App permissions", "Review camera, microphone, location and more", "↗", () -> settings(Settings.ACTION_PRIVACY_SETTINGS));
        row(content, "Advertising privacy", "Manage Android's ad controls", "↗", () -> settings("android.adservices.ui.SETTINGS"));
        row(content, "Location", "Review location access and scanning", "↗", () -> settings(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
        section("Open-source essentials");
        appRow("org.fdroid.fdroid", "F-Droid", "Check for app updates");
        appRow("helium314.keyboard", "HeliBoard", "Offline keyboard settings");
        appRow("org.fossify.filemanager", "Files", "Local file manager");
        appRow("org.fossify.gallery", "Gallery", "Local photos and videos");
        for (String[] entry : new String[][]{
            {"org.fossify.contacts", "Contacts", "Manage your address book"},
            {"org.fossify.calendar", "Calendar", "Events and reminders"},
            {"org.fossify.notes", "Notes", "Keep notes on your phone"},
            {"org.fossify.math", "Calculator", "Everyday calculations"},
            {"org.fossify.clock", "Clock", "Alarms and timers"},
            {"org.fossify.musicplayer", "Music", "Play local audio"}}) {
            if (find(entry[0]) != null) appRow(entry[0], entry[1], entry[2]);
        }
        section("Your home screen");
        row(content, "Appearance", themeMode() + " theme", "↗", this::chooseAppearance);
        row(content, "Phone display", "Android's dark theme and display settings", "↗", () -> settings(Settings.ACTION_DISPLAY_SETTINGS));
        row(content, "Choose your home app", "Use Still Home or return to Moto Launcher", "↗", this::chooseHome);
        row(content, "What this protects", "Read the scope and privacy policy", "+", this::about);
    }

    private void ai() {
        eyebrow("AI, ON YOUR TERMS"); title("Your assistants");
        add(text("Your existing AI features stay available. Each assistant keeps its own permissions and privacy settings.", 16, MUTED, false), 0, 22);
        App motorolaAI = findMotorolaAI();
        if (motorolaAI != null) row(content, motorolaAI.label, "Your phone's built-in AI features", "↗", () -> launch(motorolaAI));
        else row(content, "Motorola AI", "Review your phone's available assistant features", "↗", () -> settings(Settings.ACTION_SETTINGS));
        appRow("com.openai.chatgpt", "ChatGPT", "Cloud assistant · content is sent to its service");
        appRow("ai.x.grok", "Grok", "Cloud assistant · content is sent to its service");
        section("More private conversations");
        if (find("com.pocketpalai") != null) appRow("com.pocketpalai", "PocketPal AI", "Run downloaded models on your phone");
        else row(content, "Explore on-device AI", "PocketPal is an open-source option; models need extra storage", "↗", () -> openWeb("https://github.com/a-ghorbani/pocketpal-ai"));
        add(text("A VPN does not hide messages from the AI service receiving them. Avoid sharing information you want to keep only on your phone.", 14, MUTED, false), 20, 8);
        row(content, "Review assistant permissions", "Android privacy controls", "↗", () -> settings(Settings.ACTION_PRIVACY_SETTINGS));
        row(content, "Motorola AI access", "Review accessibility access without turning AI off", "↗", () -> settings(Settings.ACTION_ACCESSIBILITY_SETTINGS));
    }

    private void chooseAppearance() {
        String[] choices = {"Dark", "Light", "System"};
        int selected = Arrays.asList(choices).indexOf(themeMode());
        new AlertDialog.Builder(this).setTitle("Appearance")
            .setSingleChoiceItems(choices, selected, (dialog, which) -> {
                String selectedMode = choices[which];
                boolean changed = !selectedMode.equals(themeMode());
                getPreferences(MODE_PRIVATE).edit().putString("theme", selectedMode).apply();
                dialog.dismiss();
                if (changed) recreate();
            }).setNegativeButton("Cancel", null).show();
    }

    private void about() {
        new AlertDialog.Builder(this).setTitle("Built to stay local")
          .setMessage("Still Home is an open-source launcher, not an operating system.\n\nIt has no Internet permission, analytics, account, ads, or cloud backup. It reads launchable app names/icons and Android's network status locally. Search text is not saved. Your theme choice is stored only on this phone.\n\nOpening an app or website hands control to that app. Its own privacy policy applies.\n\nVPN detected means Android reports a VPN transport. It is not a leak test or proof that every app is protected. Still Home cannot hide browser fingerprints, change other apps' permissions silently, add GrapheneOS protections, or guarantee anonymity.\n\nKeep Android and your apps updated. For Tor browsing, avoid personal logins when you want to keep your identity separate.\n\nSource code and the MIT license are supplied with your setup files. This first version has not had an independent security audit.")
          .setPositiveButton("Done", null).show();
    }

    private void chooseHome() {
        RoleManager roles = getSystemService(RoleManager.class);
        if (roles != null && roles.isRoleAvailable(RoleManager.ROLE_HOME) && !roles.isRoleHeld(RoleManager.ROLE_HOME)) {
            startActivityForResult(roles.createRequestRoleIntent(RoleManager.ROLE_HOME), 101);
        } else settings(Settings.ACTION_HOME_SETTINGS);
    }

    private App find(String pkg) { for (App a : apps) if (a.pkg.equals(pkg)) return a; return null; }
    private App findMotorolaAI() {
        App qira = find("com.lenovo.qira");
        if (qira != null) return qira;
        for (App a : apps) {
            String label = a.label.toLowerCase(Locale.ROOT).replace(" ", "");
            if (a.pkg.startsWith("com.motorola.") && label.contains("qira")) return a;
        }
        App classic = find("com.motorola.uxcore");
        if (classic != null) return classic;
        for (App a : apps) if (a.pkg.startsWith("com.motorola.") && a.label.toLowerCase(Locale.ROOT).replace(" ", "").contains("motoai")) return a;
        return null;
    }
    private void appRow(String pkg, String title, String subtitle) {
        App app = find(pkg);
        row(content, title, app != null ? subtitle : "Not installed · open app store", "↗", () -> {
            if (app != null) launch(app);
            else if (!tryStart(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + pkg)))) toast("Open your app store to find " + title);
        });
    }
    private void launch(App a) {
        Intent i = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(a.component);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        if (!tryStart(i)) toast("This app is unavailable. Check its app settings.");
    }
    private void appInfo(String pkg) { tryStart(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + pkg))); }
    private void settings(String action) { if (!tryStart(new Intent(action))) { toast("Open the relevant control in Settings."); tryStart(new Intent(Settings.ACTION_SETTINGS)); } }
    private void openWeb(String url) { if (!tryStart(new Intent(Intent.ACTION_VIEW, Uri.parse(url)))) toast("No browser is available."); }
    private boolean tryStart(Intent intent) { try { startActivity(intent); return true; } catch (RuntimeException e) { return false; } }
    private void toast(String message) { Toast.makeText(this, message, Toast.LENGTH_LONG).show(); }

    private void row(LinearLayout parent, String title, String subtitle, String mark, Runnable action) {
        LinearLayout row = new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(15), 0, dp(15)); row.setMinimumHeight(dp(76));
        LinearLayout words = new LinearLayout(this); words.setOrientation(LinearLayout.VERTICAL);
        words.addView(text(title, 17, INK, true));
        TextView description = text(subtitle, 13, MUTED, false); description.setPadding(0, dp(5), dp(12), 0); words.addView(description);
        row.addView(words, new LinearLayout.LayoutParams(0, -2, 1));
        TextView arrow = text(mark, 23, TEAL, false); row.addView(arrow);
        row.setOnClickListener(v -> action.run()); row.setFocusable(true); row.setContentDescription(title + ". " + subtitle);
        parent.addView(row); View line = new View(this); line.setBackgroundColor(LINE); parent.addView(line, new LinearLayout.LayoutParams(-1, dp(1)));
    }
    private void eyebrow(String value) { TextView t = text(value, 11, TEAL, true); t.setLetterSpacing(.15f); add(t, 0, 10); }
    private void title(String value) { add(text(value, 36, INK, true), 0, 12); }
    private void section(String value) { add(text(value, 13, TEAL, true), 25, 3); }
    private TextView text(String value, float size, int color, boolean medium) {
        TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color);
        t.setTypeface(Typeface.create(medium ? "sans-serif-medium" : "sans-serif", 0));
        t.setLineSpacing(dp(2), 1); return t;
    }
    private void add(View v, int top, int bottom) { LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2); lp.setMargins(0, dp(top), 0, dp(bottom)); content.addView(v, lp); }
    private GradientDrawable shape(int color, int radius) { GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d; }
    private int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
