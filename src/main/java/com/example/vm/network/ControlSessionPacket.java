package com.example.vm.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record ControlSessionPacket(String computerId, boolean release) {
    public static void encode(ControlSessionPacket packet, FriendlyByteBuf buffer) { buffer.writeUtf(packet.computerId, 128); buffer.writeBoolean(packet.release); }
    public static ControlSessionPacket decode(FriendlyByteBuf buffer) { return new ControlSessionPacket(buffer.readUtf(128), buffer.readBoolean()); }
    public static void handle(ControlSessionPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            if (context.getSender() == null) return;
            if (packet.release) MultiplayerScreens.release(context.getSender(), packet.computerId);
            else MultiplayerScreens.renew(context.getSender(), packet.computerId);
        });
        context.setPacketHandled(true);
    }
}
