# VM Boot Request Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Separate persistent VM configuration from the boot device selected for a single startup.

**Architecture:** Add a `VirtualMachineBootRequest` carrying computer id, boot device, and optional ISO path. Keep `start(String)` as the hard-disk default for compatibility, and add `start(VirtualMachineBootRequest)` for BIOS device boot actions. QEMU command construction consumes the request so `Boot VM Disk` never accidentally includes a system disc ISO.

**Tech Stack:** Java 17, Forge mod VM backend abstraction, QEMU command builder, Gradle self-tests.

## Global Constraints

- VM descriptors store hardware and guest profile, not a forced startup device.
- `Boot from Disc` must pass the configured disc ISO for that single start.
- `Boot VM Disk` must not include `-cdrom` or `-boot d`.
- Existing `start(String)` callers keep hard-disk boot behavior.
- Tests must not launch QEMU.

---

### Task 1: Add Boot Request Types

**Files:**
- Create: `src/main/java/com/example/vm/vm/VirtualMachineBootDevice.java`
- Create: `src/main/java/com/example/vm/vm/VirtualMachineBootRequest.java`
- Modify: `src/main/java/com/example/vm/vm/backend/VirtualMachineBackend.java`
- Modify: `src/main/java/com/example/vm/vm/VirtualMachineManager.java`

**Interfaces:**
- Produces: `VirtualMachineBootDevice.VM_DISK`
- Produces: `VirtualMachineBootDevice.SYSTEM_DISC`
- Produces: `VirtualMachineBootRequest.vmDisk(String computerId)`
- Produces: `VirtualMachineBootRequest.systemDisc(String computerId, String isoPath)`

- [x] **Step 1: Define boot device model**

Represent disk and disc as explicit enum values.

- [x] **Step 2: Add backend and manager overloads**

Keep `start(String)` delegating to VM disk boot; add boot-request overload.

### Task 2: Apply Boot Requests To Backends

**Files:**
- Modify: `src/main/java/com/example/vm/vm/backend/NoopVirtualMachineBackend.java`
- Modify: `src/main/java/com/example/vm/vm/backend/local/LocalQemuBackend.java`
- Modify: `src/main/java/com/example/vm/vm/backend/local/QemuCommandBuilder.java`
- Modify: `src/main/java/com/example/vm/computer/ComputerBiosActions.java`
- Modify: `src/main/java/com/example/vm/test/LocalQemuBackendSelfTest.java`
- Modify: `src/main/java/com/example/vm/test/VirtualMachineManagerSelfTest.java`

**Interfaces:**
- `QemuCommandBuilder.startCommand(descriptor, bootRequest)` includes `-cdrom` only for `SYSTEM_DISC`.
- BIOS `Boot from Disc` calls `VM_MANAGER.start(VirtualMachineBootRequest.systemDisc(...))`.
- BIOS disk boot and power-on call hard-disk start.

- [x] **Step 1: Update QEMU command builder**

Move ISO startup logic from descriptor-only command creation into boot-request command creation.

- [x] **Step 2: Update BIOS actions**

Pass disc ISO only for the disc boot action.

- [x] **Step 3: Update tests**

Assert disc boot includes ISO and disk boot omits ISO.

## Self-Review

Spec coverage: Startup device is explicit per action, matching BIOS behavior and preventing accidental disc boot reuse.

Placeholder scan: No placeholder instructions remain.

Type consistency: Boot request names and overloads match across manager, backend, and QEMU builder.
