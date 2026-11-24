package com.example.vm.vm.backend;

import com.example.vm.floppy.FloppyMedia;
import com.example.vm.vm.VirtualMachineDescriptor;
import com.example.vm.vm.VirtualMachineBootRequest;
import com.example.vm.vm.VirtualMachineHandle;
import com.example.vm.vm.VirtualMachineState;
import com.example.vm.input.VirtualMachineInput;

public interface VirtualMachineBackend extends AutoCloseable {
    default com.example.vm.vm.VirtualMachineDiagnostics diagnostics(String computerId) {
        return com.example.vm.vm.VirtualMachineDiagnostics.unavailable(getState(computerId));
    }

    default String maintainDisk(String computerId, com.example.vm.vm.DiskMaintenance.Operation operation) {
        throw new IllegalStateException("Local QEMU backend must be enabled");
    }
    VirtualMachineHandle create(VirtualMachineDescriptor descriptor);

    VirtualMachineHandle start(String computerId);

    VirtualMachineHandle start(VirtualMachineBootRequest bootRequest);

    VirtualMachineHandle stop(String computerId);

    VirtualMachineHandle restart(String computerId);

    VirtualMachineHandle ejectSystemDisc(String computerId);

    VirtualMachineHandle ejectBootFloppy(String computerId);

    VirtualMachineHandle changeBootFloppyMedium(String computerId, FloppyMedia media);

    VirtualMachineState getState(String computerId);

    void sendInput(VirtualMachineInput input);

    @Override
    void close();
}
