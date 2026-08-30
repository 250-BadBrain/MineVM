# Remove BIOS GUI Fallback Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the BIOS GUI fallback with fully in-world screen control, improve laptop screen geometry, and restrict disc insertion to computer cases/laptops.

**Architecture:** Keep `ComputerBiosActionPacket` and `ComputerBiosActions` as the only BIOS command path. `StartScreenControlPacket` carries both the visible screen position and the actual computer position, so monitor clicks can operate the connected case while laptop clicks operate itself. Client-side immersive control owns selected BIOS action state and sends actions on left click / enter.

**Tech Stack:** Minecraft Forge 1.20.1, Java 17, block entity renderers, simple channel packets, JSON block models, Gradle verification.

## Global Constraints

- HDMI/USB connection remains sneak-right-click compatible.
- Normal empty-hand right-click remains immersive control entry.
- No `NetworkHooks.openScreen`, `MenuScreens.register`, or GUI fallback remains for BIOS.
- BIOS actions must still execute server-side through `ComputerBiosActions`.
- Full verification command: `./gradlew.bat build --no-daemon --max-workers=1 --console=plain --stacktrace`.

---

### Task 1: Screen Control Packet Carries Computer Pos

**Files:**
- Modify: `src/main/java/com/example/vm/network/StartScreenControlPacket.java`
- Modify: `src/main/java/com/example/vm/network/ModNetwork.java`
- Modify: `src/main/java/com/example/vm/block/ComputerMonitorBlock.java`
- Modify: `src/main/java/com/example/vm/block/ComputerBlock.java`

**Interfaces:**
- Produces: `StartScreenControlPacket(BlockPos screenPos, BlockPos computerPos, String computerId, boolean laptop)`
- Produces: `ModNetwork.startScreenControl(ServerPlayer, BlockPos, BlockPos, String, boolean)`

- [ ] Add `computerPos` to packet encode/decode.
- [ ] Desktop monitor sends screen pos = monitor, computer pos = linked case.
- [ ] Laptop sends both positions as laptop block pos.

### Task 2: Client BIOS Screen Controls

**Files:**
- Modify: `src/main/java/com/example/vm/client/ImmersiveControlClient.java`
- Modify: `src/main/java/com/example/vm/client/ClientForgeEvents.java`
- Modify: `src/main/java/com/example/vm/client/ScreenSurfaceRenderer.java`

**Interfaces:**
- Produces: `ImmersiveControlClient.selectedAction()`
- Produces: `ImmersiveControlClient.activateSelectedAction()`
- Produces: mouse left click / enter / space sends `ModNetwork.sendBiosAction(activeComputerPos, selectedAction)`

- [ ] Add action selection state.
- [ ] Cycle selection with wheel, arrow keys, and tab.
- [ ] Activate selected action on left click, enter, or space.
- [ ] Render selected option highlighted on the in-world screen.

### Task 3: Delete GUI Fallback

**Files:**
- Delete: `src/main/java/com/example/vm/client/ComputerBiosScreen.java`
- Delete: `src/main/java/com/example/vm/menu/ComputerBiosMenu.java`
- Delete: `src/main/java/com/example/vm/registry/ModMenus.java`
- Delete: `src/main/java/com/example/vm/client/ComputerBiosClientStateHandler.java`
- Delete: `src/main/java/com/example/vm/network/ComputerBiosStatePacket.java`
- Delete: `src/main/java/com/example/vm/network/ComputerBiosStateRequestPacket.java`
- Modify: `src/main/java/com/example/vm/MineVM.java`
- Modify: `src/main/java/com/example/vm/client/ClientModEvents.java`
- Modify: `src/main/java/com/example/vm/computer/ComputerBiosActions.java`

**Interfaces:**
- Removes: menu registration, GUI opening, state request/response packet registration.

- [ ] Remove menu imports and registration.
- [ ] Remove state packet registration and helper methods.
- [ ] Remove state packet sends from BIOS action handler.
- [ ] Remove GUI screen resource key if unused.

### Task 4: Restrict Disc Insertion To Computers

**Files:**
- Modify: `src/main/java/com/example/vm/block/ComputerMonitorBlock.java`
- Modify: `src/main/java/com/example/vm/block/ComputerBlock.java`

**Interfaces:**
- Produces: monitor rejects `SystemDiscItem` with a message and never forwards to linked case.
- Produces: computer block still accepts system discs on case/laptop only.

- [ ] Add explicit monitor disc rejection.
- [ ] Keep insertion in `ComputerBlock` only.

### Task 5: Laptop Screen Model

**Files:**
- Modify: `src/main/resources/assets/minevm/models/block/laptop_computer.json`
- Modify: `src/main/java/com/example/vm/client/ComputerScreenRenderer.java`

**Interfaces:**
- Produces: laptop screen has frame, intact top bezel, and surface coordinates aligned to new screen plane.

- [ ] Rebuild laptop display as screen plane plus black bezel/frame similar to desktop monitor.
- [ ] Keep hinge/base/keyboard.
- [ ] Adjust renderer z/y/scale if needed.

### Task 6: Verification

**Files:**
- Modify: `build.gradle`

**Interfaces:**
- Build rejects GUI fallback markers and requires in-world action markers.

- [ ] Update BIOS verification to require no GUI fallback files/classes.
- [ ] Require packet computer position, client action activation, and monitor disc rejection markers.
- [ ] Run full build.
