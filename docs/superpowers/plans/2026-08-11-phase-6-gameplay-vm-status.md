# Phase 6 Gameplay VM Status Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a small in-game verification loop for the computer block: right-click shows computer data and VM state, shift-right-click asynchronously registers the VM through `VirtualMachineManager`.

**Architecture:** Keep Minecraft block interaction on the server thread and dispatch VM work through the existing async manager. Keep message formatting in a pure helper so it can be self-tested without launching Minecraft or QEMU. Default backend remains `NoopVirtualMachineBackend`, so this phase does not start external processes in-game.

**Tech Stack:** Minecraft Forge 1.20.1, Java 17, Gradle verification tasks, existing Noop VM backend.

## Global Constraints

- Minecraft main thread must not start QEMU or block on external process work.
- BlockEntity must not directly manage QEMU.
- VM operations must go through `VirtualMachineManager`.
- Phase 6 must provide game-visible behavior.
- Tests must not launch QEMU.

---

### Task 1: Status Message Formatter

**Files:**
- Create: `src/main/java/com/example/vm/computer/ComputerStatusMessage.java`
- Create: `src/main/java/com/example/vm/test/ComputerStatusMessageSelfTest.java`
- Modify: `build.gradle`

**Interfaces:**
- Consumes: `ComputerHardwareConfiguration`, `VirtualMachineState`
- Produces: `ComputerStatusMessage.describe(String computerId, ComputerHardwareConfiguration configuration, VirtualMachineState state): String`

- [x] **Step 1: Add a pure formatter**

Create a formatter that returns one stable line of text with id, CPU, memory, disk, and state.

- [x] **Step 2: Add a self-test**

Run the formatter with fixed values and assert the full string.

- [x] **Step 3: Wire the self-test into Gradle**

Add a JavaExec task and include it in `check`.

### Task 2: Computer Block Interaction

**Files:**
- Modify: `src/main/java/com/example/vm/block/ComputerBlock.java`
- Modify: `src/main/resources/assets/minevm/lang/en_us.json`
- Modify: `build.gradle`

**Interfaces:**
- Consumes: `MineVM.VM_MANAGER`, `VirtualMachineDescriptor.from(ComputerBlockEntity)`, `ComputerStatusMessage.describe(...)`
- Produces: right-click behavior:
  - Normal right-click: async `getState`, then sends status message.
  - Shift-right-click: async `create`, then sends registration message with resulting state.

- [x] **Step 1: Implement server-side use handler**

Return immediately on the Minecraft thread, dispatch VM manager calls asynchronously, and post chat output back to the server thread.

- [x] **Step 2: Add verification rules**

Verify `ComputerBlock` has the expected interaction method and that Phase 6 classes are packaged.

- [x] **Step 3: Run build**

Run `.\gradlew.bat build --no-daemon --max-workers=1 --console=plain --stacktrace` and expect `BUILD SUCCESSFUL`.

## Self-Review

Spec coverage: This plan adds game-visible validation without starting QEMU and preserves the backend boundary.

Placeholder scan: No placeholder instructions remain.

Type consistency: All referenced classes and method names match the current codebase.
