package com.example.vm.vm;

import com.example.vm.concurrent.ComputerExecutors;
import com.example.vm.floppy.FloppyMedia;
import com.example.vm.input.VirtualMachineInput;
import com.example.vm.vm.backend.VirtualMachineBackend;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Function;

public final class VirtualMachineManager implements AutoCloseable {
    private VirtualMachineBackend backend;
    private ExecutorService executor;
    private final Set<CompletableFuture<?>> pending = ConcurrentHashMap.newKeySet();

    public VirtualMachineManager(VirtualMachineBackend backend) {
        this(backend, newVmExecutor());
    }

    VirtualMachineManager(VirtualMachineBackend backend, ExecutorService executor) {
        this.backend = Objects.requireNonNull(backend, "backend");
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    public CompletableFuture<VirtualMachineHandle> create(VirtualMachineDescriptor descriptor) {
        return submit(current -> current.create(descriptor));
    }

    public CompletableFuture<VirtualMachineHandle> start(String computerId) {
        return submit(current -> current.start(computerId));
    }

    public CompletableFuture<VirtualMachineHandle> start(VirtualMachineBootRequest bootRequest) {
        return submit(current -> current.start(bootRequest));
    }

    public CompletableFuture<VirtualMachineHandle> stop(String computerId) {
        return submit(current -> current.stop(computerId));
    }

    public CompletableFuture<VirtualMachineHandle> restart(String computerId) {
        return submit(current -> current.restart(computerId));
    }

    public CompletableFuture<VirtualMachineHandle> ejectSystemDisc(String computerId) {
        return submit(current -> current.ejectSystemDisc(computerId));
    }

    public CompletableFuture<VirtualMachineHandle> ejectBootFloppy(String computerId) {
        return submit(current -> current.ejectBootFloppy(computerId));
    }

    public CompletableFuture<VirtualMachineHandle> changeBootFloppyMedium(String computerId, FloppyMedia media) {
        return submit(current -> current.changeBootFloppyMedium(computerId, media));
    }

    public CompletableFuture<VirtualMachineState> getState(String computerId) {
        return submit(current -> current.getState(computerId));
    }

    public CompletableFuture<VirtualMachineDiagnostics> diagnostics(String computerId) {
        return submit(current -> current.diagnostics(computerId));
    }

    public CompletableFuture<String> maintainDisk(String computerId, DiskMaintenance.Operation operation) {
        return submit(current -> current.maintainDisk(computerId, operation));
    }

    public CompletableFuture<Void> sendInput(VirtualMachineInput input) {
        return submit(current -> {
            current.sendInput(input);
            return null;
        });
    }

    public synchronized void replaceBackend(VirtualMachineBackend backend) {
        Objects.requireNonNull(backend, "backend");
        pending.forEach(future -> future.cancel(false));
        this.backend.close();
        this.backend = Objects.requireNonNull(backend, "backend");
        if (executor.isShutdown() || executor.isTerminated()) {
            executor = newVmExecutor();
        }
    }

    @Override
    public synchronized void close() {
        executor.shutdownNow();
        pending.forEach(future -> future.cancel(false));
        backend.close();
    }

    private synchronized ExecutorService executor() {
        if (executor.isShutdown() || executor.isTerminated()) {
            executor = newVmExecutor();
        }
        return executor;
    }

    private synchronized <T> CompletableFuture<T> submit(Function<VirtualMachineBackend, T> action) {
        VirtualMachineBackend current = backend;
        CompletableFuture<T> future = new CompletableFuture<>();
        pending.add(future);
        future.whenComplete((result, failure) -> pending.remove(future));
        try {
            executor().execute(() -> {
                if (!future.isDone()) {
                    try {
                        future.complete(action.apply(current));
                    } catch (Throwable failure) {
                        future.completeExceptionally(failure);
                    }
                }
            });
        } catch (RejectedExecutionException exception) {
            future.completeExceptionally(exception);
        }
        return future;
    }

    private static ExecutorService newVmExecutor() {
        return ComputerExecutors.singleThread("minevm-vm-manager", 128);
    }
}
