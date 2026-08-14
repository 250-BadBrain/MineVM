package com.example.vm.network;

import com.example.vm.computer.ComputerBiosAction;
import com.example.vm.computer.ComputerBiosActions;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ComputerBiosActionPacket(BlockPos pos, ComputerBiosAction action) {
    public static void encode(ComputerBiosActionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.pos);
        buffer.writeEnum(packet.action);
    }

    public static ComputerBiosActionPacket decode(FriendlyByteBuf buffer) {
        return new ComputerBiosActionPacket(buffer.readBlockPos(), buffer.readEnum(ComputerBiosAction.class));
    }

    public static void handle(ComputerBiosActionPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender != null) {
                ComputerBiosActions.handle(sender, packet.pos, packet.action);
            }
        });
        context.setPacketHandled(true);
    }
}
