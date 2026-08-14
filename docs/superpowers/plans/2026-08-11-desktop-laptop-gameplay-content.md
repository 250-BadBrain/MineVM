# Desktop Laptop Gameplay Content Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add substantial visible gameplay content by splitting the generic computer into desktop and laptop devices with distinct models, items, shapes, and BIOS status presentation.

**Architecture:** Reuse `ComputerBlock` and its existing `ComputerFormFactor` so both devices share VM data, BIOS, disc, and backend behavior. Register two new blocks/items while keeping the original `computer_block` as a compatibility desktop-style item. The BIOS state packet grows into a lightweight status snapshot containing VM state and inserted disc; the client screen renders that state without touching VM services.

**Tech Stack:** Minecraft Forge 1.20.1, Java 17, block/item registries, blockstate/model JSON, Forge networking, Gradle verification.

## Global Constraints

- No client screen may call QEMU, QMP, or `VirtualMachineManager`.
- `ComputerBlockEntity` remains shared across desktop and laptop devices.
- Desktop and laptop must use the existing form-factor interaction resolver.
- Existing `computer_block` remains available for compatibility.
- Tests must not launch QEMU.

---

### Task 1: Register Desktop And Laptop Blocks

**Files:**
- Modify: `src/main/java/com/example/vm/registry/ModBlocks.java`
- Modify: `src/main/java/com/example/vm/registry/ModItems.java`
- Modify: `src/main/java/com/example/vm/registry/ModCreativeTabs.java`

**Interfaces:**
- Produces: `ModBlocks.DESKTOP_COMPUTER`
- Produces: `ModBlocks.LAPTOP_COMPUTER`
- Produces: matching block items in the creative tab.

- [x] **Step 1: Register blocks with form factors**

Use `new ComputerBlock(ComputerFormFactor.DESKTOP)` and `new ComputerBlock(ComputerFormFactor.LAPTOP)`.

### Task 2: Add Distinct Shapes

**Files:**
- Modify: `src/main/java/com/example/vm/block/ComputerBlock.java`

**Interfaces:**
- Desktop uses a taller tower-like voxel shape.
- Laptop uses a low wide voxel shape.

- [x] **Step 1: Override `getShape`**

Return form-factor-specific rotated shapes.

### Task 3: Add Visible Models And Assets

**Files:**
- Create: `src/main/resources/assets/computer_in_minecraft/blockstates/desktop_computer.json`
- Create: `src/main/resources/assets/computer_in_minecraft/blockstates/laptop_computer.json`
- Create: `src/main/resources/assets/computer_in_minecraft/models/block/desktop_computer.json`
- Create: `src/main/resources/assets/computer_in_minecraft/models/block/laptop_computer.json`
- Create: `src/main/resources/assets/computer_in_minecraft/models/item/desktop_computer.json`
- Create: `src/main/resources/assets/computer_in_minecraft/models/item/laptop_computer.json`
- Modify: compatibility `computer_block` assets to point at desktop styling.

**Interfaces:**
- Desktop visually reads as a tower with front panel.
- Laptop visually reads as a low open laptop with screen and keyboard deck.

- [x] **Step 1: Add JSON model elements**

Use vanilla textures and cuboid elements.

### Task 4: Add Language And Creative Content

**Files:**
- Modify: `src/main/resources/assets/computer_in_minecraft/lang/en_us.json`

**Interfaces:**
- Adds `Desktop Computer` and `Laptop Computer`.

- [x] **Step 1: Update lang**

Keep old `Computer` name for compatibility block.

### Task 5: Improve BIOS Status Snapshot

**Files:**
- Modify: `src/main/java/com/example/vm/network/ComputerBiosStatePacket.java`
- Modify: `src/main/java/com/example/vm/network/ComputerBiosStateRequestPacket.java`
- Modify: `src/main/java/com/example/vm/network/ModNetwork.java`
- Modify: `src/main/java/com/example/vm/client/ComputerBiosScreen.java`
- Modify: `src/main/java/com/example/vm/computer/ComputerDiscActions.java`

**Interfaces:**
- BIOS displays VM state and inserted disc.
- Eject helper sends the current disc value after changes.

- [x] **Step 1: Add inserted disc to state packet**

Encode it as a string enum name for simple optional rendering.

### Task 6: Verification And Cleanup

**Files:**
- Modify: `build.gradle`

**Interfaces:**
- Build verifies new blocks/items/assets/classes and BIOS status markers.

- [x] **Step 1: Run full build**

Run: `.\gradlew.bat build --no-daemon --max-workers=1 --console=plain --stacktrace`
Expected: `BUILD SUCCESSFUL`

## Self-Review

Spec coverage: This adds visible desktop/laptop game content while preserving the VM architecture boundaries.

Placeholder scan: No placeholder instructions remain.

Type consistency: Registry names, model names, and packet fields match across tasks.
