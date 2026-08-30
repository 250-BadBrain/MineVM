# Controlled Local QEMU Runtime Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let the mod switch from the safe Noop backend to `LocalQemuBackend` only when config explicitly enables it, then expose create/start/stop through the existing computer block interaction.

**Architecture:** Add a Forge common config that defaults to safe Noop behavior. Build backend selection in a config/factory class, let `VirtualMachineManager` replace its backend on server start, and keep `ComputerBlock` dependent only on manager/config abstractions rather than `LocalQemuBackend`.

**Tech Stack:** Minecraft Forge 1.20.1, Java 17, QEMU local process backend, Gradle JavaExec self-tests.

## Global Constraints

- Minecraft main thread must not start QEMU or perform blocking VM work.
- BlockEntity must not directly call QEMU.
- Default gameplay must not start external processes.
- Local QEMU may be used only when explicitly enabled in config.
- Tests must not launch QEMU.

---

### Task 1: Runtime Config And Backend Factory

**Files:**
- Create: `src/main/java/com/example/vm/config/ComputerRuntimeConfig.java`
- Modify: `src/main/java/com/example/vm/MineVM.java`
- Test: `src/main/java/com/example/vm/test/ComputerRuntimeConfigSelfTest.java`

**Interfaces:**
- Produces: `ComputerRuntimeConfig.SPEC`
- Produces: `ComputerRuntimeConfig.createBackend(): VirtualMachineBackend`
- Produces: `ComputerRuntimeConfig.descriptorFor(ComputerBlockEntity computer): VirtualMachineDescriptor`

- [x] **Step 1: Define safe Forge config defaults**

Defaults: `enableLocalQemu=false`, `qemuSystemPath=D:\qemu\qemu-system-x86_64.exe`, `qemuImgPath=D:\qemu\qemu-img.exe`, `vmStorageDirectory=run\minevm-vms`, `accelerator=whpx`, `diskSizeGb=20`, `isoPath=""`.

- [x] **Step 2: Register config in the mod entry**

Register the common config and configure the manager on server start.

- [x] **Step 3: Add a self-test for default safety**

Assert the default backend is Noop and descriptor creation preserves computer hardware values.

### Task 2: Replaceable VM Manager Backend

**Files:**
- Modify: `src/main/java/com/example/vm/vm/VirtualMachineManager.java`
- Modify: `src/main/java/com/example/vm/test/VirtualMachineManagerSelfTest.java`

**Interfaces:**
- Produces: `VirtualMachineManager.replaceBackend(VirtualMachineBackend backend): void`

- [x] **Step 1: Allow backend replacement**

Synchronize backend replacement, close the old backend, and keep async execution available after replacement.

- [x] **Step 2: Test replacement**

Assert a manager can replace backend after use and still process async operations.

### Task 3: In-Game VM Toggle

**Files:**
- Modify: `src/main/java/com/example/vm/block/ComputerBlock.java`
- Modify: `build.gradle`

**Interfaces:**
- Consumes: `ComputerRuntimeConfig.descriptorFor(...)`
- Produces: right-click behavior:
  - Normal right-click shows status.
  - Shift-right-click checks state asynchronously, then creates, starts, or stops the VM.

- [x] **Step 1: Implement shift-right-click state machine**

`NOT_CREATED -> create`, `STOPPED -> start`, `RUNNING -> stop`, busy states send a message.

- [x] **Step 2: Add Gradle verification**

Check the config class, tests, and interaction routing are present, and ensure tests do not start QEMU.

- [x] **Step 3: Run build**

Run `.\gradlew.bat build --no-daemon --max-workers=1 --console=plain --stacktrace` and expect `BUILD SUCCESSFUL`.

## Self-Review

Spec coverage: The plan preserves default safety, exposes opt-in local QEMU, keeps QEMU outside block entity logic, and gives a game-visible start/stop path.

Placeholder scan: No placeholder instructions remain.

Type consistency: All class and method names match the planned implementation.
