package com.example.vm.network;

import com.example.vm.client.ImmersiveControlClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record UpdateScreenControlModePacket(String computerId, boolean vmDisplayActive) {
    public static void encode(UpdateScreenControlModePacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.computerId);
        buffer.writeBoolean(packet.vmDisplayActive);
    }

    public static UpdateScreenControlModePacket decode(FriendlyByteBuf buffer) {
        return new UpdateScreenControlModePacket(buffer.readUtf(), buffer.readBoolean());
    }

    public static void handle(UpdateScreenControlModePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ImmersiveControlClient.updateVmInputMode(packet.computerId, packet.vmDisplayActive)));
        context.setPacketHandled(true);
    }
}
