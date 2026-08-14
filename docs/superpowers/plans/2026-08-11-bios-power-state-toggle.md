# BIOS Power State Toggle Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Change the BIOS power button from a generic `Power` label to a server-state-driven `Power On` / `Power Off` toggle.

**Architecture:** Keep VM state authoritative on the server. The BIOS screen requests state through a C2S packet, receives a S2C state packet, and updates only its label. BIOS actions still go through `ComputerBiosActions`; after power-related actions complete, the server sends the latest state back to the player.

**Tech Stack:** Minecraft Forge 1.20.1, Java 17, Forge networking, existing `VirtualMachineManager`.

## Global Constraints

- Client screen must not call `VirtualMachineManager`, QEMU, QMP, or process APIs.
- VM state shown in BIOS must come from the server.
- Packet handlers must validate sender, block position, and block entity type.
- VM operations remain asynchronous.
- Tests must not launch QEMU.

---

### Task 1: Add BIOS State Packets

**Files:**
- Create: `src/main/java/com/example/vm/network/ComputerBiosStateRequestPacket.java`
- Create: `src/main/java/com/example/vm/network/ComputerBiosStatePacket.java`
- Create: `src/main/java/com/example/vm/client/ComputerBiosClientStateHandler.java`
- Modify: `src/main/java/com/example/vm/network/ModNetwork.java`

**Interfaces:**
- Produces: `ModNetwork.requestBiosState(BlockPos pos): void`
- Produces: `ModNetwork.sendBiosState(ServerPlayer player, BlockPos pos, VirtualMachineState state): void`
- Produces: `ComputerBiosClientStateHandler.handle(ComputerBiosStatePacket packet): void`

- [x] **Step 1: Add C2S state request packet**

Encode `BlockPos`, validate sender and `ComputerBlockEntity`, query `VM_MANAGER.getState`, then send a state packet.

- [x] **Step 2: Add S2C state packet**

Encode `BlockPos` and `VirtualMachineState`; handle only on the client side through a client handler.

- [x] **Step 3: Register both packets**

Add both messages to the existing `SimpleChannel`.

### Task 2: Update BIOS Screen Power Label

**Files:**
- Modify: `src/main/java/com/example/vm/client/ComputerBiosScreen.java`

**Interfaces:**
- Produces: `ComputerBiosScreen.updateVmState(BlockPos pos, VirtualMachineState state): void`
- Power button label is `Power Off` for `RUNNING` or `STARTING`; otherwise `Power On`.

- [x] **Step 1: Store power button reference**

Keep a `Button powerButton` field and update its label when VM state changes.

- [x] **Step 2: Request state on init**

Call `ModNetwork.requestBiosState(menu.computerPos())` after creating widgets.

### Task 3: Push State After BIOS Actions

**Files:**
- Modify: `src/main/java/com/example/vm/computer/ComputerBiosActions.java`
- Modify: `build.gradle`

**Interfaces:**
- Power-related action completions send a state packet to the requesting player.
- Verification checks for `Power On`, `Power Off`, state packets, and no generic `Power` button.

- [x] **Step 1: Send state after create/start/stop and rejected power actions**

Use `ModNetwork.sendBiosState` from server callbacks.

- [x] **Step 2: Strengthen Gradle verification**

Check packet registration and screen labels.

- [x] **Step 3: Run full build**

Run: `.\gradlew.bat build --no-daemon --max-workers=1 --console=plain --stacktrace`
Expected: `BUILD SUCCESSFUL`

## Self-Review

Spec coverage: Power button label now reflects server VM state and toggles between explicit on/off labels.

Placeholder scan: No placeholder instructions remain.

Type consistency: Packet, network, screen, and service method names match across tasks.
