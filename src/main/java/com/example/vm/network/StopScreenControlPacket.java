package com.example.vm.network;

import com.example.vm.client.ImmersiveControlClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record StopScreenControlPacket(String computerId) {
    public static void encode(StopScreenControlPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.computerId);
    }

    public static StopScreenControlPacket decode(FriendlyByteBuf buffer) {
        return new StopScreenControlPacket(buffer.readUtf());
    }

    public static void handle(StopScreenControlPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ImmersiveControlClient.stopIfControlling(packet.computerId)));
        context.setPacketHandled(true);
    }
}
