package ai.prophetizo.wavelet.demo;

import ai.prophetizo.wavelet.demo.model.Bar;
import ai.prophetizo.wavelet.demo.model.ResampleType;
import ai.prophetizo.wavelet.demo.model.Resampler;
import ai.prophetizo.wavelet.demo.model.Tick;

import java.util.List;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        // Sample tick data. Timestamps are simplified for readability.
        List<Tick> sampleTicks = List.of(
                new Tick(1000, 100.0, 10), // Bar 1 starts
                new Tick(2000, 101.5, 5),
                new Tick(8000, 100.5, 20),
                new Tick(10000, 102.0, 15), // Time Bar 1 ends here (threshold 10s)
                new Tick(11000, 102.5, 8),  // Tick/Vol/Dollar Bar 1 ends here
                new Tick(14000, 101.0, 30),
                new Tick(19000, 103.0, 10),
                new Tick(22000, 102.8, 40)
        );

        System.out.println("--- Resampling by TIME (10 seconds) ---");
        Resampler timeResampler = new Resampler(ResampleType.TIME, 10000); // 10 second bars
        List<Bar> timeBars = timeResampler.resample(sampleTicks);
        timeBars.forEach(System.out::println);

        System.out.println("\n--- Resampling by TICK (5 ticks) ---");
        Resampler tickResampler = new Resampler(ResampleType.TICK, 5);
        List<Bar> tickBars = tickResampler.resample(sampleTicks);
        tickBars.forEach(System.out::println);

        System.out.println("\n--- Resampling by VOLUME (50 contracts) ---");
        Resampler volumeResampler = new Resampler(ResampleType.VOLUME, 50);
        List<Bar> volumeBars = volumeResampler.resample(sampleTicks);
        volumeBars.forEach(System.out::println);

        System.out.println("\n--- Resampling by DOLLAR VALUE ($5000) ---");
        Resampler dollarResampler = new Resampler(ResampleType.DOLLAR, 5000);
        List<Bar> dollarBars = dollarResampler.resample(sampleTicks);
        dollarBars.forEach(System.out::println);
    }
}