package com.example.vm.qmp;

public record QmpEndpoint(String host, int port) {
    private static final int FIRST_DYNAMIC_PORT = 43000;
    private static final int PORT_COUNT = 20_000;

    public QmpEndpoint {
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("host must not be blank");
        }
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("port must be between 1 and 65535");
        }
    }

    public static QmpEndpoint forComputer(String computerId) {
        if (computerId == null || computerId.isBlank()) {
            throw new IllegalArgumentException("computerId must not be blank");
        }

        int offset = Math.floorMod(computerId.hashCode(), PORT_COUNT);
        return new QmpEndpoint("127.0.0.1", FIRST_DYNAMIC_PORT + offset);
    }

    public String toQemuArgument() {
        return "tcp:" + host + ":" + port + ",server,nowait";
    }
}
