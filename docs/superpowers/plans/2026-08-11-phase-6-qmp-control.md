# Phase 6 QMP Control Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add QEMU Machine Protocol control so local VM stop/restart operations can be sent as QMP JSON commands instead of direct process termination.

**Architecture:** Keep QMP isolated in `com.example.vm.qmp`. `LocalQemuBackend` configures QEMU with a per-computer TCP QMP endpoint and calls a small QMP client from the VM manager background thread. Minecraft blocks and block entities continue to depend only on `VirtualMachineManager`.

**Tech Stack:** Minecraft Forge 1.20.1, Java 17 sockets, Gson for JSON, Gradle JavaExec self-tests.

## Global Constraints

- Minecraft main thread must not perform QMP I/O.
- BlockEntity must not directly call QEMU or QMP.
- QMP commands are JSON.
- Tests must not launch QEMU or open a real QMP connection.
- Local QEMU remains opt-in by config.

---

### Task 1: QMP Command Model

**Files:**
- Create: `src/main/java/com/example/vm/qmp/QmpCommand.java`
- Create: `src/main/java/com/example/vm/qmp/QmpEndpoint.java`
- Create: `src/main/java/com/example/vm/test/QmpCommandSelfTest.java`

**Interfaces:**
- Produces: `QmpCommand.capabilities(): String`
- Produces: `QmpCommand.execute(String command): String`
- Produces: `QmpEndpoint.forComputer(String computerId): QmpEndpoint`

- [x] **Step 1: Build stable JSON command strings**

Implement `{"execute":"qmp_capabilities"}`, `{"execute":"system_powerdown"}`, and `{"execute":"system_reset"}` string builders with Gson.

- [x] **Step 2: Build deterministic local endpoints**

Map computer ids to `127.0.0.1` ports in a bounded range to avoid hardcoding a single QMP port.

- [x] **Step 3: Add self-test**

Assert JSON strings and endpoint stability.

### Task 2: QMP Client

**Files:**
- Create: `src/main/java/com/example/vm/qmp/QmpClient.java`

**Interfaces:**
- Produces: `send(QmpEndpoint endpoint, String execute): void`

- [x] **Step 1: Implement socket exchange**

Open a TCP socket, read greeting, send capabilities, read response, send command, read response.

- [x] **Step 2: Keep timeout bounded**

Use short connect/read timeouts so failed QMP operations do not hang the VM manager thread.

### Task 3: Local QEMU Backend QMP Wiring

**Files:**
- Modify: `src/main/java/com/example/vm/vm/backend/VirtualMachineBackend.java`
- Modify: `src/main/java/com/example/vm/vm/backend/NoopVirtualMachineBackend.java`
- Modify: `src/main/java/com/example/vm/vm/VirtualMachineManager.java`
- Modify: `src/main/java/com/example/vm/vm/backend/local/QemuCommandBuilder.java`
- Modify: `src/main/java/com/example/vm/vm/backend/local/LocalQemuBackend.java`
- Modify: `src/main/java/com/example/vm/test/LocalQemuBackendSelfTest.java`
- Modify: `src/main/java/com/example/vm/test/VirtualMachineManagerSelfTest.java`
- Modify: `build.gradle`

**Interfaces:**
- Produces: `VirtualMachineBackend.restart(String computerId): VirtualMachineHandle`
- Produces: `VirtualMachineManager.restart(String computerId): CompletableFuture<VirtualMachineHandle>`

- [x] **Step 1: Add restart to backend and manager**

Noop restart moves existing VMs to `RUNNING`.

- [x] **Step 2: Add QMP endpoint to QEMU start command**

Include `-qmp tcp:127.0.0.1:<port>,server,nowait`.

- [x] **Step 3: Use QMP for stop and restart in local backend**

Stop sends `system_powerdown`; restart sends `system_reset`.

- [x] **Step 4: Add verification task**

Check QMP classes, self-test, command args, and package entries.

## Self-Review

Spec coverage: This plan implements QMP JSON control while preserving async VM manager boundaries and default Local QEMU opt-in behavior.

Placeholder scan: No placeholder instructions remain.

Type consistency: Method names and return types match the existing manager/backend style.
