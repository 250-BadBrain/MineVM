# Multiblock Computer Occupancy Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add real multi-block occupancy for enlarged desktop monitors and laptop computers so their visual size matches world placement, collision, interaction, and destruction behavior.

**Architecture:** Add an invisible `ComputerOccupancyBlock` plus `ComputerOccupancyBlockEntity` that stores the origin block position. Enlarged primary blocks place occupancy blocks across their footprint, and occupancy interactions/destruction forward to the origin block. The primary block removes its occupancy blocks on destruction so there are no orphan blockers.

**Tech Stack:** Minecraft Forge 1.20.1, Java 17, existing block/entity registry patterns, Gradle JavaExec self-tests.

## Global Constraints

- Minecraft main thread must not start QEMU or perform blocking VM/screen work.
- BlockEntity must not directly call QEMU.
- Existing desktop case, laptop, monitor, HDMI, USB, disc, BIOS, and VNC behavior must keep working through the origin block entity.
- Occupancy blocks must not drop items and must be invisible.
- Destroying any occupied part must destroy the origin exactly once.

---

### Task 1: Occupancy Block and Entity

**Files:**
- Create: `src/main/java/com/example/vm/block/ComputerOccupancyBlock.java`
- Create: `src/main/java/com/example/vm/blockentity/ComputerOccupancyBlockEntity.java`
- Modify: `src/main/java/com/example/vm/registry/ModBlocks.java`
- Modify: `src/main/java/com/example/vm/registry/ModBlockEntities.java`

**Interfaces:**
- Produces: `ComputerOccupancyBlockEntity#setOrigin(BlockPos)` and `origin(): Optional<BlockPos>`.
- Produces: `ModBlocks.COMPUTER_OCCUPANCY` registry entry.

- [ ] Add invisible non-dropping occupancy block.
- [ ] Add block entity storing origin position in NBT and update packet.
- [ ] Register block and block entity.

### Task 2: Footprint Planner

**Files:**
- Create: `src/main/java/com/example/vm/block/ComputerFootprint.java`

**Interfaces:**
- Produces: `ComputerFootprint.monitorOffsets(Direction)` for 3x2x1 monitor.
- Produces: `ComputerFootprint.laptopOffsets(Direction)` for 3x2x2 laptop.
- Produces: `ComputerFootprint.canPlace(LevelAccessor, BlockPos, Collection<BlockPos>)`.

- [ ] Define offsets relative to origin, excluding origin.
- [ ] Rotate width/depth offsets by facing.
- [ ] Treat replaceable air/liquid-free blocks only as placeable.

### Task 3: Placement and Cleanup

**Files:**
- Modify: `src/main/java/com/example/vm/block/ComputerMonitorBlock.java`
- Modify: `src/main/java/com/example/vm/block/ComputerBlock.java`

**Interfaces:**
- Consumes: `ComputerFootprint.monitorOffsets`, `ComputerFootprint.laptopOffsets`.
- Produces: primary blocks refuse placement if footprint is blocked.
- Produces: primary blocks create/remove occupancy blocks.

- [ ] Override `canSurvive` / placement state checks where needed.
- [ ] On placement, write occupancy blocks and origin NBT.
- [ ] On removal, remove matching occupancy blocks.

### Task 4: Interaction and Destruction Forwarding

**Files:**
- Modify: `src/main/java/com/example/vm/block/ComputerOccupancyBlock.java`

**Interfaces:**
- Occupancy `use` calls origin block `use` with origin position.
- Occupancy destruction destroys origin block and suppresses duplicate drops.

- [ ] Forward right-click to origin block.
- [ ] Destroying occupancy removes origin and all occupancy parts.
- [ ] Occupancy shape is full-block for physical occupation.

### Task 5: Verification

**Files:**
- Create: `src/main/java/com/example/vm/test/ComputerFootprintSelfTest.java`
- Modify: `build.gradle`

**Interfaces:**
- Produces: Gradle task `runComputerFootprintSelfTest`.
- `verifyBiosMenuFoundation` or architecture verification requires occupancy block/entity and registry markers.

- [ ] Test monitor footprint has 5 occupancy blocks plus origin = 3x2x1.
- [ ] Test laptop footprint has 11 occupancy blocks plus origin = 3x2x2.
- [ ] Test facing rotation changes offsets deterministically.
- [ ] Run full build.
