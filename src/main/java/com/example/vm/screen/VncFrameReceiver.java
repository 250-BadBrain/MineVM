package com.example.vm.screen;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.function.Consumer;
import com.example.vm.ComputerInMinecraft;

public final class VncFrameReceiver {
    private static final int RAW_ENCODING = 0;
    private static final int DESKTOP_SIZE_ENCODING = -223;
    private static final int BYTES_PER_PIXEL = 4;
    private static final int FRAMEBUFFER_UPDATE_REQUEST = 3;
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(2);

    public void receive(VncEndpoint endpoint, Consumer<ScreenFrame> frameConsumer) throws IOException {
        if (endpoint == null) {
            throw new IllegalArgumentException("endpoint must not be null");
        }
        if (frameConsumer == null) {
            throw new IllegalArgumentException("frameConsumer must not be null");
        }

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(endpoint.host(), endpoint.port()), (int) CONNECT_TIMEOUT.toMillis());
            socket.setSoTimeout((int) READ_TIMEOUT.toMillis());
            DataInputStream input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            DataOutputStream output = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));

            handshake(input, output);
            Framebuffer framebuffer = readServerInit(input);
            ComputerInMinecraft.LOGGER.info("Connected VNC framebuffer {} at {}x{}", endpoint.toDisplayAddress(), framebuffer.width(), framebuffer.height());
            setPixelFormat(output);
            setEncodings(output);
            requestFramebufferUpdate(output, framebuffer.width(), framebuffer.height(), false);

            while (!Thread.currentThread().isInterrupted()) {
                int messageType = input.readUnsignedByte();
                if (messageType == 0) {
                    framebuffer = readFramebufferUpdate(input, output, framebuffer, endpoint, frameConsumer);
                }
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

    private static void setPixelFormat(DataOutputStream output) throws IOException {
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

    private static void setEncodings(DataOutputStream output) throws IOException {
        output.writeByte(2);
        output.writeByte(0);
        output.writeShort(2);
        output.writeInt(RAW_ENCODING);
        output.writeInt(DESKTOP_SIZE_ENCODING);
        output.flush();
    }

    private static void requestFramebufferUpdate(DataOutputStream output, int width, int height, boolean incremental) throws IOException {
        output.writeByte(FRAMEBUFFER_UPDATE_REQUEST);
        output.writeByte(incremental ? 1 : 0);
        output.writeShort(0);
        output.writeShort(0);
        output.writeShort(width);
        output.writeShort(height);
        output.flush();
    }

    private static Framebuffer readFramebufferUpdate(DataInputStream input, DataOutputStream output, Framebuffer framebuffer, VncEndpoint endpoint, Consumer<ScreenFrame> frameConsumer) throws IOException {
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
                ComputerInMinecraft.LOGGER.info("VNC framebuffer resized {} to {}x{}", endpoint.toDisplayAddress(), width, height);
                continue;
            }
            if (encoding != RAW_ENCODING) {
                throw new IOException("Unsupported VNC encoding: " + encoding);
            }
            int[] pixels = readRawPixels(input, width, height);
            if (x + width > currentFramebuffer.width() || y + height > currentFramebuffer.height()) {
                currentFramebuffer = new Framebuffer(Math.max(currentFramebuffer.width(), x + width), Math.max(currentFramebuffer.height(), y + height));
                ComputerInMinecraft.LOGGER.info("VNC framebuffer expanded {} to {}x{} from raw rectangle", endpoint.toDisplayAddress(), currentFramebuffer.width(), currentFramebuffer.height());
            }
            frameConsumer.accept(new ScreenFrame(currentFramebuffer.width(), currentFramebuffer.height(), x, y, width, height, pixels));
        }
        requestFramebufferUpdate(output, currentFramebuffer.width(), currentFramebuffer.height(), false);
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
}
