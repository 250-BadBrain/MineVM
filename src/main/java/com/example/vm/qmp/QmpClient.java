package com.example.vm.qmp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public final class QmpClient {
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(3);
    private final Duration timeout;

    public QmpClient() {
        this(DEFAULT_TIMEOUT);
    }

    public QmpClient(Duration timeout) {
        if (timeout == null || timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
        this.timeout = timeout;
    }

    public void send(QmpEndpoint endpoint, String execute) throws IOException {
        sendJson(endpoint, QmpCommand.execute(execute));
    }

    public void sendJson(QmpEndpoint endpoint, String commandJson) throws IOException {
        if (commandJson == null || commandJson.isBlank()) {
            throw new IllegalArgumentException("commandJson must not be blank");
        }

        try (Socket socket = new Socket()) {
            int timeoutMillis = Math.toIntExact(timeout.toMillis());
            socket.connect(new InetSocketAddress(endpoint.host(), endpoint.port()), timeoutMillis);
            socket.setSoTimeout(timeoutMillis);

            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

            readRequiredLine(reader, "QMP greeting");
            writeLine(writer, QmpCommand.capabilities());
            readCommandResponse(reader, "QMP capabilities response");
            writeLine(writer, commandJson);
            readCommandResponse(reader, "QMP command response");
        }
    }

    private static void writeLine(BufferedWriter writer, String json) throws IOException {
        writer.write(json);
        writer.newLine();
        writer.flush();
    }

    private static void readRequiredLine(BufferedReader reader, String description) throws IOException {
        String line = reader.readLine();
        if (line == null || line.isBlank()) {
            throw new IOException("Missing " + description);
        }
    }

    private static void readCommandResponse(BufferedReader reader, String description) throws IOException {
        while (true) {
            String line = reader.readLine();
            if (line == null || line.isBlank()) {
                throw new IOException("Missing " + description);
            }
            if (line.contains("\"return\"")) {
                return;
            }
            if (line.contains("\"error\"")) {
                throw new IOException(description + " failed: " + line);
            }
        }
    }
}
