package com.example.vm.client;

import com.example.vm.ComputerInMinecraft;
import com.example.vm.blockentity.ComputerBlockEntity;
import com.example.vm.blockentity.ComputerMonitorBlockEntity;
import com.example.vm.vm.GuestOperatingSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.entity.BlockEntity;

public class ComputerMonitorRenderer implements BlockEntityRenderer<ComputerMonitorBlockEntity> {
    private static final float SCREEN_Y = 1.22F;
    private static final float SCREEN_Z = -0.283F;
    private static final float TEXT_SCALE = 0.0105F;

    public ComputerMonitorRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(ComputerMonitorBlockEntity monitor, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (!monitor.displayPowered()) {
            ScreenSurfaceRenderer.renderText(poseStack, buffer, monitor.getBlockState(), SCREEN_Y, SCREEN_Z, TEXT_SCALE, "", "", packedLight);
            return;
        }
        if (monitor.linkedComputerId().isEmpty()) {
            ScreenSurfaceRenderer.renderText(poseStack, buffer, monitor.getBlockState(), SCREEN_Y, SCREEN_Z, TEXT_SCALE, "MONITOR", "NO HDMI", packedLight);
            return;
        }

        String computer = monitor.linkedComputerId().get();
        if (monitor.getLevel() != null && monitor.linkedComputerPos().isPresent()) {
            BlockEntity linkedEntity = monitor.getLevel().getBlockEntity(monitor.linkedComputerPos().get());
            if (linkedEntity instanceof ComputerBlockEntity linkedComputer) {
                if (!linkedComputer.poweredOn()) {
                    ScreenSurfaceRenderer.renderText(poseStack, buffer, monitor.getBlockState(), SCREEN_Y, SCREEN_Z, TEXT_SCALE, "MONITOR", "NO SIGNAL", packedLight);
                    return;
                }
                if (linkedComputer.vmDisplayActive()) {
                    GuestOperatingSystem guestOperatingSystem = linkedComputer.displayGuestOperatingSystem();
                    ScreenSurfaceRenderer.renderVmDisplay(
                            poseStack,
                            buffer,
                            monitor.getBlockState(),
                            SCREEN_Y,
                            SCREEN_Z,
                            0.0112F,
                            linkedComputer.getComputerId(),
                            guestOperatingSystem != null && guestOperatingSystem.usesGraphicalPointer(),
                            ComputerInMinecraft.SCREEN_SERVICE.latestFrameSnapshot(linkedComputer.getComputerId()),
                            packedLight
                    );
                    return;
                }
                boolean active = ImmersiveControlClient.activeComputerId().equals(computer);
                ScreenSurfaceRenderer.renderBiosPanel(
                        poseStack,
                        buffer,
                        monitor.getBlockState(),
                        SCREEN_Y,
                        SCREEN_Z,
                        0.0112F,
                        "Computer BIOS",
                        linkedComputer.getComputerId(),
                        discLabel(linkedComputer),
                        active,
                        packedLight
                );
                return;
            }
        }

        ScreenSurfaceRenderer.renderText(poseStack, buffer, monitor.getBlockState(), SCREEN_Y, SCREEN_Z, TEXT_SCALE, "MONITOR", "LINKED CASE MISSING", packedLight);
    }

    @Override
    public boolean shouldRenderOffScreen(ComputerMonitorBlockEntity monitor) {
        return true;
    }

    private static String discLabel(ComputerBlockEntity computer) {
        return computer.insertedDisc().map(Enum::name).orElse("EMPTY");
    }
}
