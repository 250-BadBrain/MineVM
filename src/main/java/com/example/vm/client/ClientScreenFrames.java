package com.example.vm.client;

import com.example.vm.MineVM;
import com.example.vm.concurrent.ComputerExecutors;
import com.example.vm.network.ModNetwork;
import com.example.vm.network.ScreenFramePacket;
import com.example.vm.network.WatchScreenPacket;
import com.example.vm.screen.FrameCodecs;
import com.example.vm.screen.ScreenFrame;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

public final class ClientScreenFrames {
    private record Received(long epoch, long sequence, ScreenFrame frame) {}
    private static final Map<String, Received> FRAMES = new HashMap<>();
    private static final Map<String, Long> WATCHED = new HashMap<>();
    private static ExecutorService decoder = ComputerExecutors.singleThread("minevm-frame-decoder", 8);
    private static long generation;

    public static Optional<ScreenFrame> snapshot(String computerId, BlockPos screen) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.hasSingleplayerServer()) return MineVM.SCREEN_SERVICE.latestFrameSnapshot(computerId);
        if (minecraft.level == null || minecraft.player == null) return Optional.empty();
        long now = System.currentTimeMillis();
        if (WATCHED.size() > 64) {
            WATCHED.entrySet().removeIf(entry -> now - entry.getValue() > 10000);
            FRAMES.keySet().retainAll(WATCHED.keySet());
        }
        if (now - WATCHED.getOrDefault(computerId, 0L) >= 1000) {
            WATCHED.put(computerId, now);
            ModNetwork.CHANNEL.sendToServer(new WatchScreenPacket(screen));
        }
        Received frame = FRAMES.get(computerId);
        return frame == null ? Optional.empty() : Optional.of(frame.frame);
    }

    public static void receive(ScreenFramePacket packet) {
        long currentGeneration = generation;
        if (packet.computerId().isBlank() || packet.computerId().length() > 128 || !WATCHED.containsKey(packet.computerId())) return;
        if (decoder.isShutdown()) decoder = ComputerExecutors.singleThread("minevm-frame-decoder", 8);
        try {
            decoder.execute(() -> {
                try {
                    long started = System.nanoTime();
                    ScreenFrame decoded = FrameCodecs.get(packet.codec()).decode(packet.width(), packet.height(), packet.payload());
                    com.example.vm.screen.FrameMetrics.forComputer(packet.computerId()).frame((long) decoded.width() * decoded.height(), System.nanoTime() - started);
                    Minecraft.getInstance().execute(() -> {
                        if (currentGeneration != generation || Minecraft.getInstance().level == null) return;
                        Received old = FRAMES.get(packet.computerId());
                        if (old == null || packet.epoch() > old.epoch || (packet.epoch() == old.epoch && packet.sequence() > old.sequence))
                            FRAMES.put(packet.computerId(), new Received(packet.epoch(), packet.sequence(), decoded));
                    });
                } catch (java.io.IOException failure) { MineVM.LOGGER.warn("Rejected invalid network screen frame: {}", failure.getMessage()); }
            });
        } catch (RejectedExecutionException busy) { /* Full frames allow the next packet to recover without a delta dependency. */ }
    }

    public static void clear() {
        generation++;
        decoder.shutdownNow();
        FRAMES.clear(); WATCHED.clear();
    }
    private ClientScreenFrames() {}
}
