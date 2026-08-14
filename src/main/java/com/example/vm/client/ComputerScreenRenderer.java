package com.example.vm.client;

import com.example.vm.ComputerInMinecraft;
import com.example.vm.blockentity.ComputerBlockEntity;
import com.example.vm.registry.ModBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public class ComputerScreenRenderer implements BlockEntityRenderer<ComputerBlockEntity> {
    private static final float SCREEN_Y = 1.18F;
    private static final float SCREEN_Z = 1.120F;
    private static final float SCREEN_SCALE = 0.0100F;

    public ComputerScreenRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(ComputerBlockEntity computer, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (computer.getBlockState().getBlock() != ModBlocks.LAPTOP_COMPUTER.get()) {
            return;
        }
        if (!computer.poweredOn()) {
            return;
        }

        if (computer.vmDisplayActive()) {
            ScreenSurfaceRenderer.renderLaptopVmDisplay(
                    poseStack,
                    buffer,
                    computer.getBlockState(),
                    SCREEN_Y,
                    SCREEN_Z,
                    SCREEN_SCALE,
                    computer.getComputerId(),
                    computer.activeGuestOperatingSystem().usesGraphicalPointer(),
                    ComputerInMinecraft.SCREEN_SERVICE.latestFrameSnapshot(computer.getComputerId()),
                    packedLight
            );
            return;
        }

        boolean active = ImmersiveControlClient.activeComputerId().equals(computer.getComputerId());
        ScreenSurfaceRenderer.renderBiosPanel(
                poseStack,
                buffer,
                computer.getBlockState(),
                SCREEN_Y,
                SCREEN_Z,
                SCREEN_SCALE,
                "Laptop BIOS",
                computer.getComputerId(),
                discLabel(computer),
                active,
                packedLight
        );
    }

    @Override
    public boolean shouldRenderOffScreen(ComputerBlockEntity computer) {
        return true;
    }

    private static String discLabel(ComputerBlockEntity computer) {
        return computer.insertedDisc().map(Enum::name).orElse("EMPTY");
    }
}
