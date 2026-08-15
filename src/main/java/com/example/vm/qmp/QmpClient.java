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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class QmpClient implements AutoCloseable {
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(3);
    private final Duration timeout;
    private final Map<QmpEndpoint, Session> sessions = new ConcurrentHashMap<>();

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
        if (endpoint == null) {
            throw new IllegalArgumentException("endpoint must not be null");
        }
        if (commandJson == null || commandJson.isBlank()) {
            throw new IllegalArgumentException("commandJson must not be blank");
        }

        IOException failure = null;
        for (int attempt = 0; attempt < 2; attempt++) {
            Session session = session(endpoint);
            try {
                session.send(commandJson);
                return;
            } catch (IOException exception) {
                failure = exception;
                sessions.remove(endpoint, session);
                session.closeQuietly();
            }
        }
        throw failure == null ? new IOException("Failed to send QMP command") : failure;
    }

    @Override
    public void close() {
        sessions.values().forEach(Session::closeQuietly);
        sessions.clear();
    }

    public void close(QmpEndpoint endpoint) {
        if (endpoint == null) {
            return;
        }
        Session session = sessions.remove(endpoint);
        if (session != null) {
            session.closeQuietly();
        }
    }

    private Session session(QmpEndpoint endpoint) throws IOException {
        Session existing = sessions.get(endpoint);
        if (existing != null) {
            return existing;
        }

        Session created = Session.open(endpoint, timeout);
        Session raced = sessions.putIfAbsent(endpoint, created);
        if (raced != null) {
            created.closeQuietly();
            return raced;
        }
        return created;
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

    private static final class Session {
        private final Socket socket;
        private final BufferedReader reader;
        private final BufferedWriter writer;

        private Session(Socket socket, BufferedReader reader, BufferedWriter writer) {
            this.socket = socket;
            this.reader = reader;
            this.writer = writer;
        }

        private static Session open(QmpEndpoint endpoint, Duration timeout) throws IOException {
            Socket socket = new Socket();
            int timeoutMillis = Math.toIntExact(timeout.toMillis());
            socket.connect(new InetSocketAddress(endpoint.host(), endpoint.port()), timeoutMillis);
            socket.setSoTimeout(timeoutMillis);
            socket.setKeepAlive(true);
            socket.setTcpNoDelay(true);

            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
            readRequiredLine(reader, "QMP greeting");
            writeLine(writer, QmpCommand.capabilities());
            readCommandResponse(reader, "QMP capabilities response");
            return new Session(socket, reader, writer);
        }

        private synchronized void send(String commandJson) throws IOException {
            writeLine(writer, commandJson);
            readCommandResponse(reader, "QMP command response");
        }

        private void closeQuietly() {
            try {
                socket.close();
            } catch (IOException ignored) {
                // Best effort cleanup; the next command will reconnect if needed.
            }
        }
    }
}
