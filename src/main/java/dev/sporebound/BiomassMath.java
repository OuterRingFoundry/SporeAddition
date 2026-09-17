package dev.sporebound;

/** Balance constants shared by conversion, server dimensions and the renderer. */
public final class BiomassMath {
    public static final int MAX_SIZE_MASS = 8, HEALTH_PER_MASS = 20, IDLE_TICKS = 600;
    private BiomassMath() {}
    public static int fromHealth(float maximumHealth) {
        if (!Float.isFinite(maximumHealth) || maximumHealth <= 0) return 1;
        return Math.max(1, (int)Math.ceil(maximumHealth / HEALTH_PER_MASS));
    }
    public static float scale(int mass) { return (float)Math.cbrt(Math.clamp(mass, 1, MAX_SIZE_MASS)); }
}
