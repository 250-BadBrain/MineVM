package com.example.vm.vm;

import com.example.vm.concurrent.ComputerExecutors;
import com.example.vm.input.VirtualMachineInput;
import com.example.vm.vm.backend.VirtualMachineBackend;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public final class VirtualMachineManager implements AutoCloseable {
    private VirtualMachineBackend backend;
    private ExecutorService executor;

    public VirtualMachineManager(VirtualMachineBackend backend) {
        this(backend, newVmExecutor());
    }

    VirtualMachineManager(VirtualMachineBackend backend, ExecutorService executor) {
        this.backend = Objects.requireNonNull(backend, "backend");
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    public CompletableFuture<VirtualMachineHandle> create(VirtualMachineDescriptor descriptor) {
        return CompletableFuture.supplyAsync(() -> backend.create(descriptor), executor());
    }

    public CompletableFuture<VirtualMachineHandle> start(String computerId) {
        return CompletableFuture.supplyAsync(() -> backend.start(computerId), executor());
    }

    public CompletableFuture<VirtualMachineHandle> start(VirtualMachineBootRequest bootRequest) {
        return CompletableFuture.supplyAsync(() -> backend.start(bootRequest), executor());
    }

    public CompletableFuture<VirtualMachineHandle> stop(String computerId) {
        return CompletableFuture.supplyAsync(() -> backend.stop(computerId), executor());
    }

    public CompletableFuture<VirtualMachineHandle> restart(String computerId) {
        return CompletableFuture.supplyAsync(() -> backend.restart(computerId), executor());
    }

    public CompletableFuture<VirtualMachineHandle> ejectSystemDisc(String computerId) {
        return CompletableFuture.supplyAsync(() -> backend.ejectSystemDisc(computerId), executor());
    }

    public CompletableFuture<VirtualMachineState> getState(String computerId) {
        return CompletableFuture.supplyAsync(() -> backend.getState(computerId), executor());
    }

    public CompletableFuture<Void> sendInput(VirtualMachineInput input) {
        return CompletableFuture.runAsync(() -> backend.sendInput(input), executor());
    }

    public synchronized void replaceBackend(VirtualMachineBackend backend) {
        this.backend.close();
        this.backend = Objects.requireNonNull(backend, "backend");
        if (executor.isShutdown() || executor.isTerminated()) {
            executor = newVmExecutor();
        }
    }

    @Override
    public synchronized void close() {
        executor.shutdownNow();
        backend.close();
    }

    private synchronized ExecutorService executor() {
        if (executor.isShutdown() || executor.isTerminated()) {
            executor = newVmExecutor();
        }
        return executor;
    }

    private static ExecutorService newVmExecutor() {
        return ComputerExecutors.singleThread("computer-in-minecraft-vm-manager", 128);
    }
}
