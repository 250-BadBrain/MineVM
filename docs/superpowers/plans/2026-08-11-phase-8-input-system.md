# Phase 8 Input System Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add asynchronous keyboard and mouse input routing from Minecraft-side services to VM backends, with Local QEMU using QMP `input-send-event`.

**Architecture:** Represent keyboard/mouse input as immutable records in `com.example.vm.input`. `InputService` queues input on a daemon executor and calls `VirtualMachineManager.sendInput`, which delegates to `VirtualMachineBackend.sendInput`. Local QEMU converts the event to QMP JSON and sends it through `QmpClient`.

**Tech Stack:** Minecraft Forge 1.20.1, Java 17, Gson JSON, QMP, Gradle JavaExec self-tests.

## Global Constraints

- Minecraft main thread must not perform QMP I/O.
- Input sending must be asynchronous.
- BlockEntity must not directly call QEMU or QMP.
- Tests must not launch QEMU or open a real QMP connection.
- Input events must be backend-neutral for future RemoteQemuBackend support.

---

### Task 1: Input Event Model

**Files:**
- Create: `src/main/java/com/example/vm/input/VirtualMachineInput.java`
- Create: `src/main/java/com/example/vm/input/KeyboardInput.java`
- Create: `src/main/java/com/example/vm/input/MouseButtonInput.java`
- Create: `src/main/java/com/example/vm/input/MouseMoveInput.java`
- Create: `src/main/java/com/example/vm/test/VirtualMachineInputSelfTest.java`

**Interfaces:**
- Produces: `VirtualMachineInput.computerId(): String`
- Produces: keyboard/button/move records with validation.

- [x] **Step 1: Add backend-neutral input records**

Create immutable records for keyboard keys, mouse button press/release, and absolute pointer movement.

- [x] **Step 2: Add validation self-test**

Assert valid records retain values and invalid values are rejected.

### Task 2: QMP Input Command Builder

**Files:**
- Create: `src/main/java/com/example/vm/qmp/QmpInputCommand.java`
- Create: `src/main/java/com/example/vm/test/QmpInputCommandSelfTest.java`

**Interfaces:**
- Produces: `QmpInputCommand.toJson(VirtualMachineInput input): String`

- [x] **Step 1: Convert keyboard input to QMP JSON**

Use QMP `input-send-event` with key down/up events.

- [x] **Step 2: Convert mouse input to QMP JSON**

Use button and absolute motion events with stable JSON.

### Task 3: Async Input Service And Backend Wiring

**Files:**
- Create: `src/main/java/com/example/vm/input/InputService.java`
- Modify: `src/main/java/com/example/vm/vm/backend/VirtualMachineBackend.java`
- Modify: `src/main/java/com/example/vm/vm/backend/NoopVirtualMachineBackend.java`
- Modify: `src/main/java/com/example/vm/vm/VirtualMachineManager.java`
- Modify: `src/main/java/com/example/vm/vm/backend/local/LocalQemuBackend.java`
- Modify: `src/main/java/com/example/vm/MineVM.java`
- Modify: `src/main/java/com/example/vm/test/VirtualMachineManagerSelfTest.java`
- Create: `src/main/java/com/example/vm/test/InputServiceSelfTest.java`
- Modify: `build.gradle`

**Interfaces:**
- Produces: `VirtualMachineManager.sendInput(VirtualMachineInput input): CompletableFuture<Void>`
- Produces: `InputService.send(VirtualMachineInput input): CompletableFuture<Void>`

- [x] **Step 1: Add backend and manager input methods**

Noop accepts input without external side effects; Local QEMU sends QMP input JSON.

- [x] **Step 2: Add async input service**

Input queue uses daemon thread `minevm-input-sender`.

- [x] **Step 3: Add verification task**

Run build and assert input classes, tests, async service, and QMP wiring exist.

## Self-Review

Spec coverage: This implements the Phase 8 input routing skeleton without coupling BlockEntity to QEMU and without blocking Minecraft thread.

Placeholder scan: No placeholder instructions remain.

Type consistency: All referenced classes and methods are defined in the tasks.
