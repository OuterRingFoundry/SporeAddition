package dev.sporebound;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BiomassMathTest {
    @Test void healthControlsYieldIncludingLargeCreatures() {
        assertEquals(1, BiomassMath.fromHealth(10));
        assertEquals(1, BiomassMath.fromHealth(20));
        assertEquals(2, BiomassMath.fromHealth(21));
        assertEquals(25, BiomassMath.fromHealth(500));
        assertEquals(1, BiomassMath.fromHealth(Float.NaN));
    }
    @Test void volumeTracksMassAndSizeRemainsBoundedDuringEvolution() {
        assertEquals(1, BiomassMath.scale(1), 0.001);
        assertEquals(2, BiomassMath.scale(8), 0.001);
        assertEquals(2, BiomassMath.scale(16), 0.001);
        assertTrue(BiomassMath.scale(5) > BiomassMath.scale(2));
    }
}
