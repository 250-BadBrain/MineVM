# BIOS Boot Button Semantics And Disc ISO Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove Power On ambiguity from BIOS by making boot device buttons perform startup, then add per-system-disc ISO configuration.

**Architecture:** BIOS remains server-authoritative. The client shows boot actions only while the VM is off/uncreated, and shows `Power Off`/`Reset` while the VM is running. System discs select a guest OS profile and now also select an OS-specific ISO path from config when creating the VM descriptor.

**Tech Stack:** Minecraft Forge 1.20.1, Java 17, Forge networking, Gradle verification.

## Global Constraints

- Client screen must not call QEMU, QMP, or `VirtualMachineManager` directly.
- `Boot from Disc` and `Boot VM Disk` are the only power-on entries.
- `Power Off` and `Reset` appear only when the VM is running or starting.
- OS installation starts by booting an inserted system disc.
- Tests must not launch QEMU.

---

### Task 1: Update BIOS Button Semantics

**Files:**
- Modify: `src/main/java/com/example/vm/computer/ComputerBiosAction.java`
- Modify: `src/main/java/com/example/vm/computer/ComputerBiosActions.java`
- Modify: `src/main/java/com/example/vm/client/ComputerBiosScreen.java`
- Modify: `build.gradle`

**Interfaces:**
- Produces: `ComputerBiosAction.RESET`
- Produces: stopped BIOS actions `Boot from Disc`, `Boot VM Disk`, `Eject Disc`
- Produces: running BIOS actions `Power Off`, `Reset`, disabled `Eject Disc`

- [x] **Step 1: Add reset action**

Add `RESET` to the enum and route it to `VirtualMachineManager.restart`.

- [x] **Step 2: Change screen button state**

Make boot buttons active only when powered off; make `Power Off` and `Reset` visible only when powered on.

- [x] **Step 3: Update verification**

Require `Power Off` and `Reset`; reject generic `Power On` and generic `Power`.

### Task 2: Add Per-Disc ISO Config

**Files:**
- Modify: `src/main/java/com/example/vm/config/ComputerRuntimeConfig.java`
- Modify: `src/main/java/com/example/vm/test/ComputerRuntimeConfigSelfTest.java`
- Modify: `build.gradle`

**Interfaces:**
- Produces: `ComputerRuntimeConfig.isoPathFor(GuestOperatingSystem guestOperatingSystem): String`
- `descriptorFor(computer, guestOperatingSystem)` uses the matching ISO path.

- [x] **Step 1: Add Linux/Windows ISO config values**

Define `linuxIsoPath`, `windows10IsoPath`, and `windows11IsoPath`.

- [x] **Step 2: Use per-disc ISO in descriptors**

`Boot from Disc` descriptors now inherit the inserted disc OS and its configured ISO path.

- [x] **Step 3: Run full build**

Run: `.\gradlew.bat build --no-daemon --max-workers=1 --console=plain --stacktrace`
Expected: `BUILD SUCCESSFUL`

## Self-Review

Spec coverage: Boot-device actions now own startup semantics, power controls no longer duplicate startup, and system discs can point to different ISO files.

Placeholder scan: No placeholder instructions remain.

Type consistency: `RESET`, per-disc ISO config names, and descriptor APIs match across tasks.
