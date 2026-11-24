package com.example.vm.network;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ControlLeases {
    private record Lease(UUID player, long expires) {}
    private final Map<String, Lease> leases = new HashMap<>();
    private final long timeoutMillis;
    public ControlLeases(long timeoutMillis) {
        if (timeoutMillis < 1) throw new IllegalArgumentException("Lease duration must be positive");
        this.timeoutMillis = timeoutMillis;
    }
    public synchronized boolean acquire(String computer, UUID player, long now) {
        Lease current = leases.get(computer);
        if (current != null && current.expires > now && !current.player.equals(player)) return false;
        leases.put(computer, new Lease(player, now + timeoutMillis));
        return true;
    }
    public synchronized boolean owns(String computer, UUID player, long now) {
        Lease current = leases.get(computer);
        return current != null && current.expires > now && current.player.equals(player);
    }
    public synchronized void release(String computer, UUID player) {
        Lease current = leases.get(computer);
        if (current != null && current.player.equals(player)) leases.remove(computer);
    }
    public synchronized void clear() { leases.clear(); }
}
