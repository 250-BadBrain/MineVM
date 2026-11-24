package com.example.vm.network;

import com.example.vm.MineVM;
import com.example.vm.computer.ComputerBiosAction;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.NetworkDirection;
import java.util.Optional;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String PROTOCOL_VERSION = "3";
    private static int nextPacketId;

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MineVM.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private ModNetwork() {
    }

    public static void register() {
        CHANNEL.registerMessage(nextPacketId++, WatchScreenPacket.class, WatchScreenPacket::encode, WatchScreenPacket::decode, WatchScreenPacket::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(nextPacketId++, ScreenFramePacket.class, ScreenFramePacket::encode, ScreenFramePacket::decode, ScreenFramePacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(nextPacketId++, ControlSessionPacket.class, ControlSessionPacket::encode, ControlSessionPacket::decode, ControlSessionPacket::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(
                nextPacketId++,
                ComputerBiosActionPacket.class,
                ComputerBiosActionPacket::encode,
                ComputerBiosActionPacket::decode,
                ComputerBiosActionPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                StartScreenControlPacket.class,
                StartScreenControlPacket::encode,
                StartScreenControlPacket::decode,
                StartScreenControlPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                ComputerInputPacket.class,
                ComputerInputPacket::encode,
                ComputerInputPacket::decode,
                ComputerInputPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                StopScreenControlPacket.class,
                StopScreenControlPacket::encode,
                StopScreenControlPacket::decode,
                StopScreenControlPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                UpdateScreenControlModePacket.class,
                UpdateScreenControlModePacket::encode,
                UpdateScreenControlModePacket::decode,
                UpdateScreenControlModePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
    }

    public static void sendBiosAction(BlockPos pos, ComputerBiosAction action) {
        CHANNEL.sendToServer(new ComputerBiosActionPacket(pos, action));
    }

    public static void startScreenControl(ServerPlayer player, BlockPos screenPos, BlockPos computerPos, String computerId, boolean laptop, boolean vmDisplayActive) {
        if (!MultiplayerScreens.acquire(player, screenPos, computerId)) return;
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new StartScreenControlPacket(screenPos, computerPos, computerId, laptop, vmDisplayActive));
    }

    public static void stopScreenControl(ServerPlayer player, String computerId) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new StopScreenControlPacket(computerId));
    }

    public static void updateScreenControlMode(ServerPlayer player, String computerId, boolean vmDisplayActive) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new UpdateScreenControlModePacket(computerId, vmDisplayActive));
    }

    public static void sendKeyboardInput(BlockPos screenPos, String computerId, String key, boolean pressed) {
        CHANNEL.sendToServer(ComputerInputPacket.keyboard(screenPos, computerId, key, pressed));
    }

    public static void sendMouseButtonInput(BlockPos screenPos, String computerId, int button, boolean pressed) {
        CHANNEL.sendToServer(ComputerInputPacket.mouseButton(screenPos, computerId, button, pressed));
    }

    public static void sendMouseMoveInput(BlockPos screenPos, String computerId, int x, int y, int screenWidth, int screenHeight) {
        CHANNEL.sendToServer(ComputerInputPacket.mouseMove(screenPos, computerId, x, y, screenWidth, screenHeight));
    }

    public static void sendMouseWheelInput(BlockPos screenPos, String computerId, int steps) {
        CHANNEL.sendToServer(ComputerInputPacket.mouseWheel(screenPos, computerId, steps));
    }
}
