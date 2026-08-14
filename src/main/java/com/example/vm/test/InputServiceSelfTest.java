package com.example.vm.test;

import com.example.vm.input.InputService;
import com.example.vm.input.KeyboardInput;
import com.example.vm.input.VirtualMachineInput;
import com.example.vm.vm.VirtualMachineBootRequest;
import com.example.vm.vm.VirtualMachineDescriptor;
import com.example.vm.vm.VirtualMachineHandle;
import com.example.vm.vm.VirtualMachineManager;
import com.example.vm.vm.VirtualMachineState;
import com.example.vm.vm.backend.NoopVirtualMachineBackend;
import com.example.vm.vm.backend.VirtualMachineBackend;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class InputServiceSelfTest {
    private InputServiceSelfTest() {
    }

    public static void main(String[] args) throws Exception {
        CountingBackend backend = new CountingBackend();
        try (VirtualMachineManager manager = new VirtualMachineManager(backend);
             InputService service = new InputService(manager)) {
            VirtualMachineDescriptor descriptor = new VirtualMachineDescriptor("computer-test", 4, 4096, "computer-test.qcow2");
            manager.create(descriptor).get(10, TimeUnit.SECONDS);

            service.send(new KeyboardInput("computer-test", "a", true)).get(10, TimeUnit.SECONDS);
            if (backend.inputCount.get() != 1) {
                throw new AssertionError("Expected one input event but got " + backend.inputCount.get());
            }

            service.close();
            service.send(new KeyboardInput("computer-test", "a", false)).get(10, TimeUnit.SECONDS);
            if (backend.inputCount.get() != 2) {
                throw new AssertionError("InputService must recover after close");
            }
        }
    }

    private static final class CountingBackend implements VirtualMachineBackend {
        private final NoopVirtualMachineBackend delegate = new NoopVirtualMachineBackend();
        private final AtomicInteger inputCount = new AtomicInteger();

        @Override
        public VirtualMachineHandle create(VirtualMachineDescriptor descriptor) {
            return delegate.create(descriptor);
        }

        @Override
        public VirtualMachineHandle start(String computerId) {
            return delegate.start(computerId);
        }

        @Override
        public VirtualMachineHandle start(VirtualMachineBootRequest bootRequest) {
            return delegate.start(bootRequest);
        }

        @Override
        public VirtualMachineHandle stop(String computerId) {
            return delegate.stop(computerId);
        }

        @Override
        public VirtualMachineHandle restart(String computerId) {
            return delegate.restart(computerId);
        }

        @Override
        public VirtualMachineHandle ejectSystemDisc(String computerId) {
            return delegate.ejectSystemDisc(computerId);
        }

        @Override
        public VirtualMachineState getState(String computerId) {
            return delegate.getState(computerId);
        }

        @Override
        public void sendInput(VirtualMachineInput input) {
            delegate.sendInput(input);
            inputCount.incrementAndGet();
        }

        @Override
        public void close() {
            delegate.close();
        }
    }
}
