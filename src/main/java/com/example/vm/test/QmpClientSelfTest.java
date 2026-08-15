package com.example.vm.test;

import com.example.vm.qmp.QmpClient;
import com.example.vm.qmp.QmpCommand;
import com.example.vm.qmp.QmpEndpoint;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class QmpClientSelfTest {
    private QmpClientSelfTest() {
    }

    public static void main(String[] args) throws Exception {
        AtomicInteger acceptedConnections = new AtomicInteger();
        CountDownLatch finished = new CountDownLatch(1);

        try (ServerSocket serverSocket = new ServerSocket(0)) {
            Thread serverThread = new Thread(() -> runServer(serverSocket, acceptedConnections, finished), "qmp-client-self-test-server");
            serverThread.setDaemon(true);
            serverThread.start();

            QmpEndpoint endpoint = new QmpEndpoint("127.0.0.1", serverSocket.getLocalPort());
            try (QmpClient client = new QmpClient(Duration.ofSeconds(2))) {
                client.send(endpoint, QmpCommand.SYSTEM_RESET);
                client.send(endpoint, QmpCommand.SYSTEM_POWERDOWN);
            }

            if (!finished.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("Fake QMP server did not receive both commands");
            }
            if (acceptedConnections.get() != 1) {
                throw new AssertionError("QmpClient must reuse one connection, got " + acceptedConnections.get());
            }
        }
    }

    private static void runServer(ServerSocket serverSocket, AtomicInteger acceptedConnections, CountDownLatch finished) {
        try (Socket socket = serverSocket.accept()) {
            acceptedConnections.incrementAndGet();
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

            writeLine(writer, "{\"QMP\":{\"version\":{\"qemu\":{\"major\":8,\"minor\":0,\"micro\":0}},\"capabilities\":[]}}");
            requireContains(reader.readLine(), QmpCommand.QMP_CAPABILITIES);
            writeLine(writer, "{\"return\":{}}");
            requireContains(reader.readLine(), QmpCommand.SYSTEM_RESET);
            writeLine(writer, "{\"return\":{}}");
            requireContains(reader.readLine(), QmpCommand.SYSTEM_POWERDOWN);
            writeLine(writer, "{\"return\":{}}");
            finished.countDown();
        } catch (IOException exception) {
            throw new AssertionError("Fake QMP server failed", exception);
        }
    }

    private static void writeLine(BufferedWriter writer, String line) throws IOException {
        writer.write(line);
        writer.newLine();
        writer.flush();
    }

    private static void requireContains(String actual, String expected) {
        if (actual == null || !actual.contains(expected)) {
            throw new AssertionError("Expected " + actual + " to contain " + expected);
        }
    }
}
