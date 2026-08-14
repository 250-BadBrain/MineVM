package com.example.vm.client;

import com.example.vm.computer.ComputerBiosAction;
import com.example.vm.network.StartScreenControlPacket;
import com.example.vm.network.ModNetwork;
import com.example.vm.screen.ScreenControlMode;
import com.example.vm.screen.ScreenControlSession;
import net.minecraft.core.BlockPos;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class ImmersiveControlClient {
    private static final String RELEASE_HINT = "CTRL+ALT";
    private static final ComputerBiosAction[] BIOS_ACTIONS = {
            ComputerBiosAction.BOOT_FROM_DISC,
            ComputerBiosAction.BOOT_VM_DISK,
            ComputerBiosAction.POWER_OFF,
            ComputerBiosAction.EJECT_DISC
    };
    private static final int VM_POINTER_WIDTH = 1280;
    private static final int VM_POINTER_HEIGHT = 800;
    private static ScreenControlSession activeSession;
    private static BlockPos activeComputerPos;
    private static String activeComputerId = "";
    private static int selectedActionIndex;
    private static boolean vmInputMode;
    private static int capturedHotbarSlot = -1;
    private static int lastMouseX = -1;
    private static int lastMouseY = -1;

    private ImmersiveControlClient() {
    }

    public static void start(StartScreenControlPacket packet) {
        String playerId = Minecraft.getInstance().player == null ? "client" : Minecraft.getInstance().player.getStringUUID();
        activeSession = new ScreenControlSession(playerId, packet.screenPos(), ScreenControlMode.IMMERSIVE_CAPTURE);
        activeComputerPos = packet.computerPos();
        activeComputerId = packet.computerId();
        selectedActionIndex = 0;
        vmInputMode = packet.vmDisplayActive();
        capturedHotbarSlot = Minecraft.getInstance().player == null ? 0 : Minecraft.getInstance().player.getInventory().selected;
        lastMouseX = VM_POINTER_WIDTH / 2;
        lastMouseY = VM_POINTER_HEIGHT / 2;
        releaseMouseForScreenControl();
        suppressMinecraftBindings();
        hideCursorForScreenControl();
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.displayClientMessage(
                    Component.literal("Screen control active: " + activeComputerId + " | Press " + RELEASE_HINT + " to release."),
                    true
            );
        }
    }

    public static void stop() {
        boolean wasActive = active();
        if (wasActive) {
            suppressMinecraftBindings();
        }
        activeSession = null;
        activeComputerPos = null;
        activeComputerId = "";
        selectedActionIndex = 0;
        vmInputMode = false;
        capturedHotbarSlot = -1;
        lastMouseX = -1;
        lastMouseY = -1;
        showCursorAfterScreenControl();
        grabMouseAfterScreenControl();
        if (wasActive && Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.displayClientMessage(Component.literal("Screen control released."), true);
        }
    }

    public static void stopIfControlling(String computerId) {
        if (active() && activeComputerId.equals(computerId)) {
            stop();
        }
    }

    public static void updateVmInputMode(String computerId, boolean active) {
        if (active() && activeComputerId.equals(computerId)) {
            vmInputMode = active;
            suppressMinecraftBindings();
        }
    }

    public static boolean active() {
        return activeSession != null && activeSession.capturesInput();
    }

    public static String activeComputerId() {
        return activeComputerId;
    }

    public static int vmPointerX() {
        return lastMouseX;
    }

    public static int vmPointerY() {
        return lastMouseY;
    }

    public static int vmPointerWidth() {
        return VM_POINTER_WIDTH;
    }

    public static int vmPointerHeight() {
        return VM_POINTER_HEIGHT;
    }

    public static boolean shouldRenderVmPointer(String computerId) {
        return controllingVmDisplay() && activeComputerId.equals(computerId) && lastMouseX >= 0 && lastMouseY >= 0;
    }

    public static boolean controllingVmDisplay() {
        return active() && (vmInputMode || vmDisplayActive());
    }

    public static ComputerBiosAction selectedAction() {
        return BIOS_ACTIONS[selectedActionIndex];
    }

    public static void selectNextAction() {
        if (!active()) {
            return;
        }
        selectedActionIndex = (selectedActionIndex + 1) % BIOS_ACTIONS.length;
    }

    public static void selectPreviousAction() {
        if (!active()) {
            return;
        }
        selectedActionIndex = (selectedActionIndex + BIOS_ACTIONS.length - 1) % BIOS_ACTIONS.length;
    }

    public static void activateSelectedAction() {
        if (!active() || activeComputerPos == null) {
            return;
        }
        ComputerBiosAction action = selectedAction();
        ModNetwork.sendBiosAction(activeComputerPos, action);
        if (action == ComputerBiosAction.POWER_OFF) {
            vmInputMode = false;
        }
        suppressMinecraftBindings();
    }

    public static void handleVmKey(int key, int modifiers, boolean pressed) {
        if (!active() || activeComputerId.isBlank()) {
            return;
        }
        if (!pressed) {
            suppressMinecraftBindings();
            return;
        }
        if (key == GLFW.GLFW_KEY_ENTER) {
            ModNetwork.sendKeyboardInput(activeComputerId, "ret", true);
            suppressMinecraftBindings();
            return;
        }
        if (key == GLFW.GLFW_KEY_KP_ENTER) {
            ModNetwork.sendKeyboardInput(activeComputerId, "kp_enter", true);
            suppressMinecraftBindings();
            return;
        }
        String qcode = qcode(key, modifiers);
        if (qcode != null) {
            ModNetwork.sendKeyboardInput(activeComputerId, qcode, pressed);
        }
        suppressMinecraftBindings();
    }

    public static void handleVmMouseButton(int button, boolean pressed) {
        if (!active() || activeComputerId.isBlank()) {
            return;
        }
        syncVmMousePosition(true);
        int vmButton = switch (button) {
            case GLFW.GLFW_MOUSE_BUTTON_LEFT -> 0;
            case GLFW.GLFW_MOUSE_BUTTON_MIDDLE -> 1;
            case GLFW.GLFW_MOUSE_BUTTON_RIGHT -> 2;
            default -> button;
        };
        ModNetwork.sendMouseButtonInput(activeComputerId, vmButton, pressed);
        suppressMinecraftBindings();
    }

    public static void syncVmMousePosition() {
        syncVmMousePosition(false);
    }

    private static void syncVmMousePosition(boolean force) {
        if (!controllingVmDisplay() || activeComputerId.isBlank()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        double windowWidth = Math.max(1, minecraft.getWindow().getScreenWidth());
        double windowHeight = Math.max(1, minecraft.getWindow().getScreenHeight());
        int x = clamp((int) Math.floor(minecraft.mouseHandler.xpos() * VM_POINTER_WIDTH / windowWidth), 0, VM_POINTER_WIDTH - 1);
        int y = clamp((int) Math.floor(minecraft.mouseHandler.ypos() * VM_POINTER_HEIGHT / windowHeight), 0, VM_POINTER_HEIGHT - 1);
        if (!force && x == lastMouseX && y == lastMouseY) {
            return;
        }
        lastMouseX = x;
        lastMouseY = y;
        ModNetwork.sendMouseMoveInput(activeComputerId, x, y, VM_POINTER_WIDTH, VM_POINTER_HEIGHT);
        suppressMinecraftBindings();
    }

    public static boolean shouldRelease(int key) {
        return active() && isAlt(key) && isControlDown();
    }

    public static boolean shouldCaptureKey(int key) {
        return active() && !shouldRelease(key);
    }

    public static void suppressMinecraftBindings() {
        if (!active()) {
            return;
        }
        Options options = Minecraft.getInstance().options;
        release(options.keyUse);
        release(options.keyAttack);
        release(options.keyPickItem);
        release(options.keyUp);
        release(options.keyDown);
        release(options.keyLeft);
        release(options.keyRight);
        release(options.keyJump);
        release(options.keyShift);
        release(options.keySprint);
        release(options.keyInventory);
        release(options.keySwapOffhand);
        release(options.keyDrop);
        release(options.keyChat);
        release(options.keyCommand);
        release(options.keyPlayerList);
        release(options.keyAdvancements);
        release(options.keyScreenshot);
        release(options.keyTogglePerspective);
        release(options.keySmoothCamera);
        release(options.keyFullscreen);
        release(options.keySpectatorOutlines);
        release(options.keySaveHotbarActivator);
        release(options.keyLoadHotbarActivator);
        for (KeyMapping keyMapping : options.keyHotbarSlots) {
            release(keyMapping);
        }
        if (Minecraft.getInstance().player != null && capturedHotbarSlot >= 0) {
            Minecraft.getInstance().player.getInventory().selected = capturedHotbarSlot;
        }
    }

    private static void release(KeyMapping keyMapping) {
        keyMapping.setDown(false);
        while (keyMapping.consumeClick()) {
            // Drain queued vanilla actions while the VM owns input.
        }
    }

    public static void releaseMouseForScreenControl() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.mouseHandler.isMouseGrabbed()) {
            minecraft.mouseHandler.releaseMouse();
        }
        hideCursorForScreenControl();
    }

    public static void grabMouseAfterScreenControl() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen == null && minecraft.player != null && !minecraft.mouseHandler.isMouseGrabbed()) {
            minecraft.mouseHandler.grabMouse();
        }
    }

    public static void hideCursorForScreenControl() {
        Minecraft minecraft = Minecraft.getInstance();
        GLFW.glfwSetInputMode(minecraft.getWindow().getWindow(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_HIDDEN);
    }

    public static void showCursorAfterScreenControl() {
        Minecraft minecraft = Minecraft.getInstance();
        GLFW.glfwSetInputMode(minecraft.getWindow().getWindow(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
    }

    private static boolean isControlDown() {
        long window = Minecraft.getInstance().getWindow().getWindow();
        return org.lwjgl.glfw.GLFW.glfwGetKey(window, org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL) == org.lwjgl.glfw.GLFW.GLFW_PRESS
                || org.lwjgl.glfw.GLFW.glfwGetKey(window, org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_CONTROL) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
    }

    private static boolean isAlt(int key) {
        return key == org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_ALT || key == org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_ALT;
    }

    private static boolean vmDisplayActive() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || activeComputerPos == null) {
            return false;
        }
        if (minecraft.level.getBlockEntity(activeComputerPos) instanceof com.example.vm.blockentity.ComputerBlockEntity computer) {
            return computer.vmDisplayActive();
        }
        return false;
    }

    private static String qcode(int key, int modifiers) {
        String base = switch (key) {
            case GLFW.GLFW_KEY_UP -> "up";
            case GLFW.GLFW_KEY_DOWN -> "down";
            case GLFW.GLFW_KEY_LEFT -> "left";
            case GLFW.GLFW_KEY_RIGHT -> "right";
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> "ret";
            case GLFW.GLFW_KEY_SPACE -> "spc";
            case GLFW.GLFW_KEY_ESCAPE -> "esc";
            case GLFW.GLFW_KEY_TAB -> "tab";
            case GLFW.GLFW_KEY_BACKSPACE -> "backspace";
            case GLFW.GLFW_KEY_COMMA -> "comma";
            case GLFW.GLFW_KEY_PERIOD -> "dot";
            case GLFW.GLFW_KEY_SLASH -> "slash";
            case GLFW.GLFW_KEY_BACKSLASH -> "backslash";
            case GLFW.GLFW_KEY_MINUS -> "minus";
            case GLFW.GLFW_KEY_EQUAL -> "equal";
            case GLFW.GLFW_KEY_SEMICOLON -> "semicolon";
            case GLFW.GLFW_KEY_APOSTROPHE -> "apostrophe";
            case GLFW.GLFW_KEY_LEFT_BRACKET -> "bracket_left";
            case GLFW.GLFW_KEY_RIGHT_BRACKET -> "bracket_right";
            case GLFW.GLFW_KEY_GRAVE_ACCENT -> "grave_accent";
            default -> printableQcode(key);
        };
        if (base == null || !isShiftDown(modifiers) || isModifierKey(key)) {
            return base;
        }
        return "shift-" + base;
    }

    private static String printableQcode(int key) {
        if (key >= GLFW.GLFW_KEY_A && key <= GLFW.GLFW_KEY_Z) {
            return String.valueOf((char) ('a' + key - GLFW.GLFW_KEY_A));
        }
        if (key >= GLFW.GLFW_KEY_0 && key <= GLFW.GLFW_KEY_9) {
            return String.valueOf((char) ('0' + key - GLFW.GLFW_KEY_0));
        }
        return null;
    }

    private static boolean isShiftDown(int modifiers) {
        return (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
    }

    private static boolean isModifierKey(int key) {
        return key == GLFW.GLFW_KEY_LEFT_SHIFT
                || key == GLFW.GLFW_KEY_RIGHT_SHIFT
                || key == GLFW.GLFW_KEY_LEFT_CONTROL
                || key == GLFW.GLFW_KEY_RIGHT_CONTROL
                || key == GLFW.GLFW_KEY_LEFT_ALT
                || key == GLFW.GLFW_KEY_RIGHT_ALT;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
