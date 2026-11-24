package com.example.vm.network;

import com.example.vm.MineVM;
import com.example.vm.blockentity.ComputerBlockEntity;
import com.example.vm.blockentity.ComputerMonitorBlockEntity;
import com.example.vm.computer.ComputerInputAuthorization;
import com.example.vm.concurrent.ComputerExecutors;
import com.example.vm.config.ComputerRuntimeConfig;
import com.example.vm.input.MouseButtonInput;
import com.example.vm.input.KeyboardInput;
import com.example.vm.input.VirtualMachineInput;
import com.example.vm.registry.ModBlocks;
import com.example.vm.screen.FrameCodecs;
import com.example.vm.screen.FrameMetrics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.PacketDistributor;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicLong;

/** All subscriptions and leases are accessed exclusively on the server thread. */
public final class MultiplayerScreens {
    private record Control(ServerLevel level, BlockPos screen, String computerId, Set<String> heldKeys) {}
    private static final class Watch {
        final ServerLevel level;
        final BlockPos screen;
        long seen;
        long lastRevision = -1;
        Watch(ServerLevel level, BlockPos screen, long now) { this.level = level; this.screen = screen; seen = now; }
    }
    private static final ControlLeases LEASES = new ControlLeases(6000);
    private static final Map<UUID, Control> CONTROLS = new HashMap<>();
    private static final Map<UUID, Map<String, Watch>> WATCHERS = new HashMap<>();
    private static final Map<UUID, StreamBudget> BUDGETS = new HashMap<>();
    private static final Set<String> IN_FLIGHT = new HashSet<>();
    private static final AtomicLong SEQUENCE = new AtomicLong();
    private static long epoch;
    private static ExecutorService encoder;

    public static void start() {
        close();
        epoch = System.currentTimeMillis();
        encoder = ComputerExecutors.fixedThreads("minevm-frame-encoder", 2, 16);
    }

    public static boolean acquire(ServerPlayer player, BlockPos screen, String computerId) {
        ComputerBlockEntity computer = findComputer(player, screen);
        if (computer == null || !computer.getComputerId().equals(computerId) || !computer.canControl(player)
                || !ComputerInputAuthorization.allows(player, screen, computerId)) {
            player.sendSystemMessage(Component.translatable("message.minevm.not_owner"));
            return false;
        }
        long now = System.currentTimeMillis();
        if (!LEASES.acquire(computerId, player.getUUID(), now)) {
            player.sendSystemMessage(Component.translatable("message.minevm.control_busy"));
            return false;
        }
        for (var entry : new ArrayList<>(CONTROLS.entrySet())) {
            if (!entry.getKey().equals(player.getUUID()) && entry.getValue().computerId.equals(computerId)) {
                ServerPlayer previous = player.server.getPlayerList().getPlayer(entry.getKey());
                if (previous != null) release(previous, computerId);
                else CONTROLS.remove(entry.getKey());
            }
        }
        Control old = CONTROLS.get(player.getUUID());
        if (old != null && !old.computerId.equals(computerId)) release(player, old.computerId);
        computer.claimOwner(player.getUUID());
        CONTROLS.put(player.getUUID(), new Control(player.serverLevel(), screen.immutable(), computerId, old != null && old.computerId.equals(computerId) ? old.heldKeys : new HashSet<>()));
        return true;
    }

    public static boolean allowsInput(ServerPlayer player, BlockPos screen, String computerId) {
        Control control = CONTROLS.get(player.getUUID());
        return control != null && control.level == player.serverLevel() && control.screen.equals(screen)
                && control.computerId.equals(computerId) && LEASES.owns(computerId, player.getUUID(), System.currentTimeMillis())
                && ComputerInputAuthorization.allows(player, screen, computerId);
    }

    public static void renew(ServerPlayer player, String computerId) {
        Control control = CONTROLS.get(player.getUUID());
        if (control != null && control.computerId.equals(computerId)
                && allowsInput(player, control.screen, computerId)) LEASES.acquire(computerId, player.getUUID(), System.currentTimeMillis());
        else release(player, computerId);
    }

    public static boolean trackInput(ServerPlayer player, VirtualMachineInput input) {
        Control control = CONTROLS.get(player.getUUID());
        if (control == null || !control.computerId.equals(input.computerId())) return false;
        if (input instanceof KeyboardInput key) {
            if (key.pressed()) {
                if (control.heldKeys.size() >= 128 && !control.heldKeys.contains(key.key())) return false;
                control.heldKeys.add(key.key());
            } else control.heldKeys.remove(key.key());
        }
        return true;
    }

    public static void release(ServerPlayer player, String computerId) {
        Control control = CONTROLS.get(player.getUUID());
        if (control == null || !control.computerId.equals(computerId)) return;
        CONTROLS.remove(player.getUUID());
        LEASES.release(computerId, player.getUUID());
        for (String key : control.heldKeys) {
            KeyboardInput input = new KeyboardInput(computerId, key, false);
            if (!MineVM.SCREEN_SERVICE.sendVncInput(computerId, input)) MineVM.INPUT_SERVICE.send(input);
        }
        for (int button = 0; button < 3; button++) {
            MouseButtonInput input = new MouseButtonInput(computerId, button, false);
            if (!MineVM.SCREEN_SERVICE.sendVncInput(computerId, input)) MineVM.INPUT_SERVICE.send(input);
        }
        ModNetwork.stopScreenControl(player, computerId);
    }

    public static void disconnected(ServerPlayer player) {
        Control control = CONTROLS.get(player.getUUID());
        if (control != null) release(player, control.computerId);
        WATCHERS.remove(player.getUUID()); BUDGETS.remove(player.getUUID());
    }

    public static void watch(ServerPlayer player, BlockPos screen) {
        ComputerBlockEntity computer = findComputer(player, screen);
        if (computer == null || !computer.vmDisplayActive()
                || (!ComputerRuntimeConfig.sharedViewing() && !computer.canControl(player))) return;
        Map<String, Watch> subscriptions = WATCHERS.computeIfAbsent(player.getUUID(), ignored -> new HashMap<>());
        String id = computer.getComputerId();
        Watch existing = subscriptions.get(id);
        if (existing != null && existing.screen.equals(screen) && existing.level == player.serverLevel()) existing.seen = System.currentTimeMillis();
        else if (subscriptions.size() < 4) subscriptions.put(id, new Watch(player.serverLevel(), screen.immutable(), System.currentTimeMillis()));
    }

    public static ComputerBlockEntity findComputer(ServerPlayer player, BlockPos screen) {
        if (player.distanceToSqr(screen.getX() + .5, screen.getY() + .5, screen.getZ() + .5) > 256
                || !player.level().hasChunkAt(screen)) return null;
        BlockEntity entity = player.level().getBlockEntity(screen);
        if (entity instanceof ComputerBlockEntity computer && player.level().getBlockState(screen).is(ModBlocks.LAPTOP_COMPUTER.get()))
            return computer.poweredOn() ? computer : null;
        if (entity instanceof ComputerMonitorBlockEntity monitor && monitor.displayPowered() && monitor.linkedComputerPos().isPresent()
                && player.level().hasChunkAt(monitor.linkedComputerPos().get())
                && player.level().getBlockEntity(monitor.linkedComputerPos().get()) instanceof ComputerBlockEntity computer
                && computer.poweredOn() && monitor.linkedComputerId().filter(computer.getComputerId()::equals).isPresent()) return computer;
        return null;
    }

    public static void tick(MinecraftServer server) {
        long now = System.currentTimeMillis();
        for (var entry : new ArrayList<>(CONTROLS.entrySet())) {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player != null && !allowsInput(player, entry.getValue().screen, entry.getValue().computerId)) release(player, entry.getValue().computerId);
        }
        int interval = (20 + ComputerRuntimeConfig.networkFramesPerSecond() - 1) / ComputerRuntimeConfig.networkFramesPerSecond();
        if (encoder == null || server.getTickCount() % interval != 0) return;
        Map<String, List<ServerPlayer>> viewers = new HashMap<>();
        for (var iterator = WATCHERS.entrySet().iterator(); iterator.hasNext();) {
            var entry = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) { iterator.remove(); BUDGETS.remove(entry.getKey()); continue; }
            entry.getValue().entrySet().removeIf(subscription -> {
                Watch watch = subscription.getValue();
                ComputerBlockEntity computer = watch.level == player.serverLevel() ? findComputer(player, watch.screen) : null;
                return now - watch.seen > 3000 || computer == null || !computer.vmDisplayActive()
                        || !computer.getComputerId().equals(subscription.getKey())
                        || (!ComputerRuntimeConfig.sharedViewing() && !computer.canControl(player));
            });
            for (var subscription : entry.getValue().entrySet()) {
                long revision = MineVM.SCREEN_SERVICE.frameRevision(subscription.getKey());
                if (revision != 0 && revision != subscription.getValue().lastRevision)
                    viewers.computeIfAbsent(subscription.getKey(), ignored -> new ArrayList<>()).add(player);
            }
            if (entry.getValue().isEmpty()) iterator.remove();
        }
        for (var entry : viewers.entrySet()) {
            String id = entry.getKey();
            if (!IN_FLIGHT.add(id)) continue;
            long currentEpoch = epoch;
            long revision = MineVM.SCREEN_SERVICE.frameRevision(id);
            String codecId = ComputerRuntimeConfig.networkFrameCodec();
            try {
                encoder.execute(() -> {
                    ScreenFramePacket packet = null;
                    try {
                        var full = MineVM.SCREEN_SERVICE.fullFrameSnapshot(id);
                        if (full.isPresent()) {
                            var frame = FrameCodecs.scale(full.get());
                            var codec = FrameCodecs.get(codecId);
                            packet = new ScreenFramePacket(id, currentEpoch, SEQUENCE.incrementAndGet(), codec.id(), frame.width(), frame.height(), codec.encode(frame));
                        }
                    } catch (Exception failure) { MineVM.LOGGER.warn("Screen encode failed for {}", id, failure); }
                    ScreenFramePacket encoded = packet;
                    if (!server.isRunning()) return;
                    server.execute(() -> {
                        if (currentEpoch != epoch) return;
                        IN_FLIGHT.remove(id);
                        if (encoded == null) return;
                        for (ServerPlayer player : entry.getValue()) {
                            Watch watch = WATCHERS.getOrDefault(player.getUUID(), Map.of()).get(id);
                            if (watch == null || player.serverLevel() != watch.level) continue;
                            ComputerBlockEntity computer = findComputer(player, watch.screen);
                            if (computer == null || !computer.getComputerId().equals(id) || !computer.vmDisplayActive()
                                    || (!ComputerRuntimeConfig.sharedViewing() && !computer.canControl(player))) continue;
                            StreamBudget budget = BUDGETS.computeIfAbsent(player.getUUID(), ignored -> new StreamBudget(ComputerRuntimeConfig.networkBytesPerSecond(), System.currentTimeMillis()));
                            if (!budget.consume(encoded.payload().length, System.currentTimeMillis())) continue;
                            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), encoded);
                            watch.lastRevision = revision;
                            FrameMetrics.forComputer(id).sent(encoded.payload().length);
                        }
                    });
                });
            } catch (RejectedExecutionException busy) { IN_FLIGHT.remove(id); }
        }
    }

    public static void close() {
        epoch++;
        if (encoder != null) encoder.shutdownNow();
        encoder = null;
        CONTROLS.clear(); WATCHERS.clear(); BUDGETS.clear(); IN_FLIGHT.clear(); LEASES.clear();
    }
    private MultiplayerScreens() {}
}
