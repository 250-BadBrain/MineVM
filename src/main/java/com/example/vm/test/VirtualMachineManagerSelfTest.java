package com.example.vm.test;

import com.example.vm.input.KeyboardInput;
import com.example.vm.input.VirtualMachineInput;
import com.example.vm.vm.VirtualMachineDescriptor;
import com.example.vm.vm.VirtualMachineBootRequest;
import com.example.vm.vm.VirtualMachineHandle;
import com.example.vm.vm.VirtualMachineManager;
import com.example.vm.vm.VirtualMachineState;
import com.example.vm.vm.backend.NoopVirtualMachineBackend;
import com.example.vm.vm.backend.VirtualMachineBackend;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public final class VirtualMachineManagerSelfTest {
    private VirtualMachineManagerSelfTest() {
    }

    public static void main(String[] args) throws Exception {
        RecordingBackend backend = new RecordingBackend();

        try (VirtualMachineManager manager = new VirtualMachineManager(backend)) {
            VirtualMachineDescriptor descriptor = new VirtualMachineDescriptor(
                    "computer-test",
                    4,
                    4096,
                    "computer-test.qcow2"
            );

            assertHandle(manager.create(descriptor).get(10, TimeUnit.SECONDS), "computer-test", VirtualMachineState.STOPPED);
            assertState(manager.getState("computer-test").get(10, TimeUnit.SECONDS), VirtualMachineState.STOPPED);
            assertHandle(manager.start("computer-test").get(10, TimeUnit.SECONDS), "computer-test", VirtualMachineState.RUNNING);
            assertState(manager.getState("computer-test").get(10, TimeUnit.SECONDS), VirtualMachineState.RUNNING);
            assertHandle(manager.restart("computer-test").get(10, TimeUnit.SECONDS), "computer-test", VirtualMachineState.RUNNING);
            assertHandle(manager.start(VirtualMachineBootRequest.vmDisk("computer-test")).get(10, TimeUnit.SECONDS), "computer-test", VirtualMachineState.RUNNING);
            assertHandle(manager.ejectSystemDisc("computer-test").get(10, TimeUnit.SECONDS), "computer-test", VirtualMachineState.RUNNING);
            manager.sendInput(new KeyboardInput("computer-test", "a", true)).get(10, TimeUnit.SECONDS);
            assertHandle(manager.stop("computer-test").get(10, TimeUnit.SECONDS), "computer-test", VirtualMachineState.STOPPED);

            String threadName = backend.lastThreadName.get();
            if (threadName == null || !threadName.startsWith("minevm-vm-manager")) {
                throw new AssertionError("VM manager did not use its background executor: " + threadName);
            }

            manager.close();
            assertHandle(manager.create(descriptor).get(10, TimeUnit.SECONDS), "computer-test", VirtualMachineState.STOPPED);
            assertState(manager.getState("computer-test").get(10, TimeUnit.SECONDS), VirtualMachineState.STOPPED);

            manager.replaceBackend(new RecordingBackend());
            assertState(manager.getState("computer-test").get(10, TimeUnit.SECONDS), VirtualMachineState.NOT_CREATED);
            assertHandle(manager.create(descriptor).get(10, TimeUnit.SECONDS), "computer-test", VirtualMachineState.STOPPED);
        }
    }

    private static void assertHandle(VirtualMachineHandle actual, String computerId, VirtualMachineState state) {
        if (!actual.computerId().equals(computerId) || actual.state() != state) {
            throw new AssertionError("Unexpected VM handle: " + actual);
        }
    }

    private static void assertState(VirtualMachineState actual, VirtualMachineState expected) {
        if (actual != expected) {
            throw new AssertionError("Expected state " + expected + " but got " + actual);
        }
    }

    private static final class RecordingBackend implements VirtualMachineBackend {
        private final NoopVirtualMachineBackend delegate = new NoopVirtualMachineBackend();
        private final AtomicReference<String> lastThreadName = new AtomicReference<>();

        @Override
        public VirtualMachineHandle create(VirtualMachineDescriptor descriptor) {
            recordThread();
            return delegate.create(descriptor);
        }

        @Override
        public VirtualMachineHandle start(String computerId) {
            recordThread();
            return delegate.start(computerId);
        }

        @Override
        public VirtualMachineHandle start(VirtualMachineBootRequest bootRequest) {
            recordThread();
            return delegate.start(bootRequest);
        }

        @Override
        public VirtualMachineHandle stop(String computerId) {
            recordThread();
            return delegate.stop(computerId);
        }

        @Override
        public VirtualMachineHandle restart(String computerId) {
            recordThread();
            return delegate.restart(computerId);
        }

        @Override
        public VirtualMachineHandle ejectSystemDisc(String computerId) {
            recordThread();
            return delegate.ejectSystemDisc(computerId);
        }

        @Override
        public VirtualMachineState getState(String computerId) {
            recordThread();
            return delegate.getState(computerId);
        }

        @Override
        public void sendInput(VirtualMachineInput input) {
            recordThread();
            delegate.sendInput(input);
        }

        @Override
        public void close() {
            delegate.close();
        }

        private void recordThread() {
            lastThreadName.set(Thread.currentThread().getName());
        }
    }
}
