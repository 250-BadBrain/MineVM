package com.example.vm.input;

import com.example.vm.concurrent.ComputerExecutors;
import com.example.vm.vm.VirtualMachineManager;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public final class InputService implements AutoCloseable {
    private final VirtualMachineManager virtualMachineManager;
    private ExecutorService executor;

    public InputService(VirtualMachineManager virtualMachineManager) {
        this(virtualMachineManager, newInputExecutor());
    }

    InputService(VirtualMachineManager virtualMachineManager, ExecutorService executor) {
        this.virtualMachineManager = Objects.requireNonNull(virtualMachineManager, "virtualMachineManager");
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    public CompletableFuture<Void> send(VirtualMachineInput input) {
        Objects.requireNonNull(input, "input");
        return CompletableFuture.runAsync(() -> virtualMachineManager.sendInput(input).join(), executor());
    }

    @Override
    public synchronized void close() {
        executor.shutdownNow();
    }

    private synchronized ExecutorService executor() {
        if (executor.isShutdown() || executor.isTerminated()) {
            executor = newInputExecutor();
        }
        return executor;
    }

    private static ExecutorService newInputExecutor() {
        return ComputerExecutors.singleThread("minevm-input-sender", 256);
    }
}
