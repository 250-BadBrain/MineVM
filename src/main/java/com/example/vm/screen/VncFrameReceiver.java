package com.example.vm.screen;

import com.example.vm.ComputerInMinecraft;
import com.example.vm.input.KeyboardInput;
import com.example.vm.input.MouseButtonInput;
import com.example.vm.input.MouseMoveInput;
import com.example.vm.input.MouseWheelInput;
import com.example.vm.input.VirtualMachineInput;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class VncFrameReceiver {
    private static final int RAW_ENCODING = 0;
    private static final int DESKTOP_SIZE_ENCODING = -223;
    private static final int BYTES_PER_PIXEL = 4;
    private static final int FRAMEBUFFER_UPDATE_REQUEST = 3;
    private static final int KEY_EVENT = 4;
    private static final int POINTER_EVENT = 5;
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(2);
    private static final long IDLE_INPUT_REPRIME_NANOS = Duration.ofSeconds(10).toNanos();

    private final Map<VncEndpoint, ActiveConnection> connections = new ConcurrentHashMap<>();

    public boolean sendInput(VncEndpoint endpoint, VirtualMachineInput input) {
        ActiveConnection connection = connections.get(endpoint);
        if (connection == null) {
            return false;
        }
        try {
            connection.send(input);
            return true;
        } catch (IOException | IllegalArgumentException exception) {
            connections.remove(endpoint, connection);
            connection.closeQuietly();
            ComputerInMinecraft.LOGGER.warn("VNC input failed for {}", endpoint.toDisplayAddress(), exception);
            return false;
        }
    }

    public boolean hasActiveConnection(VncEndpoint endpoint) {
        return connections.containsKey(endpoint);
    }

    public void receive(VncEndpoint endpoint, Consumer<ScreenFrame> frameConsumer) throws IOException {
        if (endpoint == null) {
            throw new IllegalArgumentException("endpoint must not be null");
        }
        if (frameConsumer == null) {
            throw new IllegalArgumentException("frameConsumer must not be null");
        }

        ActiveConnection connection = null;
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(endpoint.host(), endpoint.port()), (int) CONNECT_TIMEOUT.toMillis());
            socket.setSoTimeout((int) READ_TIMEOUT.toMillis());
            DataInputStream input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            DataOutputStream output = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));

            handshake(input, output);
            Framebuffer framebuffer = readServerInit(input);
            ComputerInMinecraft.LOGGER.info("Connected VNC framebuffer {} at {}x{}", endpoint.toDisplayAddress(), framebuffer.width(), framebuffer.height());
            connection = new ActiveConnection(socket, output, framebuffer.width(), framebuffer.height());
            connections.put(endpoint, connection);
            connection.setPixelFormat();
            connection.setEncodings();
            connection.requestFramebufferUpdate(false);

            while (!Thread.currentThread().isInterrupted()) {
                int messageType;
                try {
                    messageType = input.readUnsignedByte();
                } catch (SocketTimeoutException ignored) {
                    connection.requestFramebufferUpdate(true);
                    continue;
                }
                if (messageType == 0) {
                    framebuffer = readFramebufferUpdate(input, connection, framebuffer, endpoint, frameConsumer);
                }
            }
        } finally {
            if (connection != null) {
                connections.remove(endpoint, connection);
            }
        }
    }

    private static void handshake(DataInputStream input, DataOutputStream output) throws IOException {
        byte[] versionBytes = input.readNBytes(12);
        if (versionBytes.length != 12) {
            throw new EOFException("VNC server closed before protocol version");
        }
        String version = new String(versionBytes, StandardCharsets.US_ASCII);
        if (!version.startsWith("RFB ")) {
            throw new IOException("Unsupported VNC protocol header: " + version.trim());
        }

        output.write(versionBytes);
        output.flush();

        int securityTypeCount = input.readUnsignedByte();
        if (securityTypeCount == 0) {
            int reasonLength = input.readInt();
            String reason = new String(input.readNBytes(reasonLength), StandardCharsets.UTF_8);
            throw new IOException("VNC security negotiation failed: " + reason);
        }

        boolean supportsNone = false;
        for (int index = 0; index < securityTypeCount; index++) {
            if (input.readUnsignedByte() == 1) {
                supportsNone = true;
            }
        }
        if (!supportsNone) {
            throw new IOException("VNC server does not allow None security");
        }

        output.writeByte(1);
        output.flush();
        int securityResult = input.readInt();
        if (securityResult != 0) {
            throw new IOException("VNC None security rejected with code " + securityResult);
        }

        output.writeByte(1);
        output.flush();
    }

    private static Framebuffer readServerInit(DataInputStream input) throws IOException {
        int width = input.readUnsignedShort();
        int height = input.readUnsignedShort();
        input.skipNBytes(16);
        int nameLength = input.readInt();
        input.skipNBytes(nameLength);
        return new Framebuffer(width, height);
    }

    private static Framebuffer readFramebufferUpdate(DataInputStream input, ActiveConnection connection, Framebuffer framebuffer, VncEndpoint endpoint, Consumer<ScreenFrame> frameConsumer) throws IOException {
        input.readUnsignedByte();
        int rectangleCount = input.readUnsignedShort();
        Framebuffer currentFramebuffer = framebuffer;
        for (int rectangleIndex = 0; rectangleIndex < rectangleCount; rectangleIndex++) {
            int x = input.readUnsignedShort();
            int y = input.readUnsignedShort();
            int width = input.readUnsignedShort();
            int height = input.readUnsignedShort();
            int encoding = input.readInt();
            if (encoding == DESKTOP_SIZE_ENCODING) {
                currentFramebuffer = new Framebuffer(width, height);
                connection.setFramebuffer(width, height);
                ComputerInMinecraft.LOGGER.info("VNC framebuffer resized {} to {}x{}", endpoint.toDisplayAddress(), width, height);
                continue;
            }
            if (encoding != RAW_ENCODING) {
                throw new IOException("Unsupported VNC encoding: " + encoding);
            }
            int[] pixels = readRawPixels(input, width, height);
            if (x + width > currentFramebuffer.width() || y + height > currentFramebuffer.height()) {
                currentFramebuffer = new Framebuffer(Math.max(currentFramebuffer.width(), x + width), Math.max(currentFramebuffer.height(), y + height));
                connection.setFramebuffer(currentFramebuffer.width(), currentFramebuffer.height());
                ComputerInMinecraft.LOGGER.info("VNC framebuffer expanded {} to {}x{} from raw rectangle", endpoint.toDisplayAddress(), currentFramebuffer.width(), currentFramebuffer.height());
            }
            frameConsumer.accept(new ScreenFrame(currentFramebuffer.width(), currentFramebuffer.height(), x, y, width, height, pixels));
        }
        connection.requestFramebufferUpdate(true);
        return currentFramebuffer;
    }

    private static int[] readRawPixels(DataInputStream input, int width, int height) throws IOException {
        byte[] bytes = input.readNBytes(width * height * BYTES_PER_PIXEL);
        if (bytes.length != width * height * BYTES_PER_PIXEL) {
            throw new EOFException("VNC framebuffer rectangle ended early");
        }

        int[] pixels = new int[width * height];
        for (int index = 0; index < pixels.length; index++) {
            int offset = index * BYTES_PER_PIXEL;
            int blue = bytes[offset] & 0xFF;
            int green = bytes[offset + 1] & 0xFF;
            int red = bytes[offset + 2] & 0xFF;
            pixels[index] = 0xFF000000 | (red << 16) | (green << 8) | blue;
        }
        return pixels;
    }

    private record Framebuffer(int width, int height) {
    }

    private static final class ActiveConnection {
        private final Socket socket;
        private final DataOutputStream output;
        private int framebufferWidth;
        private int framebufferHeight;
        private int pointerX;
        private int pointerY;
        private int buttonMask;
        private long lastInputNanos;

        private ActiveConnection(Socket socket, DataOutputStream output, int framebufferWidth, int framebufferHeight) {
            this.socket = socket;
            this.output = output;
            setFramebuffer(framebufferWidth, framebufferHeight);
        }

        private synchronized void setFramebuffer(int width, int height) {
            framebufferWidth = Math.max(1, width);
            framebufferHeight = Math.max(1, height);
            pointerX = clamp(pointerX, 0, framebufferWidth - 1);
            pointerY = clamp(pointerY, 0, framebufferHeight - 1);
        }

        private synchronized void setPixelFormat() throws IOException {
            output.writeByte(0);
            output.write(new byte[] {0, 0, 0});
            output.writeByte(32);
            output.writeByte(24);
            output.writeByte(0);
            output.writeByte(1);
            output.writeShort(255);
            output.writeShort(255);
            output.writeShort(255);
            output.writeByte(16);
            output.writeByte(8);
            output.writeByte(0);
            output.write(new byte[] {0, 0, 0});
            output.flush();
        }

        private synchronized void setEncodings() throws IOException {
            output.writeByte(2);
            output.writeByte(0);
            output.writeShort(2);
            output.writeInt(RAW_ENCODING);
            output.writeInt(DESKTOP_SIZE_ENCODING);
            output.flush();
        }

        private synchronized void requestFramebufferUpdate(boolean incremental) throws IOException {
            output.writeByte(FRAMEBUFFER_UPDATE_REQUEST);
            output.writeByte(incremental ? 1 : 0);
            output.writeShort(0);
            output.writeShort(0);
            output.writeShort(framebufferWidth);
            output.writeShort(framebufferHeight);
            output.flush();
        }

        private synchronized void send(VirtualMachineInput input) throws IOException {
            boolean reprime = shouldReprimeInput(input);
            if (input instanceof KeyboardInput keyboardInput) {
                if (keyboardInput.pressed()) {
                    sendKey(keyboardInput.key());
                }
            } else if (input instanceof MouseMoveInput mouseMoveInput) {
                pointerX = scale(mouseMoveInput.x(), mouseMoveInput.screenWidth(), framebufferWidth);
                pointerY = scale(mouseMoveInput.y(), mouseMoveInput.screenHeight(), framebufferHeight);
                reprimePointerIfIdle(reprime);
                sendPointerEvent(buttonMask);
            } else if (input instanceof MouseButtonInput mouseButtonInput) {
                reprimePointerIfIdle(reprime);
                int mask = buttonMask(mouseButtonInput.button());
                buttonMask = mouseButtonInput.pressed() ? buttonMask | mask : buttonMask & ~mask;
                sendPointerEvent(buttonMask);
            } else if (input instanceof MouseWheelInput mouseWheelInput) {
                reprimePointerIfIdle(reprime);
                int mask = mouseWheelInput.steps() > 0 ? 8 : 16;
                int count = Math.abs(mouseWheelInput.steps());
                for (int index = 0; index < count; index++) {
                    sendPointerEvent(buttonMask | mask);
                    sendPointerEvent(buttonMask);
                }
            }
            lastInputNanos = System.nanoTime();
        }

        private boolean shouldReprimeInput(VirtualMachineInput input) {
            if (!(input instanceof MouseMoveInput || input instanceof MouseButtonInput || input instanceof MouseWheelInput)) {
                return false;
            }
            long now = System.nanoTime();
            return lastInputNanos > 0L && now - lastInputNanos >= IDLE_INPUT_REPRIME_NANOS;
        }

        private void reprimePointerIfIdle(boolean reprime) throws IOException {
            if (!reprime) {
                return;
            }
            if (buttonMask != 0) {
                buttonMask = 0;
            }
            sendPointerEvent(buttonMask);
            requestFramebufferUpdate(true);
        }

        private void sendKey(String qcode) throws IOException {
            KeySequence sequence = KeySequence.fromQcode(qcode);
            for (int modifier : sequence.modifiers()) {
                sendKeyEvent(true, modifier);
            }
            sendKeyEvent(true, sequence.keysym());
            sendKeyEvent(false, sequence.keysym());
            for (int index = sequence.modifiers().length - 1; index >= 0; index--) {
                sendKeyEvent(false, sequence.modifiers()[index]);
            }
        }

        private void sendKeyEvent(boolean down, int keysym) throws IOException {
            output.writeByte(KEY_EVENT);
            output.writeByte(down ? 1 : 0);
            output.writeShort(0);
            output.writeInt(keysym);
            output.flush();
        }

        private void sendPointerEvent(int mask) throws IOException {
            output.writeByte(POINTER_EVENT);
            output.writeByte(mask);
            output.writeShort(pointerX);
            output.writeShort(pointerY);
            output.flush();
        }

        private void closeQuietly() {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }

        private static int scale(int coordinate, int sourceSize, int targetSize) {
            if (sourceSize <= 1 || targetSize <= 1) {
                return 0;
            }
            int clamped = clamp(coordinate, 0, sourceSize - 1);
            return Math.round((float) clamped * (targetSize - 1) / (sourceSize - 1));
        }

        private static int buttonMask(int button) {
            return switch (button) {
                case 0 -> 1;
                case 1 -> 2;
                case 2 -> 4;
                default -> 0;
            };
        }

        private static int clamp(int value, int min, int max) {
            return Math.max(min, Math.min(max, value));
        }
    }

    private record KeySequence(int[] modifiers, int keysym) {
        private static final int SHIFT = 0xFFE1;
        private static final int CTRL = 0xFFE3;
        private static final int ALT = 0xFFE9;

        private static KeySequence fromQcode(String qcode) {
            String remaining = qcode.toLowerCase(Locale.ROOT);
            java.util.ArrayList<Integer> modifiers = new java.util.ArrayList<>();
            while (remaining.startsWith("ctrl-") || remaining.startsWith("alt-") || remaining.startsWith("shift-")) {
                if (remaining.startsWith("ctrl-")) {
                    modifiers.add(CTRL);
                    remaining = remaining.substring("ctrl-".length());
                } else if (remaining.startsWith("alt-")) {
                    modifiers.add(ALT);
                    remaining = remaining.substring("alt-".length());
                } else {
                    modifiers.add(SHIFT);
                    remaining = remaining.substring("shift-".length());
                }
            }
            int[] modifierArray = modifiers.stream().mapToInt(Integer::intValue).toArray();
            return new KeySequence(modifierArray, keysym(remaining));
        }

        private static int keysym(String qcode) {
            if (qcode.length() == 1) {
                return qcode.charAt(0);
            }
            return switch (qcode) {
                case "ret", "kp_enter" -> 0xFF0D;
                case "esc" -> 0xFF1B;
                case "tab" -> 0xFF09;
                case "backspace" -> 0xFF08;
                case "spc" -> 0x20;
                case "up" -> 0xFF52;
                case "down" -> 0xFF54;
                case "left" -> 0xFF51;
                case "right" -> 0xFF53;
                case "comma" -> ',';
                case "dot" -> '.';
                case "slash" -> '/';
                case "backslash" -> '\\';
                case "minus" -> '-';
                case "equal" -> '=';
                case "semicolon" -> ';';
                case "apostrophe" -> '\'';
                case "bracket_left" -> '[';
                case "bracket_right" -> ']';
                case "grave_accent" -> '`';
                default -> functionKey(qcode);
            };
        }

        private static int functionKey(String qcode) {
            if (qcode.startsWith("f")) {
                try {
                    int index = Integer.parseInt(qcode.substring(1));
                    if (index >= 1 && index <= 12) {
                        return 0xFFBE + index - 1;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
            throw new IllegalArgumentException("Unsupported VNC qcode: " + qcode);
        }
    }
}
