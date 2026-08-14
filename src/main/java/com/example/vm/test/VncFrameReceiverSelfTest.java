package com.example.vm.test;

import com.example.vm.screen.ScreenFrame;
import com.example.vm.screen.VncEndpoint;
import com.example.vm.screen.VncFrameReceiver;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class VncFrameReceiverSelfTest {
    private VncFrameReceiverSelfTest() {
    }

    public static void main(String[] args) throws Exception {
        try (ServerSocket serverSocket = openVncLikeServerSocket()) {
            CountDownLatch serverDone = new CountDownLatch(1);
            Thread serverThread = new Thread(() -> runSingleFrameServer(serverSocket, serverDone), "vnc-frame-receiver-self-test-server");
            serverThread.setDaemon(true);
            serverThread.start();

            List<ScreenFrame> frames = new ArrayList<>();
            VncFrameReceiver receiver = new VncFrameReceiver();
            try {
                receiver.receive(VncEndpoint.localPort(serverSocket.getLocalPort()), frames::add);
            } catch (java.io.EOFException expected) {
                // The fake server sends one frame and closes the socket to end the receive loop.
            }

            if (!serverDone.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("Fake VNC server did not complete");
            }
            if (frames.size() != 2) {
                throw new AssertionError("Expected two frames but got " + frames.size());
            }
            ScreenFrame frame = frames.get(0);
            if (frame.width() != 4 || frame.height() != 2 || frame.changedWidth() != 1 || frame.changedHeight() != 1) {
                throw new AssertionError("Unexpected frame dimensions: " + frame);
            }
            if (frame.argbPixels()[0] != 0xFF336699) {
                throw new AssertionError("Unexpected ARGB pixel: " + Integer.toHexString(frame.argbPixels()[0]));
            }
            ScreenFrame resizedFrame = frames.get(1);
            if (resizedFrame.width() != 4 || resizedFrame.height() != 2 || resizedFrame.changedX() != 3 || resizedFrame.changedWidth() != 1) {
                throw new AssertionError("Unexpected resized frame dimensions: " + resizedFrame);
            }
            if (resizedFrame.argbPixels()[0] != 0xFF112233) {
                throw new AssertionError("Unexpected resized ARGB pixel: " + Integer.toHexString(resizedFrame.argbPixels()[0]));
            }
        }
    }

    private static ServerSocket openVncLikeServerSocket() throws java.io.IOException {
        for (int port = 6200; port < 6300; port++) {
            try {
                return new ServerSocket(port);
            } catch (java.io.IOException ignored) {
            }
        }
        throw new java.io.IOException("No free VNC-like test port found");
    }

    private static void runSingleFrameServer(ServerSocket serverSocket, CountDownLatch done) {
        try (Socket socket = serverSocket.accept()) {
            DataInputStream input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            DataOutputStream output = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));

            output.write("RFB 003.008\n".getBytes(StandardCharsets.US_ASCII));
            output.flush();
            input.readNBytes(12);

            output.writeByte(1);
            output.writeByte(1);
            output.flush();
            input.readUnsignedByte();
            output.writeInt(0);
            output.flush();
            input.readUnsignedByte();

            output.writeShort(2);
            output.writeShort(1);
            output.write(new byte[16]);
            output.writeInt(4);
            output.write("test".getBytes(StandardCharsets.US_ASCII));
            output.flush();

            input.readNBytes(20);
            input.readNBytes(12);
            FramebufferUpdateRequest initialRequest = readFramebufferUpdateRequest(input);
            if (initialRequest.incremental()) {
                throw new AssertionError("Initial framebuffer update request must be full-frame");
            }

            output.writeByte(0);
            output.writeByte(0);
            output.writeShort(2);
            output.writeShort(0);
            output.writeShort(0);
            output.writeShort(4);
            output.writeShort(2);
            output.writeInt(-223);
            output.writeShort(0);
            output.writeShort(0);
            output.writeShort(1);
            output.writeShort(1);
            output.writeInt(0);
            output.write(new byte[] {(byte) 0x99, 0x66, 0x33, 0});
            output.flush();
            FramebufferUpdateRequest followupRequest = readFramebufferUpdateRequest(input);
            if (!followupRequest.incremental()) {
                throw new AssertionError("Follow-up framebuffer update request must be incremental");
            }

            output.writeByte(0);
            output.writeByte(0);
            output.writeShort(1);
            output.writeShort(3);
            output.writeShort(1);
            output.writeShort(1);
            output.writeShort(1);
            output.writeInt(0);
            output.write(new byte[] {0x33, 0x22, 0x11, 0});
            output.flush();
            done.countDown();
        } catch (Exception exception) {
            throw new AssertionError("Fake VNC server failed", exception);
        }
    }

    private static FramebufferUpdateRequest readFramebufferUpdateRequest(DataInputStream input) throws java.io.IOException {
        int messageType = input.readUnsignedByte();
        boolean incremental = input.readUnsignedByte() != 0;
        int x = input.readUnsignedShort();
        int y = input.readUnsignedShort();
        int width = input.readUnsignedShort();
        int height = input.readUnsignedShort();
        if (messageType != 3 || x != 0 || y != 0 || width <= 0 || height <= 0) {
            throw new AssertionError("Unexpected framebuffer update request");
        }
        return new FramebufferUpdateRequest(incremental);
    }

    private record FramebufferUpdateRequest(boolean incremental) {
    }
}
