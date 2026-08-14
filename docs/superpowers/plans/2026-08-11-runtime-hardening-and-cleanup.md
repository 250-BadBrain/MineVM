# Runtime Hardening And Cleanup Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Harden the local VM runtime for real QEMU use and clean the BIOS/server action code after the boot-request refactor.

**Architecture:** Keep all VM process work in `VirtualMachineManager` and backend threads. Treat each computer as owning a managed VM directory under the configured storage root. BIOS and block interactions validate VM state before disc ejection. QEMU command generation makes boot order explicit and process lifecycle updates backend state when QEMU exits.

**Tech Stack:** Minecraft Forge 1.20.1, Java 17, QEMU, QMP, Gradle self-tests.

## Global Constraints

- Minecraft main thread must not launch QEMU or block on VM I/O.
- `ComputerBlockEntity` stores data only; VM lifecycle remains in manager/backend.
- Running VMs must not allow system disc ejection.
- VM files must live under the managed storage directory.
- `Boot from Disc` and `Boot VM Disk` must produce different QEMU boot commands.
- Tests must not launch QEMU.

---

### Task 1: Protect Disc Ejection While Running

**Files:**
- Create: `src/main/java/com/example/vm/computer/ComputerDiscActions.java`
- Modify: `src/main/java/com/example/vm/computer/ComputerBiosActions.java`
- Modify: `src/main/java/com/example/vm/block/ComputerBlock.java`
- Modify: `build.gradle`

**Interfaces:**
- Produces: `ComputerDiscActions.ejectIfStopped(MinecraftServer server, Player player, ComputerBlockEntity computer, BlockPos pos): void`
- BIOS and block shift-right-click both use VM state before ejecting.

- [x] **Step 1: Add shared disc action helper**

Asynchronously query VM state and eject only when not running/starting/stopping.

- [x] **Step 2: Route BIOS and block ejection through helper**

Remove duplicate direct ejection code.

### Task 2: Managed Per-Computer VM Directory

**Files:**
- Create: `src/main/java/com/example/vm/vm/backend/local/LocalVmPaths.java`
- Modify: `src/main/java/com/example/vm/vm/backend/local/QemuCommandBuilder.java`
- Modify: `src/main/java/com/example/vm/vm/backend/local/LocalQemuPreflight.java`
- Modify: `src/main/java/com/example/vm/test/LocalQemuBackendSelfTest.java`

**Interfaces:**
- Produces: `LocalVmPaths.vmDirectory(descriptor): Path`
- Produces: `LocalVmPaths.diskPath(descriptor): Path`
- QEMU disk and OVMF vars use managed per-computer paths.

- [x] **Step 1: Add path helper**

Normalize all VM files under the configured storage root and reject path traversal.

- [x] **Step 2: Update disk and firmware paths**

Use per-computer directory for qcow2 and OVMF vars.

### Task 3: QEMU Boot Command Hardening

**Files:**
- Modify: `src/main/java/com/example/vm/vm/backend/local/QemuCommandBuilder.java`
- Modify: `src/main/java/com/example/vm/test/LocalQemuBackendSelfTest.java`

**Interfaces:**
- Disk boot includes `-boot order=c`.
- Disc boot includes `-cdrom <iso>` and `-boot order=d`.
- Both include stable QMP/VNC/name options.

- [x] **Step 1: Make boot order explicit**

Avoid relying on QEMU defaults.

### Task 4: Local Process Lifecycle State

**Files:**
- Modify: `src/main/java/com/example/vm/vm/backend/local/LocalQemuBackend.java`
- Modify: `src/main/java/com/example/vm/test/LocalQemuBackendSelfTest.java`

**Interfaces:**
- When a launched process exits naturally, backend state transitions from `RUNNING` to `STOPPED`.
- Backend removes dead process handles.

- [x] **Step 1: Register process exit callback**

Use `process.onExit()` without blocking Minecraft.

### Task 5: BIOS Action Cleanup

**Files:**
- Modify: `src/main/java/com/example/vm/computer/ComputerBiosActions.java`
- Modify: `build.gradle`

**Interfaces:**
- Reduce duplicated state checks through helper methods.
- Preserve all server validation and async VM manager calls.

- [x] **Step 1: Extract state predicates and messages**

Keep behavior stable while making later feature work easier.

### Task 6: Full Verification

**Files:**
- Modify: `build.gradle`

**Interfaces:**
- Verification checks managed paths, explicit boot order, ejection guard, process exit handling, and no client QEMU calls.

- [x] **Step 1: Run full build**

Run: `.\gradlew.bat build --no-daemon --max-workers=1 --console=plain --stacktrace`
Expected: `BUILD SUCCESSFUL`

## Self-Review

Spec coverage: The plan hardens runtime process boundaries, storage management, disc lifecycle behavior, QEMU commands, and code maintainability.

Placeholder scan: No placeholder instructions remain.

Type consistency: Helper class names and methods are used consistently across tasks.
