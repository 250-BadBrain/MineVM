package com.example.vm.vm.backend.local;

import com.example.vm.MineVM;
import com.example.vm.input.VirtualMachineInput;
import com.example.vm.qmp.QmpClient;
import com.example.vm.qmp.QmpCommand;
import com.example.vm.qmp.QmpEndpoint;
import com.example.vm.qmp.QmpInputCommand;
import com.example.vm.screen.VncEndpoint;
import com.example.vm.vm.VirtualMachineBootRequest;
import com.example.vm.vm.VirtualMachineDescriptor;
import com.example.vm.vm.VirtualMachineHandle;
import com.example.vm.vm.VirtualMachineState;
import com.example.vm.vm.backend.VirtualMachineBackend;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public final class LocalQemuBackend implements VirtualMachineBackend {
    private static final long QMP_KEEP_ALIVE_INITIAL_DELAY_SECONDS = 5L;
    private static final long QMP_KEEP_ALIVE_INTERVAL_SECONDS = 10L;
    private static final int QMP_READY_ATTEMPTS = 20;
    private static final long QMP_READY_DELAY_MS = 250L;
    private static final long INPUT_QMP_IDLE_RESET_NANOS = TimeUnit.SECONDS.toNanos(10L);

    private final LocalQemuConfiguration configuration;
    private final QemuCommandBuilder commandBuilder;
    private final QmpClient qmpClient;
    private final Map<String, VirtualMachineDescriptor> descriptors = new ConcurrentHashMap<>();
    private final Map<String, Process> processes = new ConcurrentHashMap<>();
    private final Map<String, VirtualMachineState> states = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> qmpKeepAliveTasks = new ConcurrentHashMap<>();
    private final Map<String, Long> lastInputNanos = new ConcurrentHashMap<>();
    private final ScheduledExecutorService qmpKeepAliveExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "minevm-qmp-keepalive");
        thread.setDaemon(true);
        return thread;
    });

    public LocalQemuBackend(LocalQemuConfiguration configuration) {
        this(configuration, new QemuCommandBuilder(configuration), new QmpClient());
    }

    LocalQemuBackend(LocalQemuConfiguration configuration, QemuCommandBuilder commandBuilder, QmpClient qmpClient) {
        this.configuration = configuration;
        this.commandBuilder = commandBuilder;
        this.qmpClient = qmpClient;
    }

    @Override
    public VirtualMachineHandle create(VirtualMachineDescriptor descriptor) {
        LocalQemuPreflight.validateTools(configuration);
        descriptors.put(descriptor.computerId(), descriptor);
        states.putIfAbsent(descriptor.computerId(), VirtualMachineState.STOPPED);

        Path diskPath = commandBuilder.diskPath(descriptor);
        if (!Files.exists(diskPath)) {
            runCreateDiskCommand(descriptor, diskPath);
        }
        prepareFirmwareFiles(descriptor);

        return new VirtualMachineHandle(descriptor.computerId(), VirtualMachineState.STOPPED);
    }

    @Override
    public VirtualMachineHandle start(String computerId) {
        return start(VirtualMachineBootRequest.vmDisk(computerId));
    }

    @Override
    public VirtualMachineHandle start(VirtualMachineBootRequest bootRequest) {
        LocalQemuPreflight.validateTools(configuration);
        LocalQemuPreflight.validateBootRequest(bootRequest);
        VirtualMachineDescriptor descriptor = requireDescriptor(bootRequest.computerId());
        Process existingProcess = processes.get(bootRequest.computerId());
        if (existingProcess != null && existingProcess.isAlive()) {
            states.put(bootRequest.computerId(), VirtualMachineState.RUNNING);
            scheduleQmpKeepAlive(bootRequest.computerId(), existingProcess);
            return new VirtualMachineHandle(bootRequest.computerId(), VirtualMachineState.RUNNING);
        }

        states.put(bootRequest.computerId(), VirtualMachineState.STARTING);
        try {
            qmpClient.close(QmpEndpoint.forComputer(bootRequest.computerId()));
            java.util.List<String> command = commandBuilder.startCommand(descriptor, bootRequest);
            writeCommandLog(descriptor, command);
            Process process = new ProcessBuilder(command)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(commandBuilder.vmDirectory(descriptor).resolve("qemu-stderr.log").toFile())
                    .start();
            processes.put(bootRequest.computerId(), process);
            registerProcessExitHandler(bootRequest.computerId(), process);
            waitForQmpReady(bootRequest.computerId(), process);
            scheduleQmpKeepAlive(bootRequest.computerId(), process);
            MineVM.SCREEN_SERVICE.startVncReceiver(bootRequest.computerId(), VncEndpoint.forComputer(bootRequest.computerId()));
            states.put(bootRequest.computerId(), VirtualMachineState.RUNNING);
            return new VirtualMachineHandle(bootRequest.computerId(), VirtualMachineState.RUNNING);
        } catch (IOException exception) {
            Process process = processes.remove(bootRequest.computerId());
            if (process != null) {
                process.destroy();
            }
            MineVM.SCREEN_SERVICE.stopVncReceiver(bootRequest.computerId());
            qmpClient.close(QmpEndpoint.forComputer(bootRequest.computerId()));
            states.put(bootRequest.computerId(), VirtualMachineState.ERROR);
            throw new IllegalStateException("Failed to start local QEMU VM: " + bootRequest.computerId(), exception);
        }
    }

    @Override
    public VirtualMachineHandle stop(String computerId) {
        requireDescriptor(computerId);
        states.put(computerId, VirtualMachineState.STOPPING);

        Process process = processes.get(computerId);
        if (process != null) {
            cancelQmpKeepAlive(computerId);
            trySendQmpCommand(computerId, QmpCommand.SYSTEM_POWERDOWN);
            if (!waitForProcessExit(process)) {
                process.destroy();
            }
            processes.remove(computerId);
        }

        MineVM.SCREEN_SERVICE.stopVncReceiver(computerId);
        qmpClient.close(QmpEndpoint.forComputer(computerId));
        lastInputNanos.remove(computerId);
        states.put(computerId, VirtualMachineState.STOPPED);
        return new VirtualMachineHandle(computerId, VirtualMachineState.STOPPED);
    }

    @Override
    public VirtualMachineHandle restart(String computerId) {
        requireDescriptor(computerId);
        Process process = processes.get(computerId);
        if (process == null || !process.isAlive()) {
            return start(computerId);
        }

        sendQmpCommand(computerId, QmpCommand.SYSTEM_RESET);
        states.put(computerId, VirtualMachineState.RUNNING);
        return new VirtualMachineHandle(computerId, VirtualMachineState.RUNNING);
    }

    @Override
    public VirtualMachineHandle ejectSystemDisc(String computerId) {
        requireDescriptor(computerId);
        Process process = processes.get(computerId);
        VirtualMachineState state = getState(computerId);
        if (process != null && process.isAlive()) {
            trySendQmpJson(computerId, QmpCommand.eject(QemuCommandBuilder.SYSTEM_DISC_DEVICE_ID));
        }
        return new VirtualMachineHandle(computerId, state);
    }

    @Override
    public VirtualMachineState getState(String computerId) {
        Process process = processes.get(computerId);
        if (process != null && !process.isAlive()) {
            processes.remove(computerId);
            cancelQmpKeepAlive(computerId);
            MineVM.SCREEN_SERVICE.stopVncReceiver(computerId);
            qmpClient.close(QmpEndpoint.forComputer(computerId));
            states.put(computerId, VirtualMachineState.STOPPED);
        }
        if (states.get(computerId) == VirtualMachineState.ERROR && (process == null || !process.isAlive())) {
            states.put(computerId, descriptors.containsKey(computerId) ? VirtualMachineState.STOPPED : VirtualMachineState.NOT_CREATED);
        }
        return states.getOrDefault(computerId, VirtualMachineState.NOT_CREATED);
    }

    @Override
    public void sendInput(VirtualMachineInput input) {
        requireDescriptor(input.computerId());
        Process process = processes.get(input.computerId());
        if (process == null || !process.isAlive()) {
            if (process != null) {
                processes.remove(input.computerId(), process);
            }
            lastInputNanos.remove(input.computerId());
            states.put(input.computerId(), VirtualMachineState.STOPPED);
            return;
        }
        resetInputQmpSessionIfIdle(input.computerId());
        trySendQmpJson(input.computerId(), QmpInputCommand.toJson(input));
        lastInputNanos.put(input.computerId(), System.nanoTime());
    }

    @Override
    public void close() {
        processes.keySet().forEach(MineVM.SCREEN_SERVICE::stopVncReceiver);
        qmpKeepAliveTasks.values().forEach(task -> task.cancel(false));
        qmpKeepAliveTasks.clear();
        qmpKeepAliveExecutor.shutdownNow();
        processes.values().forEach(Process::destroy);
        processes.clear();
        qmpClient.close();
        descriptors.clear();
        states.clear();
        lastInputNanos.clear();
    }

    private void runCreateDiskCommand(VirtualMachineDescriptor descriptor, Path diskPath) {
        try {
            Files.createDirectories(diskPath.getParent());
            Process process = new ProcessBuilder(commandBuilder.createDiskCommand(descriptor))
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                states.put(descriptor.computerId(), VirtualMachineState.ERROR);
                throw new IllegalStateException("qemu-img exited with code " + exitCode + " for " + descriptor.computerId());
            }
        } catch (IOException exception) {
            states.put(descriptor.computerId(), VirtualMachineState.ERROR);
            throw new IllegalStateException("Failed to create VM disk: " + diskPath, exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            states.put(descriptor.computerId(), VirtualMachineState.ERROR);
            throw new IllegalStateException("Interrupted while creating VM disk: " + diskPath, exception);
        }
    }

    private void writeCommandLog(VirtualMachineDescriptor descriptor, java.util.List<String> command) {
        Path commandPath = commandBuilder.vmDirectory(descriptor).resolve("qemu-command.txt");
        try {
            Files.createDirectories(commandPath.getParent());
            Files.writeString(commandPath, String.join(System.lineSeparator(), command));
        } catch (IOException exception) {
            MineVM.LOGGER.warn("Failed to write QEMU command log for {}", descriptor.computerId(), exception);
        }
    }

    private void prepareFirmwareFiles(VirtualMachineDescriptor descriptor) {
        if (!descriptor.firmwareConfiguration().requiresUefi()) {
            return;
        }

        Path varsPath = commandBuilder.vmDirectory(descriptor).resolve("OVMF_VARS.fd").normalize();
        if (Files.exists(varsPath)) {
            return;
        }

        try {
            Files.copy(new LocalVmPaths(configuration).uefiVarsTemplatePath(), varsPath);
        } catch (IOException exception) {
            states.put(descriptor.computerId(), VirtualMachineState.ERROR);
            throw new IllegalStateException("Failed to prepare UEFI variable store for " + descriptor.computerId(), exception);
        }
    }

    private void sendQmpCommand(String computerId, String command) {
        sendQmpJson(computerId, QmpCommand.execute(command));
    }

    private void trySendQmpCommand(String computerId, String command) {
        trySendQmpJson(computerId, QmpCommand.execute(command));
    }

    private void sendQmpJson(String computerId, String json) {
        try {
            qmpClient.sendJson(QmpEndpoint.forComputer(computerId), json);
        } catch (IOException exception) {
            states.put(computerId, VirtualMachineState.ERROR);
            throw new IllegalStateException("Failed to send QMP command to " + computerId, exception);
        }
    }

    private void trySendQmpJson(String computerId, String json) {
        try {
            qmpClient.sendJson(QmpEndpoint.forComputer(computerId), json);
        } catch (IOException exception) {
            MineVM.LOGGER.warn("Ignored non-critical QMP failure for {}", computerId, exception);
        }
    }

    private void waitForQmpReady(String computerId, Process process) throws IOException {
        IOException lastFailure = null;
        for (int attempt = 0; attempt < QMP_READY_ATTEMPTS; attempt++) {
            if (!process.isAlive()) {
                throw new IOException("QEMU exited before QMP became ready");
            }
            try {
                qmpClient.sendJson(QmpEndpoint.forComputer(computerId), QmpCommand.execute(QmpCommand.QUERY_STATUS));
                return;
            } catch (IOException exception) {
                lastFailure = exception;
                sleepBeforeQmpRetry();
            }
        }
        throw lastFailure == null ? new IOException("QMP did not become ready") : lastFailure;
    }

    private static void sleepBeforeQmpRetry() {
        try {
            Thread.sleep(QMP_READY_DELAY_MS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private static boolean waitForProcessExit(Process process) {
        try {
            return process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private void registerProcessExitHandler(String computerId, Process process) {
        process.onExit().thenAccept(exitedProcess -> {
            if (processes.remove(computerId, exitedProcess)) {
                cancelQmpKeepAlive(computerId);
                MineVM.SCREEN_SERVICE.stopVncReceiver(computerId);
                qmpClient.close(QmpEndpoint.forComputer(computerId));
                lastInputNanos.remove(computerId);
                states.put(computerId, VirtualMachineState.STOPPED);
            }
        });
    }

    private void resetInputQmpSessionIfIdle(String computerId) {
        Long lastInput = lastInputNanos.get(computerId);
        if (lastInput == null) {
            return;
        }
        long now = System.nanoTime();
        if (shouldResetInputQmpSession(lastInput, now)) {
            qmpClient.close(QmpEndpoint.forComputer(computerId));
            MineVM.LOGGER.debug("Reset idle QMP input session for {}", computerId);
        }
    }

    public static boolean shouldResetInputQmpSession(long lastInputNanos, long nowNanos) {
        return nowNanos - lastInputNanos >= INPUT_QMP_IDLE_RESET_NANOS;
    }

    private void scheduleQmpKeepAlive(String computerId, Process process) {
        qmpKeepAliveTasks.computeIfAbsent(computerId, ignored -> qmpKeepAliveExecutor.scheduleAtFixedRate(
                () -> {
                    if (!process.isAlive()) {
                        cancelQmpKeepAlive(computerId);
                        return;
                    }
                    trySendQmpCommand(computerId, QmpCommand.QUERY_STATUS);
                },
                QMP_KEEP_ALIVE_INITIAL_DELAY_SECONDS,
                QMP_KEEP_ALIVE_INTERVAL_SECONDS,
                TimeUnit.SECONDS
        ));
    }

    private void cancelQmpKeepAlive(String computerId) {
        ScheduledFuture<?> task = qmpKeepAliveTasks.remove(computerId);
        if (task != null) {
            task.cancel(false);
        }
    }

    private VirtualMachineDescriptor requireDescriptor(String computerId) {
        VirtualMachineDescriptor descriptor = descriptors.get(computerId);
        if (descriptor == null) {
            throw new IllegalStateException("VM has not been created: " + computerId);
        }
        return descriptor;
    }
}
