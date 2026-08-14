package com.example.vm.network;

import com.example.vm.client.ImmersiveControlClient;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record StartScreenControlPacket(BlockPos screenPos, BlockPos computerPos, String computerId, boolean laptop, boolean vmDisplayActive) {
    public static void encode(StartScreenControlPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.screenPos);
        buffer.writeBlockPos(packet.computerPos);
        buffer.writeUtf(packet.computerId);
        buffer.writeBoolean(packet.laptop);
        buffer.writeBoolean(packet.vmDisplayActive);
    }

    public static StartScreenControlPacket decode(FriendlyByteBuf buffer) {
        return new StartScreenControlPacket(buffer.readBlockPos(), buffer.readBlockPos(), buffer.readUtf(), buffer.readBoolean(), buffer.readBoolean());
    }

    public static void handle(StartScreenControlPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ImmersiveControlClient.start(packet)));
        context.setPacketHandled(true);
    }
}
