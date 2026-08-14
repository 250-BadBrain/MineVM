package com.example.vm.vm.backend.local;

import com.example.vm.ComputerInMinecraft;
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

public final class LocalQemuBackend implements VirtualMachineBackend {
    private final LocalQemuConfiguration configuration;
    private final QemuCommandBuilder commandBuilder;
    private final QmpClient qmpClient;
    private final Map<String, VirtualMachineDescriptor> descriptors = new ConcurrentHashMap<>();
    private final Map<String, Process> processes = new ConcurrentHashMap<>();
    private final Map<String, VirtualMachineState> states = new ConcurrentHashMap<>();

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
            return new VirtualMachineHandle(bootRequest.computerId(), VirtualMachineState.RUNNING);
        }

        states.put(bootRequest.computerId(), VirtualMachineState.STARTING);
        try {
            Process process = new ProcessBuilder(commandBuilder.startCommand(descriptor, bootRequest))
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
            processes.put(bootRequest.computerId(), process);
            registerProcessExitHandler(bootRequest.computerId(), process);
            ComputerInMinecraft.SCREEN_SERVICE.startVncReceiver(bootRequest.computerId(), VncEndpoint.forComputer(bootRequest.computerId()));
            states.put(bootRequest.computerId(), VirtualMachineState.RUNNING);
            return new VirtualMachineHandle(bootRequest.computerId(), VirtualMachineState.RUNNING);
        } catch (IOException exception) {
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
            trySendQmpCommand(computerId, QmpCommand.SYSTEM_POWERDOWN);
            if (!waitForProcessExit(process)) {
                process.destroy();
            }
            processes.remove(computerId);
        }

        ComputerInMinecraft.SCREEN_SERVICE.stopVncReceiver(computerId);
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
        trySendQmpJson(input.computerId(), QmpInputCommand.toJson(input));
    }

    @Override
    public void close() {
        processes.keySet().forEach(ComputerInMinecraft.SCREEN_SERVICE::stopVncReceiver);
        processes.values().forEach(Process::destroy);
        processes.clear();
        descriptors.clear();
        states.clear();
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
            ComputerInMinecraft.LOGGER.warn("Ignored non-critical QMP failure for {}", computerId, exception);
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
                ComputerInMinecraft.SCREEN_SERVICE.stopVncReceiver(computerId);
                states.put(computerId, VirtualMachineState.STOPPED);
            }
        });
    }

    private VirtualMachineDescriptor requireDescriptor(String computerId) {
        VirtualMachineDescriptor descriptor = descriptors.get(computerId);
        if (descriptor == null) {
            throw new IllegalStateException("VM has not been created: " + computerId);
        }
        return descriptor;
    }
}
