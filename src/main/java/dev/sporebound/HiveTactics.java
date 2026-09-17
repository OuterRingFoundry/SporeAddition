package dev.sporebound;

/** Deterministic balance rules, independent of entity ticking. */
public final class HiveTactics {
    public static final int IDLE_SECONDS = 90, IDLE_CROWD = 6, MARK_SECONDS = 30;
    private HiveTactics() {}
    public static double threat(float health, double damage) {
        return Math.max(1, health / 20.0 + Math.max(0, damage) / 4.0);
    }
    public static double assignment(double threat, double distanceSquared, int assigned) {
        return threat / ((assigned + 1.0) * (1 + Math.sqrt(Math.max(0, distanceSquared)) / 64));
    }
    public static boolean canMerge(int seconds, int neighbors) {
        return seconds >= IDLE_SECONDS && neighbors >= IDLE_CROWD;
    }
}
