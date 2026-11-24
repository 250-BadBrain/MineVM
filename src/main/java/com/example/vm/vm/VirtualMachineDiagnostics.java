package com.example.vm.vm;

public record VirtualMachineDiagnostics(VirtualMachineState state, long processId, long cpuMillis, String lastError, GuestOperatingSystem guest) {
    public static VirtualMachineDiagnostics unavailable(VirtualMachineState state) {
        return new VirtualMachineDiagnostics(state, -1, 0, "", null);
    }
}
