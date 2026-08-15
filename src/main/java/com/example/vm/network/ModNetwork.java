package com.example.vm.network;

import com.example.vm.ComputerInMinecraft;
import com.example.vm.computer.ComputerBiosAction;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String PROTOCOL_VERSION = "1";
    private static int nextPacketId;

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(ComputerInMinecraft.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private ModNetwork() {
    }

    public static void register() {
        CHANNEL.registerMessage(
                nextPacketId++,
                ComputerBiosActionPacket.class,
                ComputerBiosActionPacket::encode,
                ComputerBiosActionPacket::decode,
                ComputerBiosActionPacket::handle
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                StartScreenControlPacket.class,
                StartScreenControlPacket::encode,
                StartScreenControlPacket::decode,
                StartScreenControlPacket::handle
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                ComputerInputPacket.class,
                ComputerInputPacket::encode,
                ComputerInputPacket::decode,
                ComputerInputPacket::handle
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                StopScreenControlPacket.class,
                StopScreenControlPacket::encode,
                StopScreenControlPacket::decode,
                StopScreenControlPacket::handle
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                UpdateScreenControlModePacket.class,
                UpdateScreenControlModePacket::encode,
                UpdateScreenControlModePacket::decode,
                UpdateScreenControlModePacket::handle
        );
    }

    public static void sendBiosAction(BlockPos pos, ComputerBiosAction action) {
        CHANNEL.sendToServer(new ComputerBiosActionPacket(pos, action));
    }

    public static void startScreenControl(ServerPlayer player, BlockPos screenPos, BlockPos computerPos, String computerId, boolean laptop, boolean vmDisplayActive) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new StartScreenControlPacket(screenPos, computerPos, computerId, laptop, vmDisplayActive));
    }

    public static void stopScreenControl(ServerPlayer player, String computerId) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new StopScreenControlPacket(computerId));
    }

    public static void updateScreenControlMode(ServerPlayer player, String computerId, boolean vmDisplayActive) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new UpdateScreenControlModePacket(computerId, vmDisplayActive));
    }

    public static void sendKeyboardInput(String computerId, String key, boolean pressed) {
        CHANNEL.sendToServer(ComputerInputPacket.keyboard(computerId, key, pressed));
    }

    public static void sendMouseButtonInput(String computerId, int button, boolean pressed) {
        CHANNEL.sendToServer(ComputerInputPacket.mouseButton(computerId, button, pressed));
    }

    public static void sendMouseMoveInput(String computerId, int x, int y, int screenWidth, int screenHeight) {
        CHANNEL.sendToServer(ComputerInputPacket.mouseMove(computerId, x, y, screenWidth, screenHeight));
    }

    public static void sendMouseWheelInput(String computerId, int steps) {
        CHANNEL.sendToServer(ComputerInputPacket.mouseWheel(computerId, steps));
    }
}
