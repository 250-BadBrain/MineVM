package com.example.vm.client;

import com.example.vm.screen.FrameMetrics;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;

public final class ClientDiagnosticCommands {
    public static void register(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("minevmclient")
                .then(Commands.literal("stats").executes(context -> {
                    Minecraft minecraft = Minecraft.getInstance();
                    String id = ImmersiveControlClient.activeComputerId();
                    if (id.isBlank() && minecraft.level != null && minecraft.hitResult instanceof net.minecraft.world.phys.BlockHitResult hit) {
                        var entity = minecraft.level.getBlockEntity(hit.getBlockPos());
                        if (entity instanceof com.example.vm.blockentity.ComputerMonitorBlockEntity monitor) id = monitor.linkedComputerId().orElse("");
                        else if (entity instanceof com.example.vm.blockentity.ComputerBlockEntity computer) id = computer.getComputerId();
                    }
                    if (minecraft.player == null) return 0;
                    if (id.isBlank()) { minecraft.player.sendSystemMessage(Component.translatable("command.minevm.look_at_computer")); return 0; }
                    var metrics = FrameMetrics.forComputer(id).snapshot();
                    minecraft.player.sendSystemMessage(Component.translatable("command.minevm.frame_status", metrics.frames(),
                            metrics.frames() == 0 ? 0 : metrics.mergeNanos() / metrics.frames() / 1000,
                            metrics.uploads(), metrics.uploads() == 0 ? 0 : metrics.uploadNanos() / metrics.uploads() / 1000, metrics.networkBytes()));
                    return 1;
                })));
    }
    private ClientDiagnosticCommands() {}
}
