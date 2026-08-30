# Hardware Buttons And Input Capture Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix laptop BIOS rendering, remove camera jitter by disabling Minecraft mouse-look during control, make desktop control enter only from monitor, and add physical display/computer power buttons.

**Architecture:** Use Minecraft mouse release/grab APIs for GUI-like cursor capture without reintroducing GUI fallback. Add small hardware state to monitor block entity for display power. Keep VM power actions on computer power buttons only, while BIOS only exposes Power Off.

**Tech Stack:** Forge 1.20.1, Java 17, block hit coordinate resolution, block entity renderer/model JSON, Gradle verification.

## Global Constraints

- No GUI fallback, no menu screens, no `NetworkHooks.openScreen`.
- Right-click desktop case must not enter screen control.
- Right-click desktop monitor enters control except when hitting monitor display power button.
- BIOS shows Power Off only; VM Power On is triggered by physical computer/laptop power button.
- Full verification command: `./gradlew.bat build --no-daemon --max-workers=1 --console=plain --stacktrace`.

---

### Task 1: Mouse Capture Without Jitter

**Files:**
- Modify: `src/main/java/com/example/vm/client/ImmersiveControlClient.java`
- Modify: `src/main/java/com/example/vm/client/ClientForgeEvents.java`

**Interfaces:**
- Produce: `releaseMouseForScreenControl()` and `grabMouseAfterScreenControl()`.
- Remove: per-tick camera lock call.

- [ ] Release mouse on control start.
- [ ] Grab mouse on control stop.
- [ ] Keep keyboard/button input capture.

### Task 2: Hardware Power Targets

**Files:**
- Modify: `src/main/java/com/example/vm/computer/ComputerInteractionTarget.java`
- Modify: `src/main/java/com/example/vm/computer/ComputerInteractionResolver.java`
- Modify: `src/main/java/com/example/vm/block/ComputerBlock.java`
- Modify: `src/main/java/com/example/vm/block/ComputerMonitorBlock.java`
- Modify: `src/main/java/com/example/vm/blockentity/ComputerMonitorBlockEntity.java`

**Interfaces:**
- Produce: `DISPLAY_POWER_BUTTON` target.
- Produce: `ComputerMonitorBlockEntity.displayPowered()` and `toggleDisplayPower()`.

- [ ] Desktop case right-click body shows status, power button toggles VM.
- [ ] Laptop power button toggles VM, laptop screen/body right-click enters control.
- [ ] Monitor power button toggles display black/Bios screen.

### Task 3: Models And Renderers

**Files:**
- Modify: `src/main/resources/assets/minevm/models/block/computer_monitor.json`
- Modify: `src/main/resources/assets/minevm/models/block/desktop_computer.json`
- Modify: `src/main/resources/assets/minevm/models/block/laptop_computer.json`
- Modify: `src/main/java/com/example/vm/client/ComputerMonitorRenderer.java`
- Modify: `src/main/java/com/example/vm/client/ComputerScreenRenderer.java`
- Modify: `src/main/java/com/example/vm/client/ScreenSurfaceRenderer.java`

**Interfaces:**
- Monitor renderer respects `displayPowered()`.
- Laptop renderer z/y matches new screen plane.
- BIOS panel labels include Power Off but no Power On.

- [ ] Add small monitor power button to lower-right bezel.
- [ ] Add desktop case and laptop C-side power buttons.
- [ ] Adjust laptop BIOS panel surface z/y.
- [ ] Render black screen when monitor display is off.

### Task 4: Verification

**Files:**
- Modify: `build.gradle`

**Interfaces:**
- Require mouse release/grab markers, no camera lock tick marker, no desktop case control entry, display power markers.

- [ ] Update verification markers.
- [ ] Run full build.
