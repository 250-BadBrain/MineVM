# Phase 5 Local QEMU Backend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a local QEMU backend implementation capable of creating qcow2 disks and launching QEMU through the VM Manager boundary.

**Architecture:** Keep `LocalQemuBackend` behind `VirtualMachineBackend` and keep `NoopVirtualMachineBackend` as the default mod runtime backend. Split command construction into focused value objects so build verification can test command shape without launching QEMU.

**Tech Stack:** Minecraft Forge 1.20.1, Java 17, Gradle 8.8, QEMU CLI, `ProcessBuilder`.

## Global Constraints

- Minecraft version: `1.20.1`
- Mod loader: Forge
- Java version: JDK 17
- Phase 5 only: implement LocalQemuBackend process support, no QMP, no VNC screen receiver, no input system.
- Minecraft BlockEntity must not directly call QEMU.
- Build tests must not start QEMU or create real VM disk images.

---

### Task 1: Extend VM Descriptor For Local Launch Data

**Files:**
- Modify: `src/main/java/com/example/vm/vm/VirtualMachineDescriptor.java`
- Modify: `src/main/java/com/example/vm/test/VirtualMachineManagerSelfTest.java`

**Interfaces:**
- Produces: `VirtualMachineDescriptor#diskSizeGb()`
- Produces: `VirtualMachineDescriptor#isoPath()`

- [x] Add disk size and optional ISO path to VM descriptors.
- [x] Preserve the existing constructor used by Phase 4 tests.

### Task 2: Add QEMU Command Builder

**Files:**
- Create: `src/main/java/com/example/vm/vm/backend/local/LocalQemuConfiguration.java`
- Create: `src/main/java/com/example/vm/vm/backend/local/QemuCommandBuilder.java`

**Interfaces:**
- Produces: `QemuCommandBuilder#createDiskCommand(VirtualMachineDescriptor)`
- Produces: `QemuCommandBuilder#startCommand(VirtualMachineDescriptor)`

- [x] Build `qemu-img create -f qcow2 <disk> <size>G`.
- [x] Build `qemu-system-x86_64` command with CPU, memory, qcow2 disk, optional ISO boot, and WHPX accelerator.

### Task 3: Add Local QEMU Backend

**Files:**
- Create: `src/main/java/com/example/vm/vm/backend/local/LocalQemuBackend.java`

**Interfaces:**
- Consumes: `VirtualMachineBackend`
- Consumes: `QemuCommandBuilder`

- [x] Create qcow2 only when the disk file does not exist.
- [x] Start QEMU with `ProcessBuilder` and retain the process by `computerId`.
- [x] Stop QEMU by destroying the process.
- [x] Return lifecycle state without exposing QEMU details to callers.

### Task 4: Add Verification

**Files:**
- Create: `src/main/java/com/example/vm/test/LocalQemuBackendSelfTest.java`
- Modify: `build.gradle`

**Interfaces:**
- Consumes: `QemuCommandBuilder`

- [x] Verify generated QEMU commands without launching QEMU.
- [x] Verify default mod backend remains `NoopVirtualMachineBackend`.
- [x] Add `verifyPhase5LocalQemuBackend` to `check`.
- [x] Run `gradlew build --no-daemon --max-workers=1`.
