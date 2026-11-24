package com.example.vm.screen;

import com.example.vm.input.VirtualMachineInput;
import java.io.IOException;
import java.util.function.Consumer;

public interface ScreenTransport extends AutoCloseable {
    void receive(VncEndpoint endpoint, Consumer<ScreenFrame> consumer) throws IOException;
    boolean sendInput(VncEndpoint endpoint, VirtualMachineInput input);
    boolean hasActiveConnection(VncEndpoint endpoint);
    void close(VncEndpoint endpoint);
    @Override void close();
    default String name() { return "VNC"; }
}
