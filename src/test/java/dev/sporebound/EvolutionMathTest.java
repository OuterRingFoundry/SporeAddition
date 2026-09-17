package dev.sporebound;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class EvolutionMathTest {
    @Test void nativeThresholdsDefineStageAndMeter(){
        assertEquals(0,EvolutionMath.stage(0,1,7));assertEquals(1,EvolutionMath.stage(1,1,7));
        assertEquals(1,EvolutionMath.stage(6,1,7));assertEquals(2,EvolutionMath.stage(7,1,7));
        assertEquals(0,EvolutionMath.progress(1,1,7));assertEquals(0.5,EvolutionMath.progress(4,1,7));
        assertEquals(1,EvolutionMath.progress(70,1,7));
    }
    @Test void customAndInvalidConfigCannotBreakMeter(){
        int first=EvolutionMath.first(-1),hyper=EvolutionMath.hyper(first,-5);
        assertEquals(1,first);assertEquals(2,hyper);assertTrue(Double.isFinite(EvolutionMath.progress(1,first,hyper)));
        assertEquals(0,EvolutionMath.stage(3,4,20));assertEquals(1,EvolutionMath.stage(4,4,20));
        assertEquals(0.5,EvolutionMath.progress(12,4,20));
    }
    @Test void LongRunningScoresDoNotOverflowOrGrantInfiniteStats(){
        assertEquals(EvolutionMath.MAX_POINTS,EvolutionMath.add(999999,Integer.MAX_VALUE));
        assertEquals(20,EvolutionMath.bonus(999999));assertEquals(0,EvolutionMath.bonus(-2));
        assertTrue(EvolutionMath.exhaustion(4,2)>EvolutionMath.exhaustion(4,0));
        assertTrue(EvolutionMath.exhaustion(4,0)>EvolutionMath.exhaustion(1,0));
    }
}
