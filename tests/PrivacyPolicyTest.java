package org.stillhome.launcher;

public final class PrivacyPolicyTest {
  private static void require(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
  }

  public static void main(String[] args) {
    // Preserve an owner's stricter timeout, including uncommon supported values.
    for (int timeout : new int[] {1000, 15000, 30000, 45000, 60000})
      require(
          PrivacyPolicy.recommendedTimeout(timeout) == timeout,
          "Must not lengthen a short timeout");
    for (int timeout : new int[] {0, -1, 120000, 1800000, Integer.MAX_VALUE})
      require(
          PrivacyPolicy.recommendedTimeout(timeout) == 60000,
          "Long or disabled timeout must become one minute");
    require(!PrivacyPolicy.hasShortTimeout(null), "Unavailable is not a verified setting");
    require(!PrivacyPolicy.hasShortTimeout(0), "Disabled timeout is not a short timeout");
    require(PrivacyPolicy.shouldRestore(60000, 60000), "Unchanged applied value can be restored");
    require(
        !PrivacyPolicy.shouldRestore(30000, 60000), "Undo must preserve a newer shorter timeout");
    require(
        !PrivacyPolicy.shouldRestore(120000, 60000), "Undo must preserve any newer owner choice");
    require(
        !PrivacyPolicy.shouldRestore(null, 0), "Unavailable value must not be overwritten by undo");
    System.out.println("Privacy policy checks passed.");
  }
}
