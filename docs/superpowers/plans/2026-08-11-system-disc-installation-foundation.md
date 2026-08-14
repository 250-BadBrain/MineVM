# System Disc Installation Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add OS disc items and persistent computer disc insertion state as the foundation for a later BIOS/install UI.

**Architecture:** Treat disc items as lightweight OS profile metadata. `ComputerBlockEntity` stores the inserted `GuestOperatingSystem`, while `ComputerBlock` handles inserting/ejecting discs and status display. VM backend and QEMU command generation continue to read OS profile from descriptors/config until BIOS install state is implemented.

**Tech Stack:** Minecraft Forge 1.20.1, Java 17, item registry, block entity NBT, Gradle JavaExec/self-test verification.

## Global Constraints

- Disc items must not embed ISO files.
- Disc items carry installation intent and OS metadata only.
- Computer data persists inserted disc profile in NBT.
- BlockEntity must not call QEMU or QMP.
- Tests must not launch QEMU.

---

### Task 1: System Disc Item Model

**Files:**
- Create: `src/main/java/com/example/vm/item/SystemDiscItem.java`
- Create: `src/main/java/com/example/vm/test/SystemDiscItemSelfTest.java`

**Interfaces:**
- Produces: `SystemDiscItem.guestOperatingSystem(): GuestOperatingSystem`
- Produces: `SystemDiscItem.displayNameKey(): String`

- [x] **Step 1: Add item class**

Create an item carrying `GuestOperatingSystem` and translation key metadata.

- [x] **Step 2: Add self-test**

Assert Linux/Windows disc metadata.

### Task 2: Register Disc Items And Resources

**Files:**
- Modify: `src/main/java/com/example/vm/registry/ModItems.java`
- Modify: `src/main/java/com/example/vm/registry/ModCreativeTabs.java`
- Modify: `src/main/resources/assets/computer_in_minecraft/lang/en_us.json`
- Create: item model JSON files for the three discs.

**Interfaces:**
- Produces registry objects: `UBUNTU_SERVER_DISC`, `WINDOWS_10_DISC`, `WINDOWS_11_DISC`

- [x] **Step 1: Register three disc items**

Add all discs to the creative tab.

- [x] **Step 2: Add simple generated item models**

Use vanilla disc-like item texture placeholders for now.

### Task 3: Computer Disc Insertion State

**Files:**
- Modify: `src/main/java/com/example/vm/blockentity/ComputerBlockEntity.java`
- Modify: `src/main/java/com/example/vm/block/ComputerBlock.java`
- Modify: `src/main/java/com/example/vm/computer/ComputerStatusMessage.java`
- Modify: `build.gradle`

**Interfaces:**
- Produces: `insertDisc(GuestOperatingSystem): boolean`
- Produces: `ejectDisc(): Optional<GuestOperatingSystem>`
- Produces: `insertedDisc(): Optional<GuestOperatingSystem>`

- [x] **Step 1: Persist inserted disc**

Save/load inserted guest OS profile under `InsertedDisc`.

- [x] **Step 2: Handle right-click insertion/ejection**

Right-click with a disc inserts it. Shift-right-click with empty hand ejects it before VM toggle logic.

- [x] **Step 3: Show disc in status**

Status includes `Disc: EMPTY` or the inserted OS profile.

- [x] **Step 4: Add verification task**

Run build and assert disc items, NBT state, resources, and tests exist.

## Self-Review

Spec coverage: This implements the agreed disc-as-metadata foundation without embedding ISOs or changing VM backend responsibilities.

Placeholder scan: No placeholder instructions remain.

Type consistency: All referenced methods and classes are defined in this plan.
