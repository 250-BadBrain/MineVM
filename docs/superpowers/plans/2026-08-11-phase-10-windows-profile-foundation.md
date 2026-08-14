# Phase 10 Windows Profile Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add OS profile-aware VM descriptors and QEMU command generation for Linux, Windows 10, and Windows 11 without enabling Windows by default.

**Architecture:** Store guest OS intent in `VirtualMachineDescriptor`, derive it from Forge config, and let `QemuCommandBuilder` add firmware/device options based on that descriptor. Windows support remains command-generation/configuration foundation; it does not download firmware, drivers, or Windows media.

**Tech Stack:** Minecraft Forge 1.20.1, Java 17 records/enums, QEMU command arguments, Gradle JavaExec self-tests.

## Global Constraints

- Default guest OS remains Ubuntu/Linux.
- Windows 10 requires UEFI and VirtIO-friendly storage options.
- Windows 11 additionally requires TPM 2.0 and Secure Boot intent in the descriptor.
- Tests must not launch QEMU.
- No unknown software downloads.

---

### Task 1: OS Profile Descriptor

**Files:**
- Create: `src/main/java/com/example/vm/vm/GuestOperatingSystem.java`
- Create: `src/main/java/com/example/vm/vm/FirmwareConfiguration.java`
- Modify: `src/main/java/com/example/vm/vm/VirtualMachineDescriptor.java`
- Create: `src/main/java/com/example/vm/test/GuestOperatingSystemSelfTest.java`

**Interfaces:**
- Produces: `GuestOperatingSystem.LINUX`, `WINDOWS_10`, `WINDOWS_11`
- Produces: `FirmwareConfiguration.forGuest(GuestOperatingSystem guestOperatingSystem)`

- [x] **Step 1: Add OS and firmware models**

Define required UEFI, Secure Boot, TPM, and VirtIO flags.

- [x] **Step 2: Extend descriptor**

Add `guestOperatingSystem` and `firmwareConfiguration` while preserving old constructor compatibility.

- [x] **Step 3: Add self-test**

Assert Linux/Windows profile requirements.

### Task 2: Config And Status Wiring

**Files:**
- Modify: `src/main/java/com/example/vm/config/ComputerRuntimeConfig.java`
- Modify: `src/main/java/com/example/vm/computer/ComputerStatusMessage.java`
- Modify: `src/main/java/com/example/vm/block/ComputerBlock.java`
- Modify: `src/main/java/com/example/vm/test/ComputerRuntimeConfigSelfTest.java`
- Modify: `src/main/java/com/example/vm/test/ComputerStatusMessageSelfTest.java`

**Interfaces:**
- Config `guestOperatingSystem` defaults to `LINUX`.
- Status message includes `Guest: LINUX`.

- [x] **Step 1: Add config value**

Parse guest OS safely, falling back to Linux for invalid values.

- [x] **Step 2: Add in-game status visibility**

Right-click status includes the guest OS profile.

### Task 3: QEMU Command Strategy

**Files:**
- Modify: `src/main/java/com/example/vm/vm/backend/local/QemuCommandBuilder.java`
- Modify: `src/main/java/com/example/vm/test/LocalQemuBackendSelfTest.java`
- Modify: `build.gradle`

**Interfaces:**
- Windows command includes `-machine q35`, VirtIO drive, and UEFI pflash placeholders.
- Windows 11 command additionally includes TPM and Secure Boot placeholders.

- [x] **Step 1: Add firmware/device args**

Generate OS-specific QEMU args only from descriptor data.

- [x] **Step 2: Add verification task**

Assert all profile classes and command fragments exist.

## Self-Review

Spec coverage: This implements the Windows support foundation requested by Phase 10 while keeping Linux default and avoiding downloads.

Placeholder scan: No placeholder instructions remain.

Type consistency: All referenced records/enums and methods are defined.
