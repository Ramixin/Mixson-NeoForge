package net.ramixin.mixson.util;

public class ReadableTimer {

    private final long start;

    public ReadableTimer() {
        start = System.nanoTime();
    }

    public String timestamp() {
        long nanos = System.nanoTime() - start;
        if (nanos < 1_000) return nanos + " ns";
        if (nanos < 1_000_000) return String.format("%.3f µs", nanos / 1_000.0);
        if (nanos < 1_000_000_000) return String.format("%.3f ms", nanos / 1_000_000.0);
        return String.format("%.3f s", nanos / 1_000_000_000.0);
    }

}
