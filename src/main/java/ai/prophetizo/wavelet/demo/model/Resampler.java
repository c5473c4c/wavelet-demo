package ai.prophetizo.wavelet.demo.model;

import java.util.ArrayList;
import java.util.List;

/**
 * The Resampler class aggregates a list of Tick data into Bar objects
 * using different resampling strategies: TIME, TICK, VOLUME, or DOLLAR.
 */
public class Resampler {

    private final ResampleType resampleType;
    private final long threshold;

    /**
     * Constructs a Resampler with a specific configuration.
     *
     * @param resampleType The type of resampling to perform (TIME, TICK, VOLUME, DOLLAR).
     * @param threshold    The value that defines when a bar is complete.
     *                     - For TIME: The duration in milliseconds (e.g., 60000 for 1-minute bars).
     *                     - For TICK: The number of ticks per bar (e.g., 1000).
     *                     - For VOLUME: The total volume per bar.
     *                     - For DOLLAR: The total dollar value per bar.
     * @throws IllegalArgumentException if threshold is not positive.
     */
    public Resampler(ResampleType resampleType, long threshold) {
        if (threshold <= 0) {
            throw new IllegalArgumentException("Threshold must be positive.");
        }
        this.resampleType = resampleType;
        this.threshold = threshold;
    }

    /**
     * Resamples a list of ticks into a list of bars based on the configuration.
     *
     * @param ticks A chronological list of Tick objects.
     * @return A list of Bar objects.
     */
    public List<Bar> resample(List<Tick> ticks) {
        if (ticks == null || ticks.isEmpty()) {
            return new ArrayList<>();
        }

        // Dispatch to the appropriate resampling method based on type
        return switch (resampleType) {
            case TIME -> resampleByTime(ticks);
            case TICK -> resampleByTick(ticks);
            case VOLUME -> resampleByVolume(ticks);
            case DOLLAR -> resampleByDollar(ticks);
        };
    }

    /**
     * Groups ticks into bars by fixed time intervals.
     * Each bar contains all ticks whose timestamps fall within the interval.
     */
    private List<Bar> resampleByTime(List<Tick> ticks) {
        List<Bar> bars = new ArrayList<>();
        Tick firstTick = ticks.get(0);

        // Calculate the start and end time of the first bar
        long barStartTime = firstTick.timestamp() - (firstTick.timestamp() % threshold);
        long barEndTime = barStartTime + threshold;

        // Initialize the first bar with the first tick
        Bar currentBar = new Bar(barStartTime, firstTick.price(), firstTick.price(), firstTick.price(), firstTick.price(), firstTick.volume());

        for (int i = 1; i < ticks.size(); i++) {
            Tick currentTick = ticks.get(i);

            if (currentTick.timestamp() < barEndTime) {
                // Tick belongs to the current bar, update high, low, close, and volume
                currentBar = new Bar(
                        currentBar.openTimestamp(),
                        currentBar.open(),
                        Math.max(currentBar.high(), currentTick.price()),
                        Math.min(currentBar.low(), currentTick.price()),
                        currentTick.price(),
                        currentBar.totalVolume() + currentTick.volume()
                );
            } else {
                // Finalize the current bar and start a new one
                bars.add(currentBar);

                // Set new bar's start and end time
                barStartTime = currentTick.timestamp() - (currentTick.timestamp() % threshold);
                barEndTime = barStartTime + threshold;
                currentBar = new Bar(barStartTime, currentTick.price(), currentTick.price(), currentTick.price(), currentTick.price(), currentTick.volume());
            }
        }
        // Add the last bar
        bars.add(currentBar);
        return bars;
    }

    /**
     * Groups ticks into bars by a fixed number of ticks.
     * Each bar contains up to 'threshold' number of ticks.
     */
    private List<Bar> resampleByTick(List<Tick> ticks) {
        List<Bar> bars = new ArrayList<>();
        int tickCounter = 0;
        Bar currentBar = null;

        for (Tick tick : ticks) {
            if (currentBar == null) {
                // Start a new bar with the current tick
                currentBar = new Bar(tick.timestamp(), tick.price(), tick.price(), tick.price(), tick.price(), tick.volume());
                tickCounter = 1;
                continue;
            }

            // Update the current bar with the new tick
            currentBar = new Bar(
                    currentBar.openTimestamp(),
                    currentBar.open(),
                    Math.max(currentBar.high(), tick.price()),
                    Math.min(currentBar.low(), tick.price()),
                    tick.price(),
                    currentBar.totalVolume() + tick.volume()
            );
            tickCounter++;

            // If threshold is met, finalize bar and reset
            if (tickCounter >= threshold) {
                bars.add(currentBar);
                currentBar = null;
            }
        }

        // Add the last incomplete bar if it exists
        if (currentBar != null) {
            bars.add(currentBar);
        }
        return bars;
    }

    /**
     * Groups ticks into bars by accumulated volume.
     * Each bar contains ticks until the total volume reaches or exceeds the threshold.
     */
    private List<Bar> resampleByVolume(List<Tick> ticks) {
        List<Bar> bars = new ArrayList<>();
        Bar currentBar = null;

        for (Tick tick : ticks) {
            if (currentBar == null) {
                // Start a new bar with the current tick, volume starts at 0
                currentBar = new Bar(tick.timestamp(), tick.price(), tick.price(), tick.price(), tick.price(), 0L);
            }

            // Update the bar with the current tick's data
            currentBar = new Bar(
                    currentBar.openTimestamp(),
                    currentBar.open(),
                    Math.max(currentBar.high(), tick.price()),
                    Math.min(currentBar.low(), tick.price()),
                    tick.price(),
                    currentBar.totalVolume() + tick.volume()
            );

            // If accumulated volume meets or exceeds threshold, finalize bar
            if (currentBar.totalVolume() >= threshold) {
                bars.add(currentBar);
                currentBar = null;
            }
        }

        // Add the last incomplete bar if it exists
        if (currentBar != null) {
            bars.add(currentBar);
        }
        return bars;
    }

    /**
     * Groups ticks into bars by accumulated dollar value (price * volume).
     * Each bar contains ticks until the total dollar value reaches or exceeds the threshold.
     */
    private List<Bar> resampleByDollar(List<Tick> ticks) {
        List<Bar> bars = new ArrayList<>();
        Bar currentBar = null;
        double currentDollarValue = 0.0;

        for (Tick tick : ticks) {
            if (currentBar == null) {
                // Start a new bar with the current tick, volume starts at 0
                currentBar = new Bar(tick.timestamp(), tick.price(), tick.price(), tick.price(), tick.price(), 0L);
                currentDollarValue = 0.0;
            }

            // Calculate the dollar value for this tick and accumulate
            double tickDollarValue = tick.price() * tick.volume();
            currentDollarValue += tickDollarValue;

            // Update the bar with the current tick's data
            currentBar = new Bar(
                    currentBar.openTimestamp(),
                    currentBar.open(),
                    Math.max(currentBar.high(), tick.price()),
                    Math.min(currentBar.low(), tick.price()),
                    tick.price(),
                    currentBar.totalVolume() + tick.volume()
            );

            // If accumulated dollar value meets or exceeds threshold, finalize bar
            if (currentDollarValue >= threshold) {
                bars.add(currentBar);
                currentBar = null;
            }
        }

        // Add the last incomplete bar if it exists
        if (currentBar != null) {
            bars.add(currentBar);
        }
        return bars;
    }
}