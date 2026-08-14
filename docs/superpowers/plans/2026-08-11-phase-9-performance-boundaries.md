# Phase 9 Performance Boundaries Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Harden thread and process boundaries so VM, screen, and input work cannot silently pile up on unbounded queues or block on external process output.

**Architecture:** Introduce a shared bounded daemon executor factory and use it for VM manager, screen receiver, and input sender services. Adjust QEMU process launching to discard stdout/stderr where logs are not consumed, avoiding a filled pipe blocking the external process.

**Tech Stack:** Minecraft Forge 1.20.1, Java 17 `ThreadPoolExecutor`, Gradle JavaExec self-tests.

## Global Constraints

- Minecraft thread must only handle block state, UI, and rendering.
- VM communication, screen receiving, and input sending must run off the Minecraft thread.
- Async service queues must be bounded.
- QEMU process output must not block unmanaged pipes.
- Tests must not launch QEMU.

---

### Task 1: Bounded Async Executor Factory

**Files:**
- Create: `src/main/java/com/example/vm/concurrent/ComputerExecutors.java`
- Create: `src/main/java/com/example/vm/test/ComputerExecutorsSelfTest.java`

**Interfaces:**
- Produces: `ComputerExecutors.singleThread(String threadName, int queueCapacity): ExecutorService`

- [x] **Step 1: Add bounded daemon executor factory**

Use `ThreadPoolExecutor` with one daemon thread, bounded `ArrayBlockingQueue`, and abort rejection.

- [x] **Step 2: Add self-test**

Assert daemon thread name and bounded queue rejection.

### Task 2: Apply Bounded Executors

**Files:**
- Modify: `src/main/java/com/example/vm/vm/VirtualMachineManager.java`
- Modify: `src/main/java/com/example/vm/screen/ScreenService.java`
- Modify: `src/main/java/com/example/vm/input/InputService.java`

**Interfaces:**
- Consumes: `ComputerExecutors.singleThread(...)`

- [x] **Step 1: Replace unbounded single-thread executors**

Use explicit queue capacities for VM manager, screen receiver, and input sender.

- [x] **Step 2: Keep close/recovery behavior**

Existing self-tests continue to prove services recover after close.

### Task 3: QEMU Process Output Safety

**Files:**
- Modify: `src/main/java/com/example/vm/vm/backend/local/LocalQemuBackend.java`
- Modify: `build.gradle`

**Interfaces:**
- QEMU start process output uses `ProcessBuilder.Redirect.DISCARD`.
- qemu-img process output uses `ProcessBuilder.Redirect.DISCARD`.

- [x] **Step 1: Discard unmanaged process output**

Avoid external process blocking on full stdout/stderr pipes.

- [x] **Step 2: Add Phase 9 verification task**

Check bounded executor usage, no unbounded executor factory calls in services, and process output discard.

## Self-Review

Spec coverage: This plan enforces async bounded queues and improves QEMU process safety without adding tick work or gameplay shortcuts.

Placeholder scan: No placeholder instructions remain.

Type consistency: All referenced classes and methods match the planned implementation.
