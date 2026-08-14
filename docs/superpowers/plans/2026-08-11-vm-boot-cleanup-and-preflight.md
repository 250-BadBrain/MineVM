# VM Boot Cleanup And Preflight Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Finish the boot-media split, add local QEMU preflight validation, and clean BIOS action naming.

**Architecture:** `VirtualMachineDescriptor` becomes persistent hardware/guest metadata only; ISO paths live exclusively in `VirtualMachineBootRequest`. The local QEMU backend validates executables, storage directories, and system-disc ISO paths before launching external processes. BIOS actions use explicit action names (`POWER_OFF`, not generic `POWER`) and stay server-authoritative.

**Tech Stack:** Minecraft Forge 1.20.1, Java 17, QEMU command builder, Gradle self-tests.

## Global Constraints

- No Minecraft main-thread QEMU work.
- Client screen must not call VM manager, QEMU, QMP, or process APIs.
- System-disc ISO path is per boot request, not persistent VM descriptor state.
- `Boot VM Disk` must not include `-cdrom` or `-boot d`.
- Local backend must fail with clear messages before launching QEMU when configured files are missing.
- Tests must not launch QEMU.

---

### Task 1: Remove ISO From Persistent VM Descriptor

**Files:**
- Modify: `src/main/java/com/example/vm/vm/VirtualMachineDescriptor.java`
- Modify: `src/main/java/com/example/vm/config/ComputerRuntimeConfig.java`
- Modify: `src/main/java/com/example/vm/computer/ComputerBiosActions.java`
- Modify: `src/main/java/com/example/vm/test/ComputerRuntimeConfigSelfTest.java`
- Modify: `src/main/java/com/example/vm/test/LocalQemuBackendSelfTest.java`

**Interfaces:**
- `VirtualMachineDescriptor` no longer exposes `isoPath()`.
- `ComputerRuntimeConfig.isoPathFor(GuestOperatingSystem)` remains the disc-to-ISO lookup.
- `Boot from Disc` creates `VirtualMachineBootRequest.systemDisc(computerId, ComputerRuntimeConfig.isoPathFor(os))`.

- [x] **Step 1: Update descriptor record**

Remove `isoPath` from the canonical record and constructors.

- [x] **Step 2: Move BIOS disc ISO lookup to boot request**

Use `ComputerRuntimeConfig.isoPathFor` directly in `ComputerBiosActions`.

### Task 2: Add Local QEMU Preflight

**Files:**
- Create: `src/main/java/com/example/vm/vm/backend/local/LocalQemuPreflight.java`
- Modify: `src/main/java/com/example/vm/vm/backend/local/LocalQemuBackend.java`
- Modify: `src/main/java/com/example/vm/test/LocalQemuBackendSelfTest.java`

**Interfaces:**
- Produces: `LocalQemuPreflight.validateTools(LocalQemuConfiguration): void`
- Produces: `LocalQemuPreflight.validateBootRequest(VirtualMachineBootRequest): void`

- [x] **Step 1: Validate QEMU executable paths before disk creation**

Reject missing `qemu-system-x86_64.exe` and `qemu-img.exe` with clear messages.

- [x] **Step 2: Validate system-disc ISO path before start**

Reject blank or missing ISO paths for `SYSTEM_DISC`.

### Task 3: Clean BIOS Action Names And Verification

**Files:**
- Modify: `src/main/java/com/example/vm/computer/ComputerBiosAction.java`
- Modify: `src/main/java/com/example/vm/computer/ComputerBiosActions.java`
- Modify: `src/main/java/com/example/vm/client/ComputerBiosScreen.java`
- Modify: `build.gradle`

**Interfaces:**
- Replace generic `POWER` with explicit `POWER_OFF`.
- Verification checks descriptor has no `isoPath` member and QEMU preflight is packaged.

- [x] **Step 1: Rename action**

Use `POWER_OFF` for the running-state button.

- [x] **Step 2: Run full build**

Run: `.\gradlew.bat build --no-daemon --max-workers=1 --console=plain --stacktrace`
Expected: `BUILD SUCCESSFUL`

## Self-Review

Spec coverage: Boot media is now request-scoped, QEMU startup has preflight validation, and BIOS button/action names match the UI behavior.

Placeholder scan: No placeholder instructions remain.

Type consistency: Descriptor, boot request, BIOS action, backend, and tests use consistent names.
