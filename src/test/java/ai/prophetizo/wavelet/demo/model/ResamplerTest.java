package ai.prophetizo.wavelet.demo.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ResamplerTest {
    private List<Tick> sampleTicks;

    /**
     * Sets up a standard list of ticks to be used in multiple tests.
     * This method runs before each test.
     */
    @BeforeEach
    void setUp() {
        sampleTicks = List.of(
                // --- Bar 1 ---
                new Tick(1000L, 100.0, 10),
                new Tick(2000L, 101.5, 5),
                new Tick(8000L, 99.5, 20),
                // --- Bar 2 ---
                new Tick(10000L, 102.0, 15),
                new Tick(11000L, 102.5, 8),
                // --- Bar 3 ---
                new Tick(14000L, 101.0, 30),
                new Tick(19000L, 103.0, 10),
                // --- Bar 4 (incomplete) ---
                new Tick(22000L, 102.8, 40)
        );
    }

    @Test
    @DisplayName("Constructor should throw IllegalArgumentException for non-positive threshold")
    void constructorShouldThrowExceptionForZeroThreshold() {
        assertThrows(IllegalArgumentException.class, () -> new Resampler(ResampleType.TIME, 0));
        assertThrows(IllegalArgumentException.class, () -> new Resampler(ResampleType.TICK, -1));
    }

    @Test
    @DisplayName("Resampling a null list should return an empty list")
    void resampleNullList() {
        Resampler resampler = new Resampler(ResampleType.TICK, 100);
        List<Bar> bars = resampler.resample(null);
        assertNotNull(bars);
        assertTrue(bars.isEmpty());
    }

    @Test
    @DisplayName("Resampling an empty list should return an empty list")
    void resampleEmptyList() {
        Resampler resampler = new Resampler(ResampleType.TICK, 100);
        List<Bar> bars = resampler.resample(new ArrayList<>());
        assertNotNull(bars);
        assertTrue(bars.isEmpty());
    }

    @Nested
    @DisplayName("Time Resampling Tests")
    class TimeResamplingTests {

        @Test
        @DisplayName("Should correctly resample by time interval")
        void resampleByTime() {
            // Using a 10-second (10000ms) threshold
            Resampler resampler = new Resampler(ResampleType.TIME, 10000);
            List<Bar> bars = resampler.resample(sampleTicks);

            assertEquals(3, bars.size());

            // Check Bar 1 (0ms to 9999ms)
            Bar bar1 = bars.getFirst();
            assertEquals(0L, bar1.openTimestamp());
            assertEquals(100.0, bar1.open());
            assertEquals(101.5, bar1.high());
            assertEquals(99.5, bar1.low());
            assertEquals(99.5, bar1.close());
            assertEquals(35, bar1.totalVolume());

            // Check Bar 2 (10000ms to 19999ms)
            Bar bar2 = bars.get(1);
            assertEquals(10000L, bar2.openTimestamp());
            assertEquals(102.0, bar2.open());
            assertEquals(103.0, bar2.high());
            assertEquals(101.0, bar2.low());
            assertEquals(103.0, bar2.close());
            assertEquals(63, bar2.totalVolume());

            // Check Bar 3 (20000ms to 29999ms)
            Bar bar3 = bars.get(2);
            assertEquals(20000L, bar3.openTimestamp());
            assertEquals(102.8, bar3.open());
            assertEquals(102.8, bar3.high());
            assertEquals(102.8, bar3.low());
            assertEquals(102.8, bar3.close());
            assertEquals(40, bar3.totalVolume());
        }
    }

    @Nested
    @DisplayName("Tick Resampling Tests")
    class TickResamplingTests {

        @Test
        @DisplayName("Should correctly resample by number of ticks")
        void resampleByTick() {
            // Using a 3-tick threshold
            Resampler resampler = new Resampler(ResampleType.TICK, 3);
            List<Bar> bars = resampler.resample(sampleTicks);

            // 8 ticks / 3 per bar = 2 full bars, 1 incomplete bar
            assertEquals(3, bars.size());

            // Check Bar 1
            Bar bar1 = bars.getFirst();
            assertEquals(1000L, bar1.openTimestamp());
            assertEquals(100.0, bar1.open());
            assertEquals(101.5, bar1.high());
            assertEquals(99.5, bar1.low());
            assertEquals(99.5, bar1.close());
            assertEquals(35, bar1.totalVolume());

            // Check Bar 2
            Bar bar2 = bars.get(1);
            assertEquals(10000L, bar2.openTimestamp());
            assertEquals(102.0, bar2.open());
            assertEquals(102.5, bar2.high());
            assertEquals(101.0, bar2.low());
            assertEquals(101.0, bar2.close());
            assertEquals(53, bar2.totalVolume());

            // Check Bar 3 (incomplete)
            Bar bar3 = bars.get(2);
            assertEquals(19000L, bar3.openTimestamp());
            assertEquals(103.0, bar3.open());
            assertEquals(103.0, bar3.high());
            assertEquals(102.8, bar3.low());
            assertEquals(102.8, bar3.close());
            assertEquals(50, bar3.totalVolume());
        }
    }

    @Nested
    @DisplayName("Volume Resampling Tests (Corrected Logic)")
    class VolumeResamplingTests {
        @Test
        @DisplayName("Should correctly resample by volume including the threshold-crossing tick")
        void resampleByVolume() {
            Resampler resampler = new Resampler(ResampleType.VOLUME, 50);
            List<Bar> bars = resampler.resample(sampleTicks);

            assertEquals(2, bars.size());

            // Check Bar 1: Includes ticks 1-4, which sums to exactly 50 volume
            Bar bar1 = bars.getFirst();
            assertEquals(1000L, bar1.openTimestamp());
            assertEquals(100.0, bar1.open());
            assertEquals(102.0, bar1.high());
            assertEquals(99.5, bar1.low());
            assertEquals(102.0, bar1.close());
            assertEquals(50, bar1.totalVolume());

            // Check Bar 2: Incomplete bar with the remaining ticks 5-8 (8+30+10+40 = 88 volume)
            // This bar also crosses the threshold, so it's a complete bar.
            Bar bar2 = bars.get(1);
            assertEquals(11000L, bar2.openTimestamp());
            assertEquals(102.5, bar2.open());
            assertEquals(103.0, bar2.high());
            assertEquals(101.0, bar2.low());
            assertEquals(102.8, bar2.close());
            assertEquals(88, bar2.totalVolume());
        }
    }

    @Nested
    @DisplayName("Dollar Resampling Tests (Corrected Logic)")
    class DollarResamplingTests {
        @Test
        @DisplayName("Should correctly resample by dollar value including the threshold-crossing tick")
        void resampleByDollar() {
            Resampler resampler = new Resampler(ResampleType.DOLLAR, 5000);
            List<Bar> bars = resampler.resample(sampleTicks);

            assertEquals(2, bars.size());

            // Check Bar 1: Includes ticks 1-4. Total value = 1000+507.5+1990+1530 = 5027.5
            Bar bar1 = bars.getFirst();
            assertEquals(1000L, bar1.openTimestamp());
            assertEquals(100.0, bar1.open());
            assertEquals(102.0, bar1.high());
            assertEquals(99.5, bar1.low());
            assertEquals(102.0, bar1.close());
            assertEquals(50, bar1.totalVolume());

            // Check Bar 2: Incomplete bar with remaining ticks 5-8. Total value = 820+3030+1030+4112 = 8992
            Bar bar2 = bars.get(1);
            assertEquals(11000L, bar2.openTimestamp());
            assertEquals(102.5, bar2.open());
            assertEquals(103.0, bar2.high());
            assertEquals(101.0, bar2.low());
            assertEquals(102.8, bar2.close());
            assertEquals(88, bar2.totalVolume());
        }
    }
}
