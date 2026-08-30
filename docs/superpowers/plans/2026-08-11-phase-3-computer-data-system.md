# Phase 3 Computer Data System Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a `ComputerBlockEntity` that stores persistent computer identity and hardware configuration data without any VM integration.

**Architecture:** Convert `ComputerBlock` into a block-entity-backed block and register a `BlockEntityType`. Keep the computer data model in `com.example.vm.computer`, separate from future VM backend code.

**Tech Stack:** Minecraft Forge 1.20.1, Java 17, Gradle 8.8, Forge GameTest-compatible test code, Gradle verification tasks.

## Global Constraints

- Minecraft version: `1.20.1`
- Mod loader: Forge
- Java version: JDK 17
- Phase 3 only: no QEMU calls, no VM manager, no screen/input system.
- Data system must remain independent from VM backend implementation.

---

### Task 1: Add Computer Data Model

**Files:**
- Create: `src/main/java/com/example/vm/computer/ComputerHardwareConfiguration.java`
- Create: `src/main/resources/assets/minevm/defaults/computer.json`

**Interfaces:**
- Produces: `ComputerHardwareConfiguration.defaultConfiguration(String computerId)`
- Produces: `ComputerHardwareConfiguration#cpuCores()`
- Produces: `ComputerHardwareConfiguration#memoryMb()`
- Produces: `ComputerHardwareConfiguration#diskFile()`

- [x] Add immutable hardware configuration record.
- [x] Add default JSON resource documenting the Phase 3 data shape.

### Task 2: Add Computer Block Entity

**Files:**
- Create: `src/main/java/com/example/vm/blockentity/ComputerBlockEntity.java`
- Create: `src/main/java/com/example/vm/registry/ModBlockEntities.java`
- Modify: `src/main/java/com/example/vm/block/ComputerBlock.java`
- Modify: `src/main/java/com/example/vm/MineVM.java`

**Interfaces:**
- Produces: `ComputerBlockEntity#getComputerId()`
- Produces: `ComputerBlockEntity#getHardwareConfiguration()`
- Produces: `ModBlockEntities.COMPUTER_BLOCK_ENTITY`

- [x] Generate a UUID-backed `computerId` for newly created computer block entities.
- [x] Persist `computerId`, `cpuCores`, `memoryMb`, and `diskFile` in NBT.
- [x] Register the block entity type through Forge DeferredRegister.

### Task 3: Extend Tests And Verification

**Files:**
- Modify: `src/main/java/com/example/vm/test/ComputerBlockGameTests.java`
- Modify: `build.gradle`

**Interfaces:**
- Consumes: `ModBlockEntities.COMPUTER_BLOCK_ENTITY`
- Consumes: `ComputerBlockEntity#getComputerId()`

- [x] Extend GameTest source to assert the placed computer creates a `ComputerBlockEntity`.
- [x] Add `verifyPhase3ComputerDataSystem` to `check`.
- [x] Run `gradlew clean build`.
