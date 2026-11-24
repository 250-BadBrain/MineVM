package com.example.vm.vm.backend;

import com.example.vm.floppy.FloppyMedia;
import com.example.vm.input.VirtualMachineInput;
import com.example.vm.vm.VirtualMachineBootRequest;
import com.example.vm.vm.VirtualMachineDescriptor;
import com.example.vm.vm.VirtualMachineHandle;
import com.example.vm.vm.VirtualMachineState;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class NoopVirtualMachineBackend implements VirtualMachineBackend {
    private final Map<String, VirtualMachineState> states = new ConcurrentHashMap<>();

    @Override
    public VirtualMachineHandle create(VirtualMachineDescriptor descriptor) {
        states.putIfAbsent(descriptor.computerId(), VirtualMachineState.STOPPED);
        return new VirtualMachineHandle(descriptor.computerId(), states.get(descriptor.computerId()));
    }

    @Override
    public VirtualMachineHandle start(String computerId) {
        return start(VirtualMachineBootRequest.vmDisk(computerId));
    }

    @Override
    public VirtualMachineHandle start(VirtualMachineBootRequest bootRequest) {
        String computerId = bootRequest.computerId();
        requireCreated(computerId);
        states.put(computerId, VirtualMachineState.RUNNING);
        return new VirtualMachineHandle(computerId, VirtualMachineState.RUNNING);
    }

    @Override
    public VirtualMachineHandle stop(String computerId) {
        requireCreated(computerId);
        states.put(computerId, VirtualMachineState.STOPPED);
        return new VirtualMachineHandle(computerId, VirtualMachineState.STOPPED);
    }

    @Override
    public VirtualMachineHandle restart(String computerId) {
        requireCreated(computerId);
        states.put(computerId, VirtualMachineState.RUNNING);
        return new VirtualMachineHandle(computerId, VirtualMachineState.RUNNING);
    }

    @Override
    public VirtualMachineHandle ejectSystemDisc(String computerId) {
        requireCreated(computerId);
        return new VirtualMachineHandle(computerId, states.get(computerId));
    }

    @Override
    public VirtualMachineHandle ejectBootFloppy(String computerId) {
        requireCreated(computerId);
        return new VirtualMachineHandle(computerId, states.get(computerId));
    }

    @Override
    public VirtualMachineHandle changeBootFloppyMedium(String computerId, FloppyMedia media) {
        requireCreated(computerId);
        if (media == null) {
            throw new IllegalArgumentException("media must not be null");
        }
        return new VirtualMachineHandle(computerId, states.get(computerId));
    }

    @Override
    public VirtualMachineState getState(String computerId) {
        return states.getOrDefault(computerId, VirtualMachineState.NOT_CREATED);
    }

    @Override
    public void sendInput(VirtualMachineInput input) {
        requireCreated(input.computerId());
    }

    @Override
    public void close() {
        states.clear();
    }

    private void requireCreated(String computerId) {
        if (!states.containsKey(computerId)) {
            throw new IllegalStateException("VM has not been created: " + computerId);
        }
    }
}
