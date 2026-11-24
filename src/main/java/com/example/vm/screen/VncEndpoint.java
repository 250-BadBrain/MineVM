package com.example.vm.screen;

public record VncEndpoint(String host, int displayNumber) {
    private static final int FIRST_DISPLAY = 4100;
    private static final int DISPLAY_COUNT = 20_000;

    public VncEndpoint {
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("host must not be blank");
        }
        if (displayNumber < 0) {
            throw new IllegalArgumentException("displayNumber must not be negative");
        }
    }

    public static VncEndpoint forComputer(String computerId) {
        if (computerId == null || computerId.isBlank()) {
            throw new IllegalArgumentException("computerId must not be blank");
        }

        int offset = Math.floorMod(computerId.hashCode(), DISPLAY_COUNT);
        return new VncEndpoint("127.0.0.1", FIRST_DISPLAY + offset);
    }

    public static VncEndpoint localPort(int port) {
        if (port < 5900) {
            throw new IllegalArgumentException("VNC port must be 5900 or greater");
        }
        return new VncEndpoint("127.0.0.1", port - 5900);
    }

    public int port() {
        return 5900 + displayNumber;
    }

    public String toQemuArgument() {
        return host + ":" + displayNumber;
    }

    public String toDisplayAddress() {
        return host + ":" + port();
    }
}
