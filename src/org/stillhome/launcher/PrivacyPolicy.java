package org.stillhome.launcher;

/** Small, platform-independent rules for changes and undo. */
public final class PrivacyPolicy {
  private PrivacyPolicy() {}

  public static int recommendedTimeout(int current) {
    return current > 0 && current <= 60000 ? current : 60000;
  }

  public static boolean shouldRestore(Integer current, int applied) {
    return current != null && current == applied;
  }

  public static boolean hasShortTimeout(Integer value) {
    return value != null && value > 0 && value <= 60000;
  }
}
