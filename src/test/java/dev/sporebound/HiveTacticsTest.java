package dev.sporebound;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class HiveTacticsTest {
    @Test void dangerousTargetsReceiveMoreReinforcements(){
        double weak=HiveTactics.threat(20,2),strong=HiveTactics.threat(200,20);
        assertTrue(HiveTactics.assignment(strong,4096,0)>HiveTactics.assignment(weak,4096,0));
        assertTrue(HiveTactics.assignment(weak,100,0)>HiveTactics.assignment(strong,100,30));
    }
    @Test void distanceAndStaffingReduceDemand(){
        assertTrue(HiveTactics.assignment(10,100,0)>HiveTactics.assignment(10,10000,0));
        assertTrue(HiveTactics.assignment(10,100,0)>HiveTactics.assignment(10,100,3));
    }
    @Test void consumptionRequiresBothTimeAndCrowding(){
        assertFalse(HiveTactics.canMerge(89,20));assertFalse(HiveTactics.canMerge(100,5));assertTrue(HiveTactics.canMerge(90,6));
    }
}
