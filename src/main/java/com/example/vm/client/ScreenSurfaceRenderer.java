package com.example.vm.client;

import com.example.vm.computer.ComputerBiosAction;
import com.example.vm.screen.ScreenFrame;
import com.example.vm.screen.VncEndpoint;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;

import net.minecraft.client.renderer.MultiBufferSource;
import java.util.Optional;

public final class ScreenSurfaceRenderer {
    private static final float VM_SCREEN_WIDTH = 258.0F;
    private static final float VM_SCREEN_HEIGHT = 130.0F;
    private static final float LAPTOP_VM_SCREEN_WIDTH = 287.0F;
    private static final float LAPTOP_VM_SCREEN_HEIGHT = 156.0F;
    static final float CURSOR_SURFACE_Z = -0.03F;

    private ScreenSurfaceRenderer() {
    }

    public static void renderText(PoseStack poseStack, MultiBufferSource buffer, BlockState state, float y, float zOffset, String line1, String line2, int packedLight) {
        renderText(poseStack, buffer, state, y, zOffset, 0.006F, line1, line2, packedLight);
    }

    public static void renderText(PoseStack poseStack, MultiBufferSource buffer, BlockState state, float y, float zOffset, float scale, String line1, String line2, int packedLight) {
        Direction facing = state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                ? state.getValue(BlockStateProperties.HORIZONTAL_FACING)
                : Direction.NORTH;
        poseStack.pushPose();
        poseStack.translate(0.5D, y, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotationFor(facing)));
        poseStack.translate(0.0D, 0.0D, zOffset);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        poseStack.scale(scale, scale, scale);

        Font font = Minecraft.getInstance().font;
        Matrix4f matrix = poseStack.last().pose();
        font.drawInBatch(line1, -font.width(line1) / 2.0F, -9.0F, 0x66CCFF, false, matrix, buffer, Font.DisplayMode.NORMAL, 0, packedLight);
        font.drawInBatch(line2, -font.width(line2) / 2.0F, 5.0F, 0xFFFFFF, false, matrix, buffer, Font.DisplayMode.NORMAL, 0, packedLight);
        poseStack.popPose();
    }

    public static void renderBiosPanel(PoseStack poseStack, MultiBufferSource buffer, BlockState state, float y, float zOffset, float scale, String title, String computerId, String discLabel, boolean active, int packedLight) {
        Direction facing = state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                ? state.getValue(BlockStateProperties.HORIZONTAL_FACING)
                : Direction.NORTH;
        poseStack.pushPose();
        poseStack.translate(0.5D, y, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotationFor(facing)));
        poseStack.translate(0.0D, 0.0D, zOffset);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        poseStack.scale(scale, scale, scale);

        Font font = Minecraft.getInstance().font;
        Matrix4f matrix = poseStack.last().pose();
        drawCentered(font, matrix, buffer, title, -42.0F, 0x66CCFF, packedLight);
        drawCentered(font, matrix, buffer, "ID " + shortId(computerId), -28.0F, 0xD7E8FF, packedLight);
        drawCentered(font, matrix, buffer, "DISC " + discLabel, -14.0F, 0xD7E8FF, packedLight);
        drawCentered(font, matrix, buffer, optionLabel(ComputerBiosAction.BOOT_FROM_DISC, "Boot Disc") + " " + optionLabel(ComputerBiosAction.BOOT_VM_DISK, "Boot Disk"), 4.0F, 0xFFCC66, packedLight);
        drawCentered(font, matrix, buffer, optionLabel(ComputerBiosAction.POWER_OFF, "Power Off") + " " + optionLabel(ComputerBiosAction.EJECT_DISC, "Eject"), 18.0F, 0xC9D4E2, packedLight);
        drawCentered(font, matrix, buffer, active ? "CLICK/ENTER ACTIVATE  CTRL+ALT EXIT" : "RIGHT CLICK TO CONTROL", 46.0F, active ? 0xA9C7FF : 0xA9C7FF, packedLight);
        poseStack.popPose();
    }

    public static void renderVmDisplay(PoseStack poseStack, MultiBufferSource buffer, BlockState state, float y, float zOffset, float scale, String computerId, boolean renderPointer, Optional<ScreenFrame> latestFrame, int packedLight) {
        renderVmDisplay(poseStack, buffer, state, y, zOffset, scale, VM_SCREEN_WIDTH, VM_SCREEN_HEIGHT, computerId, renderPointer, latestFrame, packedLight);
    }

    public static void renderLaptopVmDisplay(PoseStack poseStack, MultiBufferSource buffer, BlockState state, float y, float zOffset, float scale, String computerId, boolean renderPointer, Optional<ScreenFrame> latestFrame, int packedLight) {
        renderVmDisplay(poseStack, buffer, state, y, zOffset, scale, LAPTOP_VM_SCREEN_WIDTH, LAPTOP_VM_SCREEN_HEIGHT, computerId, renderPointer, latestFrame, packedLight);
    }

    private static void renderVmDisplay(PoseStack poseStack, MultiBufferSource buffer, BlockState state, float y, float zOffset, float scale, float screenWidth, float screenHeight, String computerId, boolean renderPointer, Optional<ScreenFrame> latestFrame, int packedLight) {
        Direction facing = state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                ? state.getValue(BlockStateProperties.HORIZONTAL_FACING)
                : Direction.NORTH;
        poseStack.pushPose();
        poseStack.translate(0.5D, y, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotationFor(facing)));
        poseStack.translate(0.0D, 0.0D, zOffset);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        poseStack.scale(scale, scale, scale);

        Font font = Minecraft.getInstance().font;
        Matrix4f matrix = poseStack.last().pose();
        if (latestFrame.isPresent()) {
            ScreenFrame frame = latestFrame.get();
            Optional<ResourceLocation> texture = ScreenFrameTextureCache.updateAndGet(computerId, frame);
            float[] fitted = fitToScreen(frame.width(), frame.height(), screenWidth, screenHeight);
            texture.ifPresent(resourceLocation -> drawTextureQuad(matrix, buffer, resourceLocation, -fitted[0] / 2.0F, -fitted[1] / 2.0F, fitted[0], fitted[1], packedLight));
            if (renderPointer && ImmersiveControlClient.shouldRenderVmPointer(computerId)) {
                float[] cursor = cursorPosition(
                        ImmersiveControlClient.vmPointerX(),
                        ImmersiveControlClient.vmPointerY(),
                        ImmersiveControlClient.vmPointerWidth(),
                        ImmersiveControlClient.vmPointerHeight(),
                        fitted[0],
                        fitted[1]
                );
                drawCursor(matrix, buffer, -fitted[0] / 2.0F + cursor[0], -fitted[1] / 2.0F + cursor[1]);
            }
        } else {
            drawCentered(font, matrix, buffer, "VM RUNNING", -26.0F, 0x66FF99, packedLight);
            drawCentered(font, matrix, buffer, "VNC " + VncEndpoint.forComputer(computerId).toDisplayAddress(), -9.0F, 0xD7E8FF, packedLight);
            drawCentered(font, matrix, buffer, "WAITING FOR FRAME", 8.0F, 0xFFCC66, packedLight);
            drawCentered(font, matrix, buffer, "CTRL+ALT EXIT", 29.0F, 0xA9C7FF, packedLight);
        }
        poseStack.popPose();
    }

    private static void drawTextureQuad(Matrix4f matrix, MultiBufferSource buffer, ResourceLocation texture, float x, float y, float width, float height, int packedLight) {
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.text(texture));
        vertexConsumer.vertex(matrix, x, y + height, 0.0F).color(255, 255, 255, 255).uv(0.0F, 1.0F).uv2(packedLight).endVertex();
        vertexConsumer.vertex(matrix, x + width, y + height, 0.0F).color(255, 255, 255, 255).uv(1.0F, 1.0F).uv2(packedLight).endVertex();
        vertexConsumer.vertex(matrix, x + width, y, 0.0F).color(255, 255, 255, 255).uv(1.0F, 0.0F).uv2(packedLight).endVertex();
        vertexConsumer.vertex(matrix, x, y, 0.0F).color(255, 255, 255, 255).uv(0.0F, 0.0F).uv2(packedLight).endVertex();
        vertexConsumer.vertex(matrix, x, y, -0.01F).color(255, 255, 255, 255).uv(0.0F, 0.0F).uv2(packedLight).endVertex();
        vertexConsumer.vertex(matrix, x + width, y, -0.01F).color(255, 255, 255, 255).uv(1.0F, 0.0F).uv2(packedLight).endVertex();
        vertexConsumer.vertex(matrix, x + width, y + height, -0.01F).color(255, 255, 255, 255).uv(1.0F, 1.0F).uv2(packedLight).endVertex();
        vertexConsumer.vertex(matrix, x, y + height, -0.01F).color(255, 255, 255, 255).uv(0.0F, 1.0F).uv2(packedLight).endVertex();
    }

    static float[] fitToScreen(int sourceWidth, int sourceHeight, float maxWidth, float maxHeight) {
        float sourceAspect = (float) sourceWidth / sourceHeight;
        float targetAspect = maxWidth / maxHeight;
        if (sourceAspect >= targetAspect) {
            return new float[] {maxWidth, maxWidth / sourceAspect};
        }
        return new float[] {maxHeight * sourceAspect, maxHeight};
    }

    static float[] cursorPosition(int pointerX, int pointerY, int pointerWidth, int pointerHeight, float fittedWidth, float fittedHeight) {
        float x = fittedWidth * clamp(pointerX, 0, pointerWidth - 1) / Math.max(1, pointerWidth - 1);
        float y = fittedHeight * clamp(pointerY, 0, pointerHeight - 1) / Math.max(1, pointerHeight - 1);
        return new float[] {x, y};
    }

    private static void drawCursor(Matrix4f matrix, MultiBufferSource buffer, float x, float y) {
        drawCursorBlock(matrix, buffer, x + 0.6F, y + 0.6F, 0.8F, 6.0F, 0, 0, 0, 180);
        drawCursorBlock(matrix, buffer, x + 0.6F, y + 0.6F, 5.0F, 0.8F, 0, 0, 0, 180);
        drawCursorBlock(matrix, buffer, x, y, 0.8F, 6.0F, 255, 255, 255, 255);
        drawCursorBlock(matrix, buffer, x, y, 5.0F, 0.8F, 255, 255, 255, 255);
    }

    private static void drawCursorBlock(Matrix4f matrix, MultiBufferSource buffer, float x, float y, float width, float height, int red, int green, int blue, int alpha) {
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.gui());
        vertexConsumer.vertex(matrix, x, y + height, CURSOR_SURFACE_Z).color(red, green, blue, alpha).endVertex();
        vertexConsumer.vertex(matrix, x + width, y + height, CURSOR_SURFACE_Z).color(red, green, blue, alpha).endVertex();
        vertexConsumer.vertex(matrix, x + width, y, CURSOR_SURFACE_Z).color(red, green, blue, alpha).endVertex();
        vertexConsumer.vertex(matrix, x, y, CURSOR_SURFACE_Z).color(red, green, blue, alpha).endVertex();
    }

    private static String optionLabel(ComputerBiosAction action, String label) {
        boolean selected = ImmersiveControlClient.active()
                ? ImmersiveControlClient.selectedAction() == action
                : action == ComputerBiosAction.BOOT_FROM_DISC;
        return selected ? ">[" + label + "]" : "[" + label + "]";
    }

    private static void drawCentered(Font font, Matrix4f matrix, MultiBufferSource buffer, String text, float y, int color, int packedLight) {
        font.drawInBatch(text, -font.width(text) / 2.0F, y, color, false, matrix, buffer, Font.DisplayMode.NORMAL, 0, packedLight);
    }

    private static String shortId(String computerId) {
        if (computerId == null || computerId.isBlank()) {
            return "UNKNOWN";
        }
        return computerId.length() <= 13 ? computerId : computerId.substring(0, 13);
    }

    private static float rotationFor(Direction facing) {
        return switch (facing) {
            case SOUTH -> 180.0F;
            case WEST -> 90.0F;
            case EAST -> 270.0F;
            default -> 0.0F;
        };
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
