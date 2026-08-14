# In World BIOS Panel Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Render the BIOS interface directly on desktop monitors and laptop screens instead of two-line placeholder status text.

**Architecture:** Keep the existing GUI screen as a fallback/control implementation for now, but make block entity renderers draw an in-world BIOS panel using synchronized `ComputerBlockEntity` data. Desktop monitors resolve their linked computer through HDMI position data; laptops render from their own block entity.

**Tech Stack:** Minecraft Forge 1.20.1, Java 17, block entity renderers, Gradle verification.

## Global Constraints

- HDMI/USB connection remains sneak-right-click compatible with the current interaction design.
- Normal empty-hand right-click remains immersive control entry.
- Rendering must be client-only and must not perform VM/QEMU work.
- Full verification command: `./gradlew.bat build --no-daemon --max-workers=1 --console=plain --stacktrace`.

---

### Task 1: BIOS Surface Renderer

**Files:**
- Modify: `src/main/java/com/example/vm/client/ScreenSurfaceRenderer.java`

**Interfaces:**
- Produces: `renderBiosPanel(PoseStack, MultiBufferSource, BlockState, float, float, float, String, String, String, boolean, int)`

- [ ] Add multi-line BIOS drawing helper with title, computer id, disc state, boot options, eject/power controls, and release hint.
- [ ] Keep existing `renderText` for fallback no-HDMI states.

### Task 2: Desktop Monitor BIOS Data

**Files:**
- Modify: `src/main/java/com/example/vm/client/ComputerMonitorRenderer.java`

**Interfaces:**
- Consumes: `ComputerMonitorBlockEntity.linkedComputerPos()` and `ComputerBlockEntity.insertedDisc()`

- [ ] Resolve linked computer block entity client-side.
- [ ] Render BIOS panel when HDMI target exists.
- [ ] Keep `NO HDMI` / missing computer fallback text.

### Task 3: Laptop BIOS Panel

**Files:**
- Modify: `src/main/java/com/example/vm/client/ComputerScreenRenderer.java`

**Interfaces:**
- Consumes: local laptop `ComputerBlockEntity`

- [ ] Render the same BIOS panel on laptop screen.
- [ ] Preserve `Ctrl+Alt` active hint.

### Task 4: Verification

**Files:**
- Modify: `build.gradle`

**Interfaces:**
- Consumes: source markers `renderBiosPanel`, `Boot Disc`, `Boot Disk`, `linkedComputerPos`

- [ ] Extend existing BIOS foundation verification markers.
- [ ] Run full build.
