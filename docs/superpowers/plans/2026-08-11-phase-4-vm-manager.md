# Phase 4 VM Manager Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a VM Manager layer that owns VM lifecycle state transitions without calling QEMU or any external process.

**Architecture:** Introduce `VirtualMachineBackend` as the boundary between Minecraft code and future VM implementations. `VirtualMachineManager` runs backend calls on a dedicated executor and exposes `CompletableFuture` APIs; Phase 4 uses a `NoopVirtualMachineBackend` for safe local behavior.

**Tech Stack:** Minecraft Forge 1.20.1, Java 17, Gradle 8.8, `CompletableFuture`, `ExecutorService`.

## Global Constraints

- Minecraft version: `1.20.1`
- Mod loader: Forge
- Java version: JDK 17
- Phase 4 only: no QEMU process launch, no qcow2 creation, no QMP, no screen/input system.
- BlockEntity must not directly call VM backend methods.
- VM operations must be asynchronous and off the Minecraft main thread.

---

### Task 1: Define VM Lifecycle Types

**Files:**
- Create: `src/main/java/com/example/vm/vm/VirtualMachineState.java`
- Create: `src/main/java/com/example/vm/vm/VirtualMachineDescriptor.java`
- Create: `src/main/java/com/example/vm/vm/VirtualMachineHandle.java`
- Create: `src/main/java/com/example/vm/vm/backend/VirtualMachineBackend.java`

**Interfaces:**
- Produces: `VirtualMachineState`
- Produces: `VirtualMachineDescriptor.from(ComputerBlockEntity)`
- Produces: `VirtualMachineBackend#create(VirtualMachineDescriptor)`
- Produces: `VirtualMachineBackend#start(String)`
- Produces: `VirtualMachineBackend#stop(String)`
- Produces: `VirtualMachineBackend#getState(String)`

- [x] Add lifecycle enum and immutable descriptor/handle records.
- [x] Define backend interface with no QEMU-specific types.

### Task 2: Add Safe Manager And Noop Backend

**Files:**
- Create: `src/main/java/com/example/vm/vm/VirtualMachineManager.java`
- Create: `src/main/java/com/example/vm/vm/backend/NoopVirtualMachineBackend.java`
- Modify: `src/main/java/com/example/vm/ComputerInMinecraft.java`

**Interfaces:**
- Produces: `VirtualMachineManager#create(VirtualMachineDescriptor)`
- Produces: `VirtualMachineManager#start(String)`
- Produces: `VirtualMachineManager#stop(String)`
- Produces: `VirtualMachineManager#getState(String)`
- Produces: `ComputerInMinecraft.VM_MANAGER`

- [x] Run all backend work on one named background executor.
- [x] Register lifecycle shutdown hook on Forge server stopping event.
- [x] Keep `NoopVirtualMachineBackend` in-memory only.

### Task 3: Add Verification

**Files:**
- Create: `src/main/java/com/example/vm/test/VirtualMachineManagerSelfTest.java`
- Modify: `build.gradle`

**Interfaces:**
- Consumes: `NoopVirtualMachineBackend`
- Consumes: `VirtualMachineManager`

- [x] Add a self-test main class that verifies create/start/stop state transitions and async execution thread name.
- [x] Add `verifyPhase4VmManager` Gradle task to run the self-test and enforce no process/QEMU calls.
- [x] Run `gradlew build --no-daemon --max-workers=1`.
