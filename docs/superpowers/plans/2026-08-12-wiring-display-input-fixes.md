# Wiring Display Input Fixes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix placed keyboard/mouse wiring, HDMI client sync, immersive right-click capture, in-world screen text placement, and monitor geometry.

**Architecture:** Keep VM and Minecraft thread boundaries unchanged. Treat peripherals as lightweight placeable blocks with no block entity; keep connection state on `ComputerBlockEntity` and `ComputerMonitorBlockEntity`, synchronized through standard block entity update packets.

**Tech Stack:** Minecraft Forge 1.20.1, Java 17, Gradle verification tasks, JSON block/item models.

## Global Constraints

- Do not run QEMU or blocking VM work from blocks, renderers, ticks, or input handlers.
- Keep desktop wiring explicit: HDMI connects case to monitor, USB connects placed keyboard/mouse to case.
- Keep laptop built-in screen/keyboard/mouse logic.
- Full verification command: `./gradlew.bat build --no-daemon --max-workers=1 --console=plain --stacktrace`.

---

### Task 1: Placeable Keyboard And Mouse Blocks

**Files:**
- Create: `src/main/java/com/example/vm/block/ComputerPeripheralBlock.java`
- Modify: `src/main/java/com/example/vm/registry/ModBlocks.java`
- Modify: `src/main/java/com/example/vm/registry/ModItems.java`
- Modify: `src/main/resources/assets/minevm/blockstates/keyboard.json`
- Modify: `src/main/resources/assets/minevm/blockstates/mouse.json`
- Modify: `src/main/resources/assets/minevm/models/block/keyboard.json`
- Modify: `src/main/resources/assets/minevm/models/block/mouse.json`
- Modify: `src/main/resources/assets/minevm/models/item/keyboard.json`
- Modify: `src/main/resources/assets/minevm/models/item/mouse.json`

**Interfaces:**
- Produces: `ComputerPeripheralBlock.peripheralType(): ComputerPeripheralType`
- Consumes: existing `ComputerPeripheralType.KEYBOARD` and `ComputerPeripheralType.MOUSE`

- [ ] Add block class and registrations.
- [ ] Convert keyboard/mouse items to block items.
- [ ] Add blockstate/model resources while keeping distinct item icons.
- [ ] Run `./gradlew.bat compileJava --no-daemon --max-workers=1 --console=plain`.

### Task 2: USB Wiring From Placed Peripherals

**Files:**
- Modify: `src/main/java/com/example/vm/item/UsbCableItem.java`

**Interfaces:**
- Consumes: `ComputerPeripheralBlock.peripheralType()`
- Produces: USB cable NBT fields `UsbPeripheralPos` and `UsbPeripheralType`

- [ ] Store clicked keyboard/mouse block as USB source.
- [ ] Connect stored peripheral source to clicked computer case.
- [ ] Preserve existing offhand item fallback when possible.
- [ ] Verify compile.

### Task 3: HDMI Client Sync

**Files:**
- Modify: `src/main/java/com/example/vm/blockentity/ComputerMonitorBlockEntity.java`
- Modify: `src/main/java/com/example/vm/blockentity/ComputerBlockEntity.java`

**Interfaces:**
- Produces: `getUpdatePacket()` using `ClientboundBlockEntityDataPacket.create(this)`

- [ ] Add update packets to both block entities.
- [ ] Keep `sendBlockUpdated(..., 3)` in state-changing methods.
- [ ] Verify monitor renderer sees linked state client-side.

### Task 4: Immersive Input Capture Bug

**Files:**
- Modify: `src/main/java/com/example/vm/client/ImmersiveControlClient.java`
- Modify: `src/main/java/com/example/vm/client/ClientForgeEvents.java`

**Interfaces:**
- Produces: `ImmersiveControlClient.suppressMinecraftBindings()`

- [ ] Clear use/attack key mapping on control start and each client tick.
- [ ] Continue canceling keys, mouse buttons, mouse wheel, and player movement while active.
- [ ] Keep `Ctrl+Alt` release path.

### Task 5: Screen Text And Monitor Model Alignment

**Files:**
- Modify: `src/main/java/com/example/vm/client/ComputerMonitorRenderer.java`
- Modify: `src/main/java/com/example/vm/client/ComputerScreenRenderer.java`
- Modify: `src/main/java/com/example/vm/client/ScreenSurfaceRenderer.java`
- Modify: `src/main/resources/assets/minevm/models/block/computer_monitor.json`

**Interfaces:**
- Consumes: existing `ScreenSurfaceRenderer.renderText(...)`

- [ ] Move monitor text to z=4/16 screen plane and lower y into screen area.
- [ ] Move laptop text to z=13/16 screen plane and screen-height y.
- [ ] Reduce text scale to fit on physical screen.
- [ ] Move monitor stand/base forward so it touches the panel.
- [ ] Run full build.
