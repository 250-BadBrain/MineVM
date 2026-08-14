package com.example.vm.vm.backend;

import com.example.vm.vm.VirtualMachineDescriptor;
import com.example.vm.vm.VirtualMachineBootRequest;
import com.example.vm.vm.VirtualMachineHandle;
import com.example.vm.vm.VirtualMachineState;
import com.example.vm.input.VirtualMachineInput;

public interface VirtualMachineBackend extends AutoCloseable {
    VirtualMachineHandle create(VirtualMachineDescriptor descriptor);

    VirtualMachineHandle start(String computerId);

    VirtualMachineHandle start(VirtualMachineBootRequest bootRequest);

    VirtualMachineHandle stop(String computerId);

    VirtualMachineHandle restart(String computerId);

    VirtualMachineHandle ejectSystemDisc(String computerId);

    VirtualMachineState getState(String computerId);

    void sendInput(VirtualMachineInput input);

    @Override
    void close();
}
