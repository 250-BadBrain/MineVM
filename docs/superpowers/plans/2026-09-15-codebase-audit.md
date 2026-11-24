# MineVM Codebase Audit Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Audit all MineVM production code, fix concrete lifecycle and media-handling defects, and verify the shipped JAR.

**Architecture:** Review the source tree by subsystem: world interaction and NBT, network authorization and client control, local QEMU/QMP process management, and VNC frame delivery. Preserve the existing single-JAR configuration model and add regression coverage only for defects that can be isolated without a running Minecraft client.

**Tech Stack:** Forge 1.20.1, Java 17, QEMU QMP, Gradle 8.8, existing main-method self-tests.

## Global Constraints

- Do not modify or discard user-authored worktree changes unrelated to a verified defect.
- All player inventory and block-entity changes execute on the Minecraft server thread.
- QMP errors for requested media operations must reach the gameplay interaction instead of being silently swallowed.
- Source code line counts exclude Gradle output, generated files, and binary resources.

---

### Task 1: Audit persistent world state and asynchronous interactions

**Files:**
- Modify: `src/main/java/com/example/vm/block/FloppyDriveBlock.java`
- Modify: `src/main/java/com/example/vm/blockentity/FloppyDriveBlockEntity.java`
- Test: `src/main/java/com/example/vm/test/VirtualMachineManagerSelfTest.java`

**Interfaces:**
- Consumes `FloppyDriveBlockEntity.media()` and `VirtualMachineManager.changeBootFloppyMedium`.
- Produces media changes that preserve the held item when VM-side insertion fails.

- [ ] **Step 1: Verify the asynchronous state transitions**

Inspect every `whenComplete` callback in `FloppyDriveBlock` and confirm that it returns to `server.execute` before mutating player inventory or block entity state.

- [ ] **Step 2: Add the focused regression assertion**

```java
assertHandle(
        manager.changeBootFloppyMedium("computer-test", FloppyMedia.blank("fca6d5ca-5a1c-4c81-89a1-d611c3aab919")).get(10, TimeUnit.SECONDS),
        "computer-test",
        VirtualMachineState.RUNNING
);
```

- [ ] **Step 3: Fix only confirmed lifecycle defects**

Use `sendQmpJson` for user-visible floppy ejection and retain the item unless the QMP future succeeds. Do not alter world state from the completion thread.

- [ ] **Step 4: Run the focused verification**

Run: `./gradlew.bat runVirtualMachineManagerSelfTest --console=plain`

Expected: `BUILD SUCCESSFUL`.

### Task 2: Audit QEMU, QMP, path, and process boundaries

**Files:**
- Modify: `src/main/java/com/example/vm/qmp/QmpCommand.java`
- Modify: `src/main/java/com/example/vm/vm/backend/local/LocalQemuBackend.java`
- Modify: `src/main/java/com/example/vm/vm/backend/local/QemuCommandBuilder.java`
- Test: `src/main/java/com/example/vm/test/QmpCommandSelfTest.java`
- Test: `src/main/java/com/example/vm/test/LocalQemuBackendSelfTest.java`

**Interfaces:**
- Consumes `QmpCommand.changeMedium(deviceId, imagePath, readOnly)`.
- Produces QEMU 11 `device` plus `read-only-mode` exchange commands and a legacy `read-only` fallback.

- [ ] **Step 1: Inspect QMP runtime schema**

Run QEMU with a temporary 1.44 MB floppy image and inspect `query-qmp-schema` plus `query-block`. Confirm `blockdev-change-medium` accepts `device`, `filename`, `format`, and `read-only-mode`.

- [ ] **Step 2: Add exact JSON regression tests**

```java
assertEquals(
        QmpCommand.changeMedium("boot_floppy", Path.of("E:\\MinecraftVMs\\computer-test\\floppies\\disk.img"), false),
        "{\"execute\":\"blockdev-change-medium\",\"arguments\":{\"device\":\"boot_floppy\",...}}"
);
```

- [ ] **Step 3: Fix confirmed command incompatibilities**

Use the removable device name from `query-block`, never the opaque QMP block-node ID. Retry only when a legacy QEMU rejects `read-only-mode` as an unexpected parameter.

- [ ] **Step 4: Run QMP and command-builder verification**

Run: `./gradlew.bat runQmpCommandSelfTest runLocalQemuBackendSelfTest --console=plain`

Expected: `BUILD SUCCESSFUL`.

### Task 3: Audit network, rendering, VNC, and package integrity

**Files:**
- Modify only files with a reproducible defect discovered during inspection.
- Test: `src/main/java/com/example/vm/test/QmpClientSelfTest.java`
- Test: `src/main/java/com/example/vm/test/VncFrameReceiverSelfTest.java`
- Test: `src/main/java/com/example/vm/test/ImmersiveControlClientSelfTest.java`

**Interfaces:**
- Consumes server-side packet sender checks, VNC receiver frame callbacks, and client screen-control state.
- Produces an unchanged public gameplay protocol unless a specific validation or lifecycle bug is verified.

- [ ] **Step 1: Inspect all packet handlers and file-system writes**

Confirm each packet handler obtains `context.getSender()`, validates target ownership/range through existing interaction methods, and calls `setPacketHandled(true)`. Confirm every VM-derived path remains under `LocalVmPaths.storageRoot`.

- [ ] **Step 2: Run full package verification**

Run: `./gradlew.bat build --console=plain`

Expected: `BUILD SUCCESSFUL` with all project verification tasks passing.

- [ ] **Step 3: Count source lines**

Run:

```powershell
$java = (Get-ChildItem -LiteralPath 'src/main/java' -Recurse -Filter '*.java' | Get-Content | Measure-Object -Line).Lines
$gradle = (Get-ChildItem -LiteralPath . -File -Filter '*.gradle' | Get-Content | Measure-Object -Line).Lines
$resources = (Get-ChildItem -LiteralPath 'src/main/resources' -Recurse -Include '*.json','*.toml','*.mcmeta' | Get-Content | Measure-Object -Line).Lines
"Java=$java Gradle=$gradle Resources=$resources Total=$($java + $gradle + $resources)"
```

- [ ] **Step 4: Commit only when requested**

```text
git add src/main/java src/main/resources docs/superpowers/plans
git commit -m "fix: harden runtime media handling"
```
