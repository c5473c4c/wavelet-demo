package ai.prophetizo.wavelet.demo.model;

/**
 * A record to represent a single OHLCV bar.
 *
 * @param openTimestamp The starting timestamp of the bar.
 * @param open          The opening price of the bar.
 * @param high          The highest price during the bar's duration.
 * @param low           The lowest price during the bar's duration.
 * @param close         The closing price of the bar.
 * @param totalVolume   The total traded volume during the bar's duration.
 */
public record Bar(long openTimestamp, double open, double high, double low, double close, long totalVolume) {
}
