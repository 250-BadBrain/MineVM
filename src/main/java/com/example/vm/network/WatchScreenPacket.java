package com.example.vm.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record WatchScreenPacket(BlockPos screen) {
    public static void encode(WatchScreenPacket packet, FriendlyByteBuf buffer) { buffer.writeBlockPos(packet.screen); }
    public static WatchScreenPacket decode(FriendlyByteBuf buffer) { return new WatchScreenPacket(buffer.readBlockPos()); }
    public static void handle(WatchScreenPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> { if (context.getSender() != null) MultiplayerScreens.watch(context.getSender(), packet.screen); });
        context.setPacketHandled(true);
    }
}
