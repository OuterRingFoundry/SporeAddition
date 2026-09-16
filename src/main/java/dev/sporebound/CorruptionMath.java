package dev.sporebound;

/** Side independent rules. Fractions are retained; tier thresholds never round up. */
public final class CorruptionMath {
    private CorruptionMath() {}
    public static double validate(double value) {
        if (!Double.isFinite(value) || value < -2 || value > 10 || (value < 0 && value != -2 && value != -1))
            throw new IllegalArgumentException("Index must be -2, -1, or a number from 0 to 10");
        return value;
    }
    public static double advance(double index, double weightedPopulation, int elapsedTicks) {
        validate(index);
        if (index <= 0 || elapsedTicks <= 0 || weightedPopulation <= 0) return index;
        // At 100 basic infected: +0.02 per active minute; capped at +0.10/minute.
        return Math.min(10, index + Math.min(0.10, weightedPopulation * 0.0002) * elapsedTicks / 1200.0);
    }
    public static int hiveLimit(double index) { return index<5?0:index<6?1:index<7?2:index<8?3:Integer.MAX_VALUE; }
    public static double regional(double dimensionIndex,double offset) { return dimensionIndex<=0?dimensionIndex:Math.clamp(dimensionIndex+offset,0,10); }
    public static boolean allowsBoss(double index) { return index >= 5; }
    public static double healthBonus(double index) { return Math.max(0, index) * 0.15; }
    public static double damageBonus(double index) { return Math.max(0, index) * 0.10; }
    public static String state(double index) {
        if (index == -2) return "PURGED";
        if (index == -1) return "DORMANT";
        if (index == 0) return "CONTAINED";
        if (index < 5) return "INCUBATING";
        if (index < 8) return "HIVE AWAKENED";
        return "OVERRUN";
    }
}
