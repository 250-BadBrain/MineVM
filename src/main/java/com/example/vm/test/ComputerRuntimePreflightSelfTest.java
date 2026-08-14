package com.example.vm.test;

import com.example.vm.config.ComputerRuntimePreflight;
import com.example.vm.vm.GuestOperatingSystem;

import java.util.List;

public final class ComputerRuntimePreflightSelfTest {
    private ComputerRuntimePreflightSelfTest() {
    }

    public static void main(String[] args) {
        assertDefaultPowerOnIsBlockedSafely();
        assertDefaultDiscBootReportsBackendBeforeIso();
    }

    private static void assertDefaultPowerOnIsBlockedSafely() {
        List<String> errors = ComputerRuntimePreflight.validatePowerOn();
        assertContains(errors, "Local QEMU backend is disabled in config.");
    }

    private static void assertDefaultDiscBootReportsBackendBeforeIso() {
        List<String> errors = ComputerRuntimePreflight.validateSystemDiscBoot(GuestOperatingSystem.UBUNTU_SERVER);
        assertContains(errors, "Local QEMU backend is disabled in config.");
    }

    private static void assertContains(List<String> errors, String expected) {
        if (!errors.contains(expected)) {
            throw new AssertionError("Expected preflight errors to contain " + expected + " but got " + errors);
        }
    }
}
