# BIOS Menu Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a real in-game BIOS/Boot Menu screen opened from the computer block, ready for later disc install actions.

**Architecture:** Register a Forge `MenuType` for a computer BIOS menu. Server opens the menu through `NetworkHooks`, the client registers a screen renderer on the mod event bus, and the menu carries only the computer block position. BIOS UI is presentation-only in this step; VM work remains in `VirtualMachineManager`.

**Tech Stack:** Minecraft Forge 1.20.1, Java 17, Forge menu/screen registration, Gradle verification.

## Global Constraints

- BIOS UI must not call QEMU or QMP directly.
- BlockEntity must not manage VM processes.
- Opening BIOS must not block the Minecraft thread.
- Disc insertion/ejection behavior remains intact.
- Tests must not launch QEMU.

---

### Task 1: Register BIOS Menu

**Files:**
- Create: `src/main/java/com/example/vm/menu/ComputerBiosMenu.java`
- Create: `src/main/java/com/example/vm/registry/ModMenus.java`
- Modify: `src/main/java/com/example/vm/MineVM.java`

**Interfaces:**
- Produces: `ModMenus.COMPUTER_BIOS_MENU`
- Produces: `ComputerBiosMenu.computerPos(): BlockPos`

- [x] **Step 1: Add menu class**

Store the computer `BlockPos` and validate the player is still near a `ComputerBlockEntity`.

- [x] **Step 2: Register menu type**

Register the menu using Forge `IForgeMenuType.create`.

### Task 2: Add Client BIOS Screen

**Files:**
- Create: `src/main/java/com/example/vm/client/ComputerBiosScreen.java`
- Create: `src/main/java/com/example/vm/client/ClientModEvents.java`

**Interfaces:**
- Produces: a BIOS screen registered with `RegisterMenuScreensEvent`

- [x] **Step 1: Draw BIOS-style UI**

Display title, computer position, boot order, install entry, and warnings.

- [x] **Step 2: Register screen client-side**

Use `@Mod.EventBusSubscriber(... Dist.CLIENT)`.

### Task 3: Open BIOS From Computer Block

**Files:**
- Modify: `src/main/java/com/example/vm/block/ComputerBlock.java`
- Modify: `src/main/resources/assets/minevm/lang/en_us.json`
- Modify: `build.gradle`

**Interfaces:**
- Empty-hand normal right-click opens BIOS.
- Disc item right-click still inserts disc.
- Empty-hand shift-right-click still ejects disc or toggles VM.

- [x] **Step 1: Add `NetworkHooks.openScreen` path**

Open BIOS for `ServerPlayer`.

- [x] **Step 2: Add verification task**

Check menu/screen/registration classes and jar entries.

## Self-Review

Spec coverage: This implements the BIOS UI entry point for the disc installation workflow without coupling UI to QEMU.

Placeholder scan: No placeholder instructions remain.

Type consistency: All planned classes and methods are defined.
