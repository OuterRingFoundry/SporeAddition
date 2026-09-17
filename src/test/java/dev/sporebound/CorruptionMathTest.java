package dev.sporebound;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CorruptionMathTest {
    @Test void zeroIsNormal(){assertEquals("NORMAL",CorruptionMath.state(0));}
    @Test void lockedStatesNeverGrow() {
        for (double n : new double[]{-2,-1,0}) for (int weight : new int[]{0,1,100,1000000})
            assertEquals(n, CorruptionMath.advance(n, weight, 1200));
    }
    @Test void boundariesAndFractionalBossThreshold() {
        assertFalse(CorruptionMath.allowsBoss(4.99999)); assertTrue(CorruptionMath.allowsBoss(5));
        assertEquals(10, CorruptionMath.advance(9.9999, 10000,1200));
        assertEquals(6.02, CorruptionMath.advance(6,100,1200),1e-10);
        assertEquals(6.1, CorruptionMath.advance(6,1000000,1200),1e-10);
        assertEquals(6, CorruptionMath.advance(6,0,1200));
    }
    @Test void rejectInvalidAndNonFiniteSaves() {
        for (double n : new double[]{-3,-1.5,-0.5,10.1,Double.NaN,Double.POSITIVE_INFINITY})
            assertThrows(IllegalArgumentException.class, () -> CorruptionMath.validate(n));
    }
    @Test void hiveCapsAndRegionalOverrides() {
        assertEquals(0,CorruptionMath.hiveLimit(4.999));assertEquals(1,CorruptionMath.hiveLimit(5.999));
        assertEquals(2,CorruptionMath.hiveLimit(6));assertEquals(2,CorruptionMath.hiveLimit(6.999));
        assertEquals(3,CorruptionMath.hiveLimit(7.999));assertEquals(Integer.MAX_VALUE,CorruptionMath.hiveLimit(8));
        for(double locked:new double[]{-2,-1,0})for(double offset:new double[]{-4,0,1,2})assertEquals(locked,CorruptionMath.regional(locked,offset));
        assertEquals(2,CorruptionMath.regional(6,-4));assertEquals(8,CorruptionMath.regional(6,2));
        assertEquals(0,CorruptionMath.regional(1,-4));assertEquals(10,CorruptionMath.regional(9,2));
    }
    @Test void fogRespectsLockedStatesAndRegionalRefuges() {
        for(double index:new double[]{-2,-1,0,1,2}) {
            assertEquals(0,CorruptionMath.fogDensity(index));
            assertFalse(CorruptionMath.hazardousFog(index));
        }
        assertEquals(0,CorruptionMath.fogDensity(CorruptionMath.regional(6,-4)));
        assertEquals(0.5,CorruptionMath.fogDensity(6));
        assertEquals(1,CorruptionMath.fogDensity(10));
        assertFalse(CorruptionMath.hazardousFog(7.999));
        assertTrue(CorruptionMath.hazardousFog(CorruptionMath.regional(6,2)));
        assertFalse(CorruptionMath.hazardousFog(CorruptionMath.regional(10,-4)));
    }
    @Test void scalingStaysBounded() {
        assertEquals(0,CorruptionMath.healthBonus(-2)); assertEquals(1.5,CorruptionMath.healthBonus(10));
        assertEquals(1,CorruptionMath.damageBonus(10));
    }
}
