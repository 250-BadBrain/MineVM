package com.example.vm.network;

import com.example.vm.screen.FrameCodecs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record ScreenFramePacket(String computerId, long epoch, long sequence, String codec, int width, int height, byte[] payload) {
    public static void encode(ScreenFramePacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.computerId, 128); buffer.writeLong(packet.epoch); buffer.writeLong(packet.sequence);
        buffer.writeUtf(packet.codec, 16); buffer.writeVarInt(packet.width); buffer.writeVarInt(packet.height); buffer.writeByteArray(packet.payload);
    }
    public static ScreenFramePacket decode(FriendlyByteBuf buffer) {
        return new ScreenFramePacket(buffer.readUtf(128), buffer.readLong(), buffer.readLong(), buffer.readUtf(16),
                buffer.readVarInt(), buffer.readVarInt(), buffer.readByteArray(FrameCodecs.MAX_PAYLOAD));
    }
    public static void handle(ScreenFramePacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.example.vm.client.ClientScreenFrames.receive(packet)));
        context.setPacketHandled(true);
    }
}
