package org.stillhome.launcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import java.util.ArrayList;

/** Only two public System settings are writable here, after Android consent. */
final class PrivacySetup {
  static final String TIMEOUT = Settings.System.SCREEN_OFF_TIMEOUT;
  static final String PASSWORD = Settings.System.TEXT_SHOW_PASSWORD;
  private final Context context;
  final SharedPreferences state;

  PrivacySetup(Context context) {
    this.context = context;
    state = context.getSharedPreferences("privacy_setup", Context.MODE_PRIVATE);
  }

  Integer read(String key) {
    try {
      return Settings.System.getInt(context.getContentResolver(), key);
    } catch (Settings.SettingNotFoundException | SecurityException e) {
      return null;
    }
  }

  boolean canWrite() {
    return Settings.System.canWrite(context);
  }

  boolean defaultsReady() {
    return PrivacyPolicy.hasShortTimeout(read(TIMEOUT))
        && Integer.valueOf(0).equals(read(PASSWORD));
  }

  boolean hasUndo() {
    return state.contains("before_" + TIMEOUT) || state.contains("before_" + PASSWORD);
  }

  String apply() {
    if (!canWrite())
      return "Nothing changed. Android needs your permission, or you can use the settings below.";
    ArrayList<String> result = new ArrayList<>();
    Integer timeout = read(TIMEOUT);
    if (timeout == null) result.add("Screen timeout needs a manual review.");
    else result.add(change(TIMEOUT, PrivacyPolicy.recommendedTimeout(timeout), "Screen timeout"));
    result.add(change(PASSWORD, 0, "Password characters"));
    return String.join(" ", result);
  }

  private String change(String key, int desired, String label) {
    Integer before = read(key);
    if (before == null) return label + " could not be read; left unchanged.";
    if (before == desired) return label + " already meets the recommendation.";
    // Persist the undo record before changing the setting, including if the process is stopped.
    if (!state.edit().putInt("before_" + key, before).putInt("applied_" + key, desired).commit())
      return label + " left unchanged because its undo record could not be saved.";
    try {
      Settings.System.putInt(context.getContentResolver(), key, desired);
      if (Integer.valueOf(desired).equals(read(key))) return label + " updated and checked.";
    } catch (RuntimeException ignored) {
    }
    // Keep an ambiguous record for a later recheck/undo; never claim success.
    return label + " was not confirmed. Review it in Android settings.";
  }

  String restore() {
    if (!canWrite()) return "Nothing restored. Allow system-setting changes to use undo.";
    ArrayList<String> result = new ArrayList<>();
    for (String key : new String[] {TIMEOUT, PASSWORD}) {
      if (!state.contains("before_" + key)) continue;
      String label = key.equals(TIMEOUT) ? "Screen timeout" : "Password characters";
      Integer current = read(key);
      if (current == null) {
        result.add(label + " could not be checked; undo is still available.");
        continue;
      }
      if (!PrivacyPolicy.shouldRestore(current, state.getInt("applied_" + key, -1))) {
        clearRecord(key);
        result.add(label + ": kept your newer change.");
        continue;
      }
      int previous = state.getInt("before_" + key, current);
      try {
        Settings.System.putInt(context.getContentResolver(), key, previous);
        if (Integer.valueOf(previous).equals(read(key))) {
          clearRecord(key);
          result.add(label + " restored.");
          continue;
        }
      } catch (RuntimeException ignored) {
      }
      result.add(label + " could not be restored; its undo record was kept.");
    }
    return result.isEmpty() ? "No automatic changes to undo." : String.join(" ", result);
  }

  private void clearRecord(String key) {
    state.edit().remove("before_" + key).remove("applied_" + key).commit();
  }
}
