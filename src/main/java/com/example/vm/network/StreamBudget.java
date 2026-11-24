package com.example.vm.network;

public final class StreamBudget {
    private long updated;
    private double available;
    private final long bytesPerSecond;
    public StreamBudget(long bytesPerSecond, long now) {
        this.bytesPerSecond = bytesPerSecond;
        updated = now;
        available = bytesPerSecond * 2.0;
    }
    public boolean consume(int bytes, long now) {
        available = Math.min(bytesPerSecond * 2.0, available + Math.max(0, now - updated) * bytesPerSecond / 1000.0);
        updated = Math.max(updated, now);
        if (bytes < 0 || available < bytes) return false;
        available -= bytes;
        return true;
    }
}
