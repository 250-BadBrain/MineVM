package com.example.vm.screen;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

public final class FrameMetrics {
    public record Snapshot(long frames, long pixels, long mergeNanos, long uploads, long uploadNanos, long networkBytes, long encodedFrames) {}
    private static final Map<String, FrameMetrics> METRICS = new ConcurrentHashMap<>();
    private final LongAdder frames = new LongAdder();
    private final LongAdder pixels = new LongAdder();
    private final LongAdder mergeNanos = new LongAdder();
    private final LongAdder uploads = new LongAdder();
    private final LongAdder uploadNanos = new LongAdder();
    private final LongAdder networkBytes = new LongAdder();
    private final LongAdder encodedFrames = new LongAdder();
    public static FrameMetrics forComputer(String id) { return METRICS.computeIfAbsent(id, ignored -> new FrameMetrics()); }
    public static void clear() { METRICS.clear(); }
    public void frame(long area, long nanos) { frames.increment(); pixels.add(area); mergeNanos.add(nanos); }
    public void upload(long nanos) { uploads.increment(); uploadNanos.add(nanos); }
    public void sent(long bytes) { networkBytes.add(bytes); encodedFrames.increment(); }
    public Snapshot snapshot() { return new Snapshot(frames.sum(), pixels.sum(), mergeNanos.sum(), uploads.sum(), uploadNanos.sum(), networkBytes.sum(), encodedFrames.sum()); }
}
