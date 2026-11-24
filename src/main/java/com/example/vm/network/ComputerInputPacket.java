package com.example.vm.network;

import com.example.vm.MineVM;
import com.example.vm.input.KeyboardInput;
import com.example.vm.input.MouseButtonInput;
import com.example.vm.input.MouseMoveInput;
import com.example.vm.input.MouseWheelInput;
import com.example.vm.input.VirtualMachineInput;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ComputerInputPacket(BlockPos screenPos, String computerId, InputKind kind, String key, int button, int x, int y, int screenWidth, int screenHeight, boolean pressed) {
    private static final int MAX_COMPUTER_ID_LENGTH = 128;
    private static final int MAX_KEY_LENGTH = 64;
    public enum InputKind {
        KEYBOARD,
        MOUSE_BUTTON,
        MOUSE_MOVE,
        MOUSE_WHEEL
    }

    public static ComputerInputPacket keyboard(BlockPos screenPos, String computerId, String key, boolean pressed) {
        return new ComputerInputPacket(screenPos, computerId, InputKind.KEYBOARD, key, 0, 0, 0, 1, 1, pressed);
    }

    public static ComputerInputPacket mouseButton(BlockPos screenPos, String computerId, int button, boolean pressed) {
        return new ComputerInputPacket(screenPos, computerId, InputKind.MOUSE_BUTTON, "", button, 0, 0, 1, 1, pressed);
    }

    public static ComputerInputPacket mouseMove(BlockPos screenPos, String computerId, int x, int y, int screenWidth, int screenHeight) {
        return new ComputerInputPacket(screenPos, computerId, InputKind.MOUSE_MOVE, "", 0, x, y, screenWidth, screenHeight, false);
    }

    public static ComputerInputPacket mouseWheel(BlockPos screenPos, String computerId, int steps) {
        return new ComputerInputPacket(screenPos, computerId, InputKind.MOUSE_WHEEL, "", steps, 0, 0, 1, 1, false);
    }

    public static void encode(ComputerInputPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.screenPos);
        buffer.writeUtf(packet.computerId, MAX_COMPUTER_ID_LENGTH);
        buffer.writeEnum(packet.kind);
        buffer.writeUtf(packet.key, MAX_KEY_LENGTH);
        buffer.writeInt(packet.button);
        buffer.writeInt(packet.x);
        buffer.writeInt(packet.y);
        buffer.writeInt(packet.screenWidth);
        buffer.writeInt(packet.screenHeight);
        buffer.writeBoolean(packet.pressed);
    }

    public static ComputerInputPacket decode(FriendlyByteBuf buffer) {
        return new ComputerInputPacket(buffer.readBlockPos(), buffer.readUtf(MAX_COMPUTER_ID_LENGTH), buffer.readEnum(InputKind.class), buffer.readUtf(MAX_KEY_LENGTH), buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readBoolean());
    }

    public static void handle(ComputerInputPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null || !MultiplayerScreens.allowsInput(sender, packet.screenPos, packet.computerId)) {
                return;
            }
            VirtualMachineInput input;
            try {
                input = switch (packet.kind) {
                    case KEYBOARD -> new KeyboardInput(packet.computerId, packet.key, packet.pressed);
                    case MOUSE_BUTTON -> new MouseButtonInput(packet.computerId, packet.button, packet.pressed);
                    case MOUSE_MOVE -> new MouseMoveInput(packet.computerId, packet.x, packet.y, packet.screenWidth, packet.screenHeight);
                    case MOUSE_WHEEL -> new MouseWheelInput(packet.computerId, packet.button);
                };
            } catch (IllegalArgumentException exception) {
                return;
            }
            if (!MultiplayerScreens.trackInput(sender, input)) return;
            if (!MineVM.SCREEN_SERVICE.sendVncInput(packet.computerId, input)) {
                MineVM.INPUT_SERVICE.send(input);
            }
        });
        context.setPacketHandled(true);
    }
}
