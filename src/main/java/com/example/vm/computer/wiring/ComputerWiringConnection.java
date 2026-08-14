package com.example.vm.computer.wiring;

public record ComputerWiringConnection(
        ComputerConnectionType connectionType,
        ComputerWiringEndpoint source,
        ComputerWiringEndpoint target
) {
    public ComputerWiringConnection {
        if (connectionType == null) {
            throw new IllegalArgumentException("connectionType must not be null");
        }
        if (source == null || target == null) {
            throw new IllegalArgumentException("source and target must not be null");
        }
    }

    public boolean isDisplayConnection() {
        return connectionType == ComputerConnectionType.HDMI_DISPLAY;
    }

    public boolean isPeripheralConnection() {
        return connectionType == ComputerConnectionType.USB_PERIPHERAL;
    }
}
