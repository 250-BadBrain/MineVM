package com.example.vm.test;

import com.example.vm.screen.VncEndpoint;

public final class VncEndpointSelfTest {
    private VncEndpointSelfTest() {
    }

    public static void main(String[] args) {
        VncEndpoint endpoint = VncEndpoint.forComputer("computer-test");
        assertEquals(endpoint.host(), "127.0.0.1");
        if (endpoint.displayNumber() < 10 || endpoint.displayNumber() > 1009) {
            throw new AssertionError("Unexpected VNC display number: " + endpoint.displayNumber());
        }
        assertEquals(endpoint, VncEndpoint.forComputer("computer-test"));
        assertEquals(endpoint.port(), 5900 + endpoint.displayNumber());
        assertEquals(endpoint.toQemuArgument(), "127.0.0.1:" + endpoint.displayNumber());
        assertEquals(endpoint.toDisplayAddress(), "127.0.0.1:" + endpoint.port());
    }

    private static void assertEquals(Object actual, Object expected) {
        if (!actual.equals(expected)) {
            throw new AssertionError("Expected " + expected + " but got " + actual);
        }
    }
}
