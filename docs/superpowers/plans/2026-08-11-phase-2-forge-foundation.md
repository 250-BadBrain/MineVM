# Phase 2 Forge Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a minimal Forge 1.20.1 mod foundation that registers a placeable computer block.

**Architecture:** Keep the mod entry point small and delegate registry ownership to focused classes under `com.example.vm`. The block has no VM behavior in Phase 2; it is only a normal Forge block plus item registration.

**Tech Stack:** Minecraft Forge 1.20.1, Java 17, Gradle 8.8, Forge GameTest.

## Global Constraints

- Minecraft version: `1.20.1`
- Mod loader: Forge
- Java version: JDK 17
- Phase 2 only: no QEMU calls, no VM manager, no screen/input system.
- Computer block must be placeable and have no VM functionality.

---

### Task 1: Replace MDK Example Metadata

**Files:**
- Modify: `gradle.properties`
- Modify: `src/main/resources/META-INF/mods.toml`

**Interfaces:**
- Produces: `mod_id=computer_in_minecraft`, `mod_group_id=com.example.vm`

- [x] Update Gradle mod metadata to the Computer in Minecraft identity.
- [x] Keep Forge and Minecraft version values unchanged.

### Task 2: Register Computer Block

**Files:**
- Delete: `src/main/java/com/example/examplemod/ExampleMod.java`
- Delete: `src/main/java/com/example/examplemod/Config.java`
- Create: `src/main/java/com/example/vm/ComputerInMinecraft.java`
- Create: `src/main/java/com/example/vm/block/ComputerBlock.java`
- Create: `src/main/java/com/example/vm/registry/ModBlocks.java`
- Create: `src/main/java/com/example/vm/registry/ModItems.java`
- Create: `src/main/java/com/example/vm/registry/ModCreativeTabs.java`

**Interfaces:**
- Produces: `ComputerInMinecraft.MOD_ID`
- Produces: `ModBlocks.COMPUTER_BLOCK`
- Produces: `ModItems.COMPUTER_BLOCK_ITEM`

- [x] Use DeferredRegister for blocks, items, and creative tabs.
- [x] Register `computer_block` as a normal block with no VM side effects.
- [x] Add the block item to the mod creative tab.

### Task 3: Add Resources

**Files:**
- Create: `src/main/resources/assets/computer_in_minecraft/lang/en_us.json`
- Create: `src/main/resources/assets/computer_in_minecraft/blockstates/computer_block.json`
- Create: `src/main/resources/assets/computer_in_minecraft/models/block/computer_block.json`
- Create: `src/main/resources/assets/computer_in_minecraft/models/item/computer_block.json`
- Create: `src/main/resources/data/computer_in_minecraft/loot_tables/blocks/computer_block.json`

**Interfaces:**
- Consumes: `computer_in_minecraft:computer_block`

- [x] Provide simple cube model and item model.
- [x] Provide loot table so breaking the block drops itself.
- [x] Provide English names for mod tab and block.

### Task 4: Add GameTest

**Files:**
- Create: `src/main/java/com/example/vm/test/ComputerBlockGameTests.java`

**Interfaces:**
- Consumes: `ModBlocks.COMPUTER_BLOCK`

- [x] Add a GameTest that places `computer_block` in a test world and asserts it remains at the target position.
- [x] Run `gradlew build`.
- [x] Add `verifyPhase2ComputerBlock` to `check` to verify resources, compiled GameTest class, and jar contents.
- [ ] Run `gradlew runGameTestServer`. Attempted locally, but the task did not complete within 5 minutes and produced no game log output.
