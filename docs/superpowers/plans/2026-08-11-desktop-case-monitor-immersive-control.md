# Desktop Case Monitor Immersive Control Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the generic desktop computer presentation with realistic computer case + monitor + HDMI components and establish the future immersive screen-control architecture.

**Architecture:** Keep the legacy `computer_block` registered for save compatibility but remove it from normal creative gameplay. Rename the desktop block/item presentation to `Computer Case`, add a separate `Computer Monitor` block and `HDMI Cable` item, and introduce monitor link data so future rendering can show BIOS/OS directly on a monitor block. The current GUI BIOS remains a temporary development fallback until the immersive control session and in-world screen renderer are implemented.

**Tech Stack:** Minecraft Forge 1.20.1, Java 17, block/item registries, block entity data, blockstate/model JSON, Forge networking.

## Global Constraints

- Do not break existing worlds by unregistering `computer_block`.
- Hide legacy `Computer` from the creative tab.
- Desktop gameplay should use `Computer Case` + `Computer Monitor` + `HDMI Cable`.
- Final BIOS/OS UI target is in-world screen rendering, not standalone GUI.
- Immersive control mode should eventually capture keyboard/mouse until `Ctrl+Alt`.
- Tests must not launch QEMU.

---

### Task 1: Hide Legacy Computer And Rename Desktop

**Files:**
- Modify: `src/main/java/com/example/vm/registry/ModCreativeTabs.java`
- Modify: `src/main/resources/assets/computer_in_minecraft/lang/en_us.json`
- Modify: `build.gradle`

**Interfaces:**
- `computer_block` remains registered but is not listed in the creative tab.
- `desktop_computer` is presented as `Computer Case`.

- [x] **Step 1: Remove legacy creative entry**

Keep the registry object only for compatibility.

### Task 2: Add Monitor Block And HDMI Cable

**Files:**
- Create: `src/main/java/com/example/vm/block/ComputerMonitorBlock.java`
- Create: `src/main/java/com/example/vm/blockentity/ComputerMonitorBlockEntity.java`
- Create: `src/main/java/com/example/vm/item/HdmiCableItem.java`
- Modify: `src/main/java/com/example/vm/registry/ModBlocks.java`
- Modify: `src/main/java/com/example/vm/registry/ModItems.java`
- Modify: `src/main/java/com/example/vm/registry/ModBlockEntities.java`
- Modify: `src/main/java/com/example/vm/registry/ModCreativeTabs.java`

**Interfaces:**
- Produces: `ModBlocks.COMPUTER_MONITOR`
- Produces: `ModItems.HDMI_CABLE`
- Produces: monitor block entity with optional linked computer id.

- [x] **Step 1: Add monitor block/entity**

Monitor stores link metadata but does not manage VM processes.

- [x] **Step 2: Add HDMI item**

The item creates a future-safe player-facing connection tool placeholder.

### Task 3: Add Monitor And Cable Assets

**Files:**
- Create: monitor blockstate/model/item/loot JSON.
- Create: HDMI cable item model JSON.
- Modify: language JSON.

**Interfaces:**
- Monitor visually reads as a separate screen.
- Cable appears as an item in creative tab.

- [x] **Step 1: Add JSON resources**

Use vanilla textures, no custom texture files.

### Task 4: Add Immersive Control Design Markers

**Files:**
- Create: `src/main/java/com/example/vm/screen/ScreenControlMode.java`
- Create: `src/main/java/com/example/vm/screen/ScreenControlSession.java`
- Modify: `build.gradle`

**Interfaces:**
- Produces future mode names `GUI_FALLBACK` and `IMMERSIVE_CAPTURE`.
- Documents `Ctrl+Alt` release combo in code-level constants.

- [x] **Step 1: Add non-invasive session model**

No input capture yet; this is an architecture anchor for the next rendering stage.

### Task 5: Verification

**Files:**
- Modify: `build.gradle`

**Interfaces:**
- Build verifies hidden legacy computer, case/monitor/cable resources, block entities, and immersive markers.

- [x] **Step 1: Run full build**

Run: `.\gradlew.bat build --no-daemon --max-workers=1 --console=plain --stacktrace`
Expected: `BUILD SUCCESSFUL`

## Self-Review

Spec coverage: This implements the visible desktop split and establishes the future in-world screen control design without prematurely hand-rolling renderer/input capture.

Placeholder scan: No placeholder instructions remain.

Type consistency: Registry names and resource names match across Java and JSON.
