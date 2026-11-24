package com.example.vm.test;

import com.example.vm.screen.ScreenFrame;
import com.example.vm.screen.VncEndpoint;
import com.example.vm.screen.VncFrameReceiver;
import com.example.vm.input.KeyboardInput;
import com.example.vm.input.MouseMoveInput;

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
import java.util.concurrent.atomic.AtomicReference;

public final class VncFrameReceiverSelfTest {
    private VncFrameReceiverSelfTest() {
    }

    public static void main(String[] args) throws Exception {
        assertFrameAndInputFlow();
        assertIdleReadTimeoutKeepsConnectionAlive();
        assertCloseReleasesActiveConnection();
    }

    private static void assertFrameAndInputFlow() throws Exception {
        try (ServerSocket serverSocket = openVncLikeServerSocket()) {
            CountDownLatch inputReady = new CountDownLatch(1);
            CountDownLatch serverDone = new CountDownLatch(1);
            Thread serverThread = new Thread(() -> runSingleFrameServer(serverSocket, inputReady, serverDone), "vnc-frame-receiver-self-test-server");
            serverThread.setDaemon(true);
            serverThread.start();

            List<ScreenFrame> frames = new ArrayList<>();
            VncFrameReceiver receiver = new VncFrameReceiver();
            VncEndpoint endpoint = VncEndpoint.localPort(serverSocket.getLocalPort());
            AtomicReference<Throwable> receiverFailure = new AtomicReference<>();
            Thread receiverThread = new Thread(() -> {
                try {
                    receiver.receive(endpoint, frames::add);
                } catch (java.io.EOFException expected) {
                    // The fake server sends two frames and closes the socket to end the receive loop.
                } catch (java.net.SocketException expected) {
                    // Windows may report the fake server closing the socket as a connection abort.
                } catch (Throwable throwable) {
                    receiverFailure.set(throwable);
                }
            }, "vnc-frame-receiver-self-test-client");
            receiverThread.setDaemon(true);
            receiverThread.start();

            if (!inputReady.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("Fake VNC server did not become ready for input");
            }
            if (!receiver.sendInput(endpoint, new MouseMoveInput("computer-test", 100, 200, 1280, 800))) {
                throw new AssertionError("Expected VNC mouse input to be sent");
            }
            if (!receiver.sendInput(endpoint, new KeyboardInput("computer-test", "ctrl-w", true))) {
                throw new AssertionError("Expected VNC keyboard input to be sent");
            }

            if (!serverDone.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("Fake VNC server did not complete");
            }
            receiverThread.join(5000L);
            if (receiverFailure.get() != null) {
                throw new AssertionError("VNC receiver failed", receiverFailure.get());
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

    private static void assertIdleReadTimeoutKeepsConnectionAlive() throws Exception {
        try (ServerSocket serverSocket = openVncLikeServerSocket()) {
            CountDownLatch inputReady = new CountDownLatch(1);
            CountDownLatch serverDone = new CountDownLatch(1);
            Thread serverThread = new Thread(() -> runIdleFrameServer(serverSocket, inputReady, serverDone), "vnc-frame-receiver-idle-self-test-server");
            serverThread.setDaemon(true);
            serverThread.start();

            List<ScreenFrame> frames = new ArrayList<>();
            VncFrameReceiver receiver = new VncFrameReceiver();
            VncEndpoint endpoint = VncEndpoint.localPort(serverSocket.getLocalPort());
            AtomicReference<Throwable> receiverFailure = new AtomicReference<>();
            Thread receiverThread = new Thread(() -> {
                try {
                    receiver.receive(endpoint, frames::add);
                } catch (java.io.EOFException expected) {
                    // The fake server closes the socket after proving an idle timeout did not disconnect the receiver.
                } catch (java.net.SocketException expected) {
                    // Windows may report the fake server closing the socket as a connection abort.
                } catch (Throwable throwable) {
                    receiverFailure.set(throwable);
                }
            }, "vnc-frame-receiver-idle-self-test-client");
            receiverThread.setDaemon(true);
            receiverThread.start();

            if (!inputReady.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("Idle fake VNC server did not become ready");
            }
            if (!receiver.hasActiveConnection(endpoint)) {
                throw new AssertionError("Expected VNC connection to stay active before idle timeout");
            }
            if (!serverDone.await(8, TimeUnit.SECONDS)) {
                throw new AssertionError("Idle fake VNC server did not complete");
            }
            receiverThread.join(5000L);
            if (receiverFailure.get() != null) {
                throw new AssertionError("VNC receiver failed during idle timeout", receiverFailure.get());
            }
            if (frames.size() != 1) {
                throw new AssertionError("Expected one frame after idle timeout but got " + frames.size());
            }
            if (frames.get(0).argbPixels()[0] != 0xFF040302) {
                throw new AssertionError("Unexpected idle frame ARGB pixel: " + Integer.toHexString(frames.get(0).argbPixels()[0]));
            }
        }
    }

    private static void assertCloseReleasesActiveConnection() throws Exception {
        try (ServerSocket serverSocket = openVncLikeServerSocket()) {
            CountDownLatch inputReady = new CountDownLatch(1);
            Thread serverThread = new Thread(() -> runCloseableServer(serverSocket, inputReady), "vnc-frame-receiver-close-self-test-server");
            serverThread.setDaemon(true);
            serverThread.start();

            VncFrameReceiver receiver = new VncFrameReceiver();
            VncEndpoint endpoint = VncEndpoint.localPort(serverSocket.getLocalPort());
            Thread receiverThread = new Thread(() -> {
                try {
                    receiver.receive(endpoint, ignored -> { });
                } catch (java.io.IOException expected) {
                    // Closing the active connection intentionally interrupts the blocking receiver.
                }
            }, "vnc-frame-receiver-close-self-test-client");
            receiverThread.setDaemon(true);
            receiverThread.start();

            if (!inputReady.await(5, TimeUnit.SECONDS) || !receiver.hasActiveConnection(endpoint)) {
                throw new AssertionError("Expected close test to establish a VNC connection");
            }
            receiver.close(endpoint);
            receiverThread.join(5000L);
            if (receiverThread.isAlive() || receiver.hasActiveConnection(endpoint)) {
                throw new AssertionError("VNC close must release the active connection");
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

    private static void runSingleFrameServer(ServerSocket serverSocket, CountDownLatch inputReady, CountDownLatch done) {
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
            inputReady.countDown();
            assertPointerEvent(input);
            assertCtrlWKeyEvents(input);

            // Non-frame messages may arrive between framebuffer updates.
            output.writeByte(2);
            output.writeByte(1);
            output.writeByte(0);
            output.writeShort(0);
            output.writeShort(1);
            output.write(new byte[6]);
            output.writeByte(3);
            output.write(new byte[3]);
            output.writeInt(4);
            output.write("text".getBytes(StandardCharsets.US_ASCII));

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

    private static void runIdleFrameServer(ServerSocket serverSocket, CountDownLatch inputReady, CountDownLatch done) {
        try (Socket socket = serverSocket.accept()) {
            DataInputStream input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            DataOutputStream output = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));

            performHandshake(input, output, 1, 1);
            readClientStartupMessages(input);
            FramebufferUpdateRequest initialRequest = readFramebufferUpdateRequest(input);
            if (initialRequest.incremental()) {
                throw new AssertionError("Idle initial framebuffer update request must be full-frame");
            }
            inputReady.countDown();

            Thread.sleep(2500L);
            FramebufferUpdateRequest keepAliveRequest = readFramebufferUpdateRequest(input);
            if (!keepAliveRequest.incremental()) {
                throw new AssertionError("Idle timeout framebuffer update request must be incremental");
            }

            output.writeByte(0);
            output.writeByte(0);
            output.writeShort(1);
            output.writeShort(0);
            output.writeShort(0);
            output.writeShort(1);
            output.writeShort(1);
            output.writeInt(0);
            output.write(new byte[] {0x02, 0x03, 0x04, 0});
            output.flush();
            done.countDown();
        } catch (Exception exception) {
            throw new AssertionError("Idle fake VNC server failed", exception);
        }
    }

    private static void runCloseableServer(ServerSocket serverSocket, CountDownLatch inputReady) {
        try (Socket socket = serverSocket.accept()) {
            DataInputStream input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            DataOutputStream output = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            performHandshake(input, output, 1, 1);
            readClientStartupMessages(input);
            readFramebufferUpdateRequest(input);
            inputReady.countDown();
            while (input.read() != -1) {
                // Hold the server side open until the receiver closes its socket.
            }
        } catch (java.io.IOException expected) {
            // The client-side close ends this intentionally idle connection.
        }
    }

    private static void performHandshake(DataInputStream input, DataOutputStream output, int width, int height) throws java.io.IOException {
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

        output.writeShort(width);
        output.writeShort(height);
        output.write(new byte[16]);
        output.writeInt(4);
        output.write("test".getBytes(StandardCharsets.US_ASCII));
        output.flush();
    }

    private static void readClientStartupMessages(DataInputStream input) throws java.io.IOException {
        input.readNBytes(20);
        input.readNBytes(12);
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

    private static void assertPointerEvent(DataInputStream input) throws java.io.IOException {
        int messageType = input.readUnsignedByte();
        int buttonMask = input.readUnsignedByte();
        int x = input.readUnsignedShort();
        int y = input.readUnsignedShort();
        if (messageType != 5 || buttonMask != 0 || x < 0 || y < 0) {
            throw new AssertionError("Unexpected VNC pointer event");
        }
    }

    private static void assertCtrlWKeyEvents(DataInputStream input) throws java.io.IOException {
        assertKeyEvent(input, true, 0xFFE3);
        assertKeyEvent(input, true, 'w');
        assertKeyEvent(input, false, 'w');
        assertKeyEvent(input, false, 0xFFE3);
    }

    private static void assertKeyEvent(DataInputStream input, boolean down, int keysym) throws java.io.IOException {
        int messageType = input.readUnsignedByte();
        boolean actualDown = input.readUnsignedByte() != 0;
        input.readUnsignedShort();
        int actualKeysym = input.readInt();
        if (messageType != 4 || actualDown != down || actualKeysym != keysym) {
            throw new AssertionError("Unexpected VNC key event");
        }
    }
}
