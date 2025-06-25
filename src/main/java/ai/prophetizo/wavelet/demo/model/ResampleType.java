package ai.prophetizo.wavelet.demo.model;

public enum ResampleType {
    TIME,   // Bars based on fixed time intervals (e.g., 1 minute).
    TICK,   // Bars based on a fixed number of ticks (trades).
    VOLUME, // Bars based on a fixed amount of traded volume.
    DOLLAR  // Bars based on a fixed dollar value (Price * Volume).
}
