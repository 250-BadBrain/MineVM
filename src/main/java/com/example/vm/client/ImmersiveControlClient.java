package com.example.vm.client;

import com.example.vm.MineVM;
import com.example.vm.computer.ComputerBiosAction;
import com.example.vm.network.StartScreenControlPacket;
import com.example.vm.network.ModNetwork;
import com.example.vm.registry.ModBlocks;
import com.example.vm.screen.ScreenControlMode;
import com.example.vm.screen.ScreenControlSession;
import com.example.vm.screen.ScreenFrame;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;
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
    private static final float DESKTOP_SCREEN_MAX_WIDTH = 258.0F;
    private static final float DESKTOP_SCREEN_MAX_HEIGHT = 128.0F;
    private static final float DESKTOP_SCREEN_Y = 1.22F;
    private static final float DESKTOP_SCREEN_Z_OFFSET = -0.283F;
    private static final float DESKTOP_SCREEN_SCALE = 0.0112F;
    private static final float LAPTOP_SCREEN_MAX_WIDTH = 287.0F;
    private static final float LAPTOP_SCREEN_MAX_HEIGHT = 156.0F;
    private static final float LAPTOP_SCREEN_Y = 1.18F;
    private static final float LAPTOP_SCREEN_Z_OFFSET = 1.158F;
    private static final float LAPTOP_SCREEN_SCALE = 0.0100F;
    private static final long VM_MOUSE_MOVE_INTERVAL_NANOS = 33_000_000L;
    private static final int[] VM_MOUSE_BUTTONS = {
            GLFW.GLFW_MOUSE_BUTTON_LEFT,
            GLFW.GLFW_MOUSE_BUTTON_MIDDLE,
            GLFW.GLFW_MOUSE_BUTTON_RIGHT
    };
    private static ScreenControlSession activeSession;
    private static BlockPos activeComputerPos;
    private static boolean activeLaptop;
    private static String activeComputerId = "";
    private static int selectedActionIndex;
    private static boolean vmInputMode;
    private static int capturedHotbarSlot = -1;
    private static boolean capturedHideGui;
    private static int lastMouseX = -1;
    private static int lastMouseY = -1;
    private static int lastSentMouseX = -1;
    private static int lastSentMouseY = -1;
    private static long lastMouseMoveSentNanos;
    private static long lastLeaseRenewal;

    public static void renewControlLease() {
        if (active() && System.currentTimeMillis() - lastLeaseRenewal >= 1000) {
            lastLeaseRenewal = System.currentTimeMillis();
            ModNetwork.CHANNEL.sendToServer(new com.example.vm.network.ControlSessionPacket(activeComputerId, false));
        }
    }

    private ImmersiveControlClient() {
    }

    public static void start(StartScreenControlPacket packet) {
        String playerId = Minecraft.getInstance().player == null ? "client" : Minecraft.getInstance().player.getStringUUID();
        activeSession = new ScreenControlSession(playerId, packet.screenPos(), ScreenControlMode.IMMERSIVE_CAPTURE);
        activeComputerPos = packet.computerPos();
        activeLaptop = packet.laptop();
        activeComputerId = packet.computerId();
        selectedActionIndex = 0;
        vmInputMode = packet.vmDisplayActive();
        capturedHotbarSlot = Minecraft.getInstance().player == null ? 0 : Minecraft.getInstance().player.getInventory().selected;
        capturedHideGui = Minecraft.getInstance().options.hideGui;
        Minecraft.getInstance().options.hideGui = true;
        lastMouseX = -1;
        lastMouseY = -1;
        lastSentMouseX = -1;
        lastSentMouseY = -1;
        lastMouseMoveSentNanos = 0L;
        releaseMouseForScreenControl();
        suppressMinecraftBindings();
        hideCursorForScreenControl();
        syncVmMousePosition(true);
        releaseVmInputState();
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.displayClientMessage(
                    Component.translatable("message.minevm.control_started", activeComputerId),
                    true
            );
        }
    }

    public static void stop() {
        boolean wasActive = active();
        if (wasActive) {
            suppressMinecraftBindings();
            releaseVmInputState();
            if (Minecraft.getInstance().getConnection() != null)
                ModNetwork.CHANNEL.sendToServer(new com.example.vm.network.ControlSessionPacket(activeComputerId, true));
        }
        activeSession = null;
        activeComputerPos = null;
        activeLaptop = false;
        activeComputerId = "";
        selectedActionIndex = 0;
        vmInputMode = false;
        capturedHotbarSlot = -1;
        lastMouseX = -1;
        lastMouseY = -1;
        lastSentMouseX = -1;
        lastSentMouseY = -1;
        lastMouseMoveSentNanos = 0L;
        if (wasActive) {
            Minecraft.getInstance().options.hideGui = capturedHideGui;
        }
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
            sendKeyboardInput("ret", true);
            suppressMinecraftBindings();
            return;
        }
        if (key == GLFW.GLFW_KEY_KP_ENTER) {
            sendKeyboardInput("kp_enter", true);
            suppressMinecraftBindings();
            return;
        }
        String qcode = qcode(key, modifiers);
        if (qcode != null) {
            sendKeyboardInput(qcode, pressed);
        }
        suppressMinecraftBindings();
    }

    private static void releaseVmInputState() {
        if (!controllingVmDisplay() || activeComputerId.isBlank()) {
            return;
        }
        for (int button : VM_MOUSE_BUTTONS) {
            int vmButton = switch (button) {
                case GLFW.GLFW_MOUSE_BUTTON_LEFT -> 0;
                case GLFW.GLFW_MOUSE_BUTTON_MIDDLE -> 1;
                case GLFW.GLFW_MOUSE_BUTTON_RIGHT -> 2;
                default -> button;
            };
            sendMouseButtonInput(vmButton, false);
        }
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
        sendMouseButtonInput(vmButton, pressed);
        suppressMinecraftBindings();
    }

    public static void handleVmMouseScroll(double scrollDelta) {
        if (!active() || activeComputerId.isBlank() || scrollDelta == 0.0D) {
            return;
        }
        syncVmMousePosition(true);
        int steps = (int) Math.max(-8, Math.min(8, Math.round(scrollDelta)));
        if (steps == 0) {
            steps = scrollDelta > 0.0D ? 1 : -1;
        }
        if (activeSession != null) {
            ModNetwork.sendMouseWheelInput(activeSession.screenPos(), activeComputerId, steps);
        }
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
        VmPointerPosition pointer = projectedVmPointerPosition(minecraft);
        int x = pointer.x();
        int y = pointer.y();
        lastMouseX = x;
        lastMouseY = y;
        if (!force && x == lastSentMouseX && y == lastSentMouseY) {
            return;
        }
        long now = System.nanoTime();
        if (!force && lastMouseMoveSentNanos > 0L && now - lastMouseMoveSentNanos < VM_MOUSE_MOVE_INTERVAL_NANOS) {
            return;
        }
        lastSentMouseX = x;
        lastSentMouseY = y;
        lastMouseMoveSentNanos = now;
        if (activeSession != null) {
            ModNetwork.sendMouseMoveInput(activeSession.screenPos(), activeComputerId, x, y, VM_POINTER_WIDTH, VM_POINTER_HEIGHT);
        }
        suppressMinecraftBindings();
    }

    private static void sendKeyboardInput(String key, boolean pressed) {
        if (activeSession != null) {
            ModNetwork.sendKeyboardInput(activeSession.screenPos(), activeComputerId, key, pressed);
        }
    }

    private static void sendMouseButtonInput(int button, boolean pressed) {
        if (activeSession != null) {
            ModNetwork.sendMouseButtonInput(activeSession.screenPos(), activeComputerId, button, pressed);
        }
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

    private static VmPointerPosition projectedVmPointerPosition(Minecraft minecraft) {
        ScreenMapping mapping = projectedScreenMapping(minecraft);
        if (mapping == null) {
            double windowWidth = Math.max(1, minecraft.getWindow().getScreenWidth());
            double windowHeight = Math.max(1, minecraft.getWindow().getScreenHeight());
            int x = clamp((int) Math.floor(minecraft.mouseHandler.xpos() * VM_POINTER_WIDTH / windowWidth), 0, VM_POINTER_WIDTH - 1);
            int y = clamp((int) Math.floor(minecraft.mouseHandler.ypos() * VM_POINTER_HEIGHT / windowHeight), 0, VM_POINTER_HEIGHT - 1);
            return new VmPointerPosition(x, y);
        }

        double[] mapped = mapping.map(minecraft.mouseHandler.xpos(), minecraft.mouseHandler.ypos());
        double normalizedX = mapped[0];
        double normalizedY = mapped[1];
        int x = clamp((int) Math.floor(normalizedX * VM_POINTER_WIDTH), 0, VM_POINTER_WIDTH - 1);
        int y = clamp((int) Math.floor(normalizedY * VM_POINTER_HEIGHT), 0, VM_POINTER_HEIGHT - 1);
        return new VmPointerPosition(x, y);
    }

    private static ScreenMapping projectedScreenMapping(Minecraft minecraft) {
        if (minecraft.level == null || activeSession == null) {
            return null;
        }
        BlockState state = minecraft.level.getBlockState(activeSession.screenPos());
        if (!state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return null;
        }

        boolean laptop = activeLaptop || state.getBlock() == ModBlocks.LAPTOP_COMPUTER.get();
        float maxWidth = laptop ? LAPTOP_SCREEN_MAX_WIDTH : DESKTOP_SCREEN_MAX_WIDTH;
        float maxHeight = laptop ? LAPTOP_SCREEN_MAX_HEIGHT : DESKTOP_SCREEN_MAX_HEIGHT;
        float y = laptop ? LAPTOP_SCREEN_Y : DESKTOP_SCREEN_Y;
        float zOffset = laptop ? LAPTOP_SCREEN_Z_OFFSET : DESKTOP_SCREEN_Z_OFFSET;
        float scale = laptop ? LAPTOP_SCREEN_SCALE : DESKTOP_SCREEN_SCALE;
        ScreenFrame frame = activeSession == null ? null : ClientScreenFrames.snapshot(activeComputerId, activeSession.screenPos()).orElse(null);
        int sourceWidth = frame == null ? VM_POINTER_WIDTH : frame.width();
        int sourceHeight = frame == null ? VM_POINTER_HEIGHT : frame.height();
        float[] fitted = ScreenSurfaceRenderer.fitToScreen(sourceWidth, sourceHeight, maxWidth, maxHeight);

        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        ProjectedPoint topLeft = projectDisplayPoint(minecraft, activeSession.screenPos(), facing, y, zOffset, scale, -fitted[0] / 2.0F, -fitted[1] / 2.0F);
        ProjectedPoint topRight = projectDisplayPoint(minecraft, activeSession.screenPos(), facing, y, zOffset, scale, fitted[0] / 2.0F, -fitted[1] / 2.0F);
        ProjectedPoint bottomLeft = projectDisplayPoint(minecraft, activeSession.screenPos(), facing, y, zOffset, scale, -fitted[0] / 2.0F, fitted[1] / 2.0F);
        ProjectedPoint bottomRight = projectDisplayPoint(minecraft, activeSession.screenPos(), facing, y, zOffset, scale, fitted[0] / 2.0F, fitted[1] / 2.0F);
        if (topLeft == null || topRight == null || bottomLeft == null || bottomRight == null) {
            return null;
        }

        double left = Math.min(Math.min(topLeft.x(), topRight.x()), Math.min(bottomLeft.x(), bottomRight.x()));
        double right = Math.max(Math.max(topLeft.x(), topRight.x()), Math.max(bottomLeft.x(), bottomRight.x()));
        double top = Math.min(Math.min(topLeft.y(), topRight.y()), Math.min(bottomLeft.y(), bottomRight.y()));
        double bottom = Math.max(Math.max(topLeft.y(), topRight.y()), Math.max(bottomLeft.y(), bottomRight.y()));
        if (right - left < 4.0D || bottom - top < 4.0D) {
            return null;
        }
        double[] homography = solveHomography(
                topLeft.x(), topLeft.y(), 0.0D, 0.0D,
                topRight.x(), topRight.y(), 1.0D, 0.0D,
                bottomRight.x(), bottomRight.y(), 1.0D, 1.0D,
                bottomLeft.x(), bottomLeft.y(), 0.0D, 1.0D
        );
        return homography == null ? null : new ScreenMapping(homography);
    }

    private static ProjectedPoint projectDisplayPoint(Minecraft minecraft, BlockPos pos, Direction facing, float y, float zOffset, float scale, float displayX, float displayY) {
        Vec3 world = displayPointToWorld(pos, facing, y, zOffset, scale, displayX, displayY);
        Camera camera = minecraft.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.getPosition();
        Vector4f point = new Vector4f(
                (float) (world.x - cameraPos.x),
                (float) (world.y - cameraPos.y),
                (float) (world.z - cameraPos.z),
                1.0F
        );
        Matrix4f modelView = new Matrix4f(RenderSystem.getModelViewMatrix());
        Matrix4f projection = new Matrix4f(RenderSystem.getProjectionMatrix());
        point.mul(modelView);
        point.mul(projection);
        if (point.w() <= 0.0001F) {
            return null;
        }
        float ndcX = point.x() / point.w();
        float ndcY = point.y() / point.w();
        if (!Float.isFinite(ndcX) || !Float.isFinite(ndcY)) {
            return null;
        }
        double width = Math.max(1, minecraft.getWindow().getScreenWidth());
        double height = Math.max(1, minecraft.getWindow().getScreenHeight());
        double screenX = (ndcX * 0.5D + 0.5D) * width;
        double screenY = (0.5D - ndcY * 0.5D) * height;
        return new ProjectedPoint(screenX, screenY);
    }

    private static Vec3 displayPointToWorld(BlockPos pos, Direction facing, float y, float zOffset, float scale, float displayX, float displayY) {
        double localX = -displayX * scale;
        double localY = -displayY * scale;
        double localZ = zOffset;
        double rotatedX = switch (facing) {
            case SOUTH -> -localX;
            case WEST -> localZ;
            case EAST -> -localZ;
            default -> localX;
        };
        double rotatedZ = switch (facing) {
            case SOUTH -> -localZ;
            case WEST -> -localX;
            case EAST -> localX;
            default -> localZ;
        };
        return new Vec3(pos.getX() + 0.5D + rotatedX, pos.getY() + y + localY, pos.getZ() + 0.5D + rotatedZ);
    }

    private static double[] solveHomography(
            double x0, double y0, double u0, double v0,
            double x1, double y1, double u1, double v1,
            double x2, double y2, double u2, double v2,
            double x3, double y3, double u3, double v3
    ) {
        double[][] matrix = new double[8][9];
        fillHomographyRows(matrix, 0, x0, y0, u0, v0);
        fillHomographyRows(matrix, 2, x1, y1, u1, v1);
        fillHomographyRows(matrix, 4, x2, y2, u2, v2);
        fillHomographyRows(matrix, 6, x3, y3, u3, v3);
        if (!gaussianEliminate(matrix)) {
            return null;
        }
        double[] homography = new double[9];
        for (int row = 0; row < 8; row++) {
            homography[row] = matrix[row][8];
        }
        homography[8] = 1.0D;
        return homography;
    }

    private static void fillHomographyRows(double[][] matrix, int row, double x, double y, double u, double v) {
        matrix[row][0] = x;
        matrix[row][1] = y;
        matrix[row][2] = 1.0D;
        matrix[row][6] = -u * x;
        matrix[row][7] = -u * y;
        matrix[row][8] = u;

        matrix[row + 1][3] = x;
        matrix[row + 1][4] = y;
        matrix[row + 1][5] = 1.0D;
        matrix[row + 1][6] = -v * x;
        matrix[row + 1][7] = -v * y;
        matrix[row + 1][8] = v;
    }

    private static boolean gaussianEliminate(double[][] matrix) {
        for (int column = 0; column < 8; column++) {
            int pivot = column;
            for (int row = column + 1; row < 8; row++) {
                if (Math.abs(matrix[row][column]) > Math.abs(matrix[pivot][column])) {
                    pivot = row;
                }
            }
            if (Math.abs(matrix[pivot][column]) < 1.0E-9D) {
                return false;
            }
            if (pivot != column) {
                double[] swap = matrix[column];
                matrix[column] = matrix[pivot];
                matrix[pivot] = swap;
            }
            double divisor = matrix[column][column];
            for (int col = column; col < 9; col++) {
                matrix[column][col] /= divisor;
            }
            for (int row = 0; row < 8; row++) {
                if (row == column) {
                    continue;
                }
                double factor = matrix[row][column];
                for (int col = column; col < 9; col++) {
                    matrix[row][col] -= factor * matrix[column][col];
                }
            }
        }
        return true;
    }

    static String qcode(int key, int modifiers) {
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
        if (base == null || isModifierKey(key)) {
            return base;
        }
        StringBuilder qcode = new StringBuilder();
        appendModifier(qcode, isControlDown(modifiers), "ctrl");
        appendModifier(qcode, isAltDown(modifiers), "alt");
        appendModifier(qcode, isShiftDown(modifiers), "shift");
        qcode.append(base);
        return qcode.toString();
    }

    private static String printableQcode(int key) {
        if (key >= GLFW.GLFW_KEY_A && key <= GLFW.GLFW_KEY_Z) {
            return String.valueOf((char) ('a' + key - GLFW.GLFW_KEY_A));
        }
        if (key >= GLFW.GLFW_KEY_0 && key <= GLFW.GLFW_KEY_9) {
            return String.valueOf((char) ('0' + key - GLFW.GLFW_KEY_0));
        }
        if (key >= GLFW.GLFW_KEY_F1 && key <= GLFW.GLFW_KEY_F12) {
            return "f" + (key - GLFW.GLFW_KEY_F1 + 1);
        }
        return null;
    }

    private static void appendModifier(StringBuilder qcode, boolean active, String modifier) {
        if (active) {
            qcode.append(modifier).append('-');
        }
    }

    private static boolean isShiftDown(int modifiers) {
        return (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
    }

    private static boolean isControlDown(int modifiers) {
        return (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
    }

    private static boolean isAltDown(int modifiers) {
        return (modifiers & GLFW.GLFW_MOD_ALT) != 0;
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

    private record VmPointerPosition(int x, int y) {
    }

    private record ScreenMapping(double[] homography) {
        double[] map(double x, double y) {
            double denominator = homography[6] * x + homography[7] * y + homography[8];
            if (Math.abs(denominator) < 1.0E-9D) {
                return new double[] {0.5D, 0.5D};
            }
            double u = (homography[0] * x + homography[1] * y + homography[2]) / denominator;
            double v = (homography[3] * x + homography[4] * y + homography[5]) / denominator;
            return new double[] {Math.max(0.0D, Math.min(1.0D, u)), Math.max(0.0D, Math.min(1.0D, v))};
        }
    }

    private record ProjectedPoint(double x, double y) {
    }
}
