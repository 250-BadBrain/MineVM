# Peripherals Icons And Wiring Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add desktop peripherals, distinctive item icons, and a wiring foundation for the next session's immersive control and complete cable system.

**Architecture:** Keep hardware peripherals as inventory items first, with cable/item types represented explicitly in code. HDMI and USB cable items become connection tools with stable cable type metadata, while keyboard and mouse become USB peripherals that future monitor/case connection logic can consume. Disc icons are replaced with per-OS pixel art so players can visually distinguish boot media.

**Tech Stack:** Minecraft Forge 1.20.1, Java 17, generated PNG item textures, item model JSON, Gradle verification.

## Global Constraints

- No VM process or QEMU work from item classes.
- Peripherals are gameplay items and future wiring inputs, not VM input senders yet.
- HDMI connects display paths; USB connects keyboard/mouse peripherals.
- Final immersive control and complete cable linking are deferred to the next session by user request.
- Tests must not launch QEMU.

---

### Task 1: Add Peripheral Item Model Types

**Files:**
- Create: `src/main/java/com/example/vm/item/CableType.java`
- Modify: `src/main/java/com/example/vm/item/HdmiCableItem.java`
- Create: `src/main/java/com/example/vm/item/UsbCableItem.java`
- Create: `src/main/java/com/example/vm/item/ComputerPeripheralItem.java`
- Create: `src/main/java/com/example/vm/item/ComputerPeripheralType.java`

**Interfaces:**
- Produces: `CableType.HDMI` and `CableType.USB`
- Produces: `ComputerPeripheralType.KEYBOARD` and `ComputerPeripheralType.MOUSE`

- [x] **Step 1: Add typed item classes**

Items show safe placeholder messages and expose type methods.

### Task 2: Register Keyboard Mouse And USB Cable

**Files:**
- Modify: `src/main/java/com/example/vm/registry/ModItems.java`
- Modify: `src/main/java/com/example/vm/registry/ModCreativeTabs.java`
- Modify: `src/main/resources/assets/computer_in_minecraft/lang/en_us.json`

**Interfaces:**
- Produces: `KEYBOARD`, `MOUSE`, and `USB_CABLE` registry objects.
- Creative tab displays case, monitor, HDMI, USB, keyboard, mouse, laptop, discs.

- [x] **Step 1: Register items and language**

Keep stack sizes sensible for cables and peripherals.

### Task 3: Add Distinctive Item Icons

**Files:**
- Create PNG textures under `src/main/resources/assets/computer_in_minecraft/textures/item/`
- Modify item model JSON for HDMI, USB, keyboard, mouse, and all system discs.

**Interfaces:**
- Each item icon visually communicates the device type.
- Disc icons are OS-distinct.

- [x] **Step 1: Generate pixel art textures**

Use small generated PNGs committed as assets.

### Task 4: Add Wiring Foundation Types

**Files:**
- Create: `src/main/java/com/example/vm/computer/wiring/ComputerPortType.java`
- Create: `src/main/java/com/example/vm/computer/wiring/ComputerConnectionType.java`
- Create: `src/main/java/com/example/vm/computer/wiring/ComputerWiringEndpoint.java`
- Create: `src/main/java/com/example/vm/computer/wiring/ComputerWiringConnection.java`

**Interfaces:**
- Models HDMI display and USB peripheral wiring without world mutation yet.

- [x] **Step 1: Add immutable connection records**

Future block/entity linking can use these names directly.

### Task 5: Verification

**Files:**
- Modify: `build.gradle`

**Interfaces:**
- Build verifies item registrations, textures/models, wiring types, and creative tab ordering.

- [x] **Step 1: Run full build**

Run: `.\gradlew.bat build --no-daemon --max-workers=1 --console=plain --stacktrace`
Expected: `BUILD SUCCESSFUL`

## Self-Review

Spec coverage: Adds requested desktop peripherals, icon assets, USB cable, and the next-session wiring/control foundation.

Placeholder scan: No placeholder instructions remain.

Type consistency: Registry object names, item model paths, texture paths, and enum names align.
