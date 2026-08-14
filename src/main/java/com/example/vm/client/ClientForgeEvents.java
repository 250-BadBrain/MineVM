package com.example.vm.client;

import com.example.vm.ComputerInMinecraft;
import com.example.vm.block.ComputerOccupancyBlock;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = ComputerInMinecraft.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientForgeEvents {
    private static boolean redirectingOccupancyBreak;

    private ClientForgeEvents() {
    }

    @SubscribeEvent
    public static void onKey(InputEvent.Key event) {
        if (event.getAction() == GLFW.GLFW_PRESS && ImmersiveControlClient.shouldRelease(event.getKey())) {
            ImmersiveControlClient.stop();
            cancelIfPossible(event);
            return;
        }
        if (ImmersiveControlClient.active() && ImmersiveControlClient.controllingVmDisplay()) {
            if (event.getAction() == GLFW.GLFW_PRESS || event.getAction() == GLFW.GLFW_REPEAT) {
                ImmersiveControlClient.handleVmKey(event.getKey(), event.getModifiers(), true);
            }
            cancelIfPossible(event);
            return;
        }
        if (event.getAction() == GLFW.GLFW_PRESS && ImmersiveControlClient.active()) {
            if (event.getKey() == GLFW.GLFW_KEY_TAB || event.getKey() == GLFW.GLFW_KEY_DOWN || event.getKey() == GLFW.GLFW_KEY_RIGHT) {
                ImmersiveControlClient.selectNextAction();
            } else if (event.getKey() == GLFW.GLFW_KEY_UP || event.getKey() == GLFW.GLFW_KEY_LEFT) {
                ImmersiveControlClient.selectPreviousAction();
            } else if (event.getKey() == GLFW.GLFW_KEY_ENTER || event.getKey() == GLFW.GLFW_KEY_KP_ENTER || event.getKey() == GLFW.GLFW_KEY_SPACE) {
                ImmersiveControlClient.activateSelectedAction();
            }
            cancelIfPossible(event);
            return;
        }
        if (ImmersiveControlClient.shouldCaptureKey(event.getKey())) {
            cancelIfPossible(event);
        }
    }

    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        if (ImmersiveControlClient.active()) {
            if (ImmersiveControlClient.controllingVmDisplay()) {
                if (event.getAction() == GLFW.GLFW_PRESS || event.getAction() == GLFW.GLFW_RELEASE) {
                    ImmersiveControlClient.handleVmMouseButton(event.getButton(), event.getAction() == GLFW.GLFW_PRESS);
                }
            } else if (event.getAction() == GLFW.GLFW_PRESS && event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                    ImmersiveControlClient.activateSelectedAction();
            }
            cancelIfPossible(event);
        }
    }

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (ImmersiveControlClient.active()) {
            if (event.getScrollDelta() < 0.0D) {
                ImmersiveControlClient.selectNextAction();
            } else if (event.getScrollDelta() > 0.0D) {
                ImmersiveControlClient.selectPreviousAction();
            }
            cancelIfPossible(event);
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (redirectingOccupancyBreak || event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.gameMode == null || !event.getEntity().level().isClientSide()) {
            return;
        }
        BlockPos pos = event.getPos();
        if (!(minecraft.level.getBlockState(pos).getBlock() instanceof ComputerOccupancyBlock)) {
            return;
        }
        BlockPos origin = ComputerOccupancyBlock.origin(minecraft.level, pos);
        if (origin == null || origin.equals(pos) || minecraft.level.getBlockState(origin).isAir()) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        redirectingOccupancyBreak = true;
        try {
            minecraft.gameMode.startDestroyBlock(origin, event.getFace());
        } finally {
            redirectingOccupancyBreak = false;
        }
    }

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (!ImmersiveControlClient.active()) {
            return;
        }
        ImmersiveControlClient.suppressMinecraftBindings();
        event.getInput().leftImpulse = 0.0F;
        event.getInput().forwardImpulse = 0.0F;
        event.getInput().up = false;
        event.getInput().down = false;
        event.getInput().left = false;
        event.getInput().right = false;
        event.getInput().jumping = false;
        event.getInput().shiftKeyDown = false;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && ImmersiveControlClient.active()) {
            ImmersiveControlClient.suppressMinecraftBindings();
            ImmersiveControlClient.releaseMouseForScreenControl();
            ImmersiveControlClient.syncVmMousePosition();
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.screen != null && !(minecraft.screen instanceof net.minecraft.client.gui.screens.PauseScreen)) {
                minecraft.setScreen(null);
            }
        }
    }

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (ImmersiveControlClient.active() && event.getNewScreen() != null) {
            event.setCanceled(true);
            ImmersiveControlClient.suppressMinecraftBindings();
        }
    }

    @SubscribeEvent
    public static void onScreenKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (ImmersiveControlClient.active()) {
            event.setCanceled(true);
            ImmersiveControlClient.suppressMinecraftBindings();
        }
    }

    @SubscribeEvent
    public static void onScreenKeyReleased(ScreenEvent.KeyReleased.Pre event) {
        if (ImmersiveControlClient.active()) {
            event.setCanceled(true);
            ImmersiveControlClient.suppressMinecraftBindings();
        }
    }

    @SubscribeEvent
    public static void onScreenCharacterTyped(ScreenEvent.CharacterTyped.Pre event) {
        if (ImmersiveControlClient.active()) {
            event.setCanceled(true);
            ImmersiveControlClient.suppressMinecraftBindings();
        }
    }

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Pre event) {
        if (ImmersiveControlClient.active() && event.getOverlay().id().equals(VanillaGuiOverlay.CROSSHAIR.id())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ScreenFrameTextureCache.clearAll();
        ImmersiveControlClient.stop();
    }

    private static void cancelIfPossible(net.minecraftforge.eventbus.api.Event event) {
        if (event.isCancelable()) {
            event.setCanceled(true);
        }
    }
}
