# Realistic Power And Shapes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Hide cursor/crosshair during screen control, fix right-side button placement and hit logic, add realistic powered-off defaults, tighten block shapes, and enforce desktop keyboard/mouse requirements.

**Architecture:** Add lightweight powered-on state to `ComputerBlockEntity` and default monitor display power to off. Render BIOS only when both display and computer are powered; otherwise render black/default monitor standby. Use explicit interaction target hit boxes matching right-side model buttons. Use non-full-block shapes for all custom device blocks.

**Tech Stack:** Forge 1.20.1, Java 17, block entity NBT/update packets, Forge GUI overlay events, JSON block models, Gradle verification.

## Global Constraints

- No GUI fallback.
- `Ctrl+Alt` releases immersive control.
- Desktop monitor control requires HDMI plus keyboard and mouse connected.
- Desktop case body right-click does not enter control.
- Full verification: `./gradlew.bat build --no-daemon --max-workers=1 --console=plain --stacktrace`.

---

### Task 1: Cursor And Crosshair Suppression

**Files:**
- Modify: `ImmersiveControlClient.java`
- Modify: `ClientForgeEvents.java`

**Interfaces:**
- Produce cursor hide/show methods.
- Cancel crosshair overlay while active.

### Task 2: Power State Semantics

**Files:**
- Modify: `ComputerBlockEntity.java`
- Modify: `ComputerMonitorBlockEntity.java`
- Modify: `ComputerBlock.java`
- Modify: `ComputerMonitorBlock.java`
- Modify: renderers.

**Interfaces:**
- Computer `poweredOn()` and `togglePower()`.
- Monitor defaults off and has `displayPowered()`.
- BIOS renders only when computer powered on.

### Task 3: Right-Side Buttons And Shapes

**Files:**
- Modify models and block shape code.
- Modify resolver hit boxes.

**Interfaces:**
- Buttons on right side visually and interactively.
- Shapes approximate model extents.

### Task 4: Verification

**Files:**
- Modify `build.gradle` and self tests.

**Interfaces:**
- Require cursor hidden, crosshair cancel, power states, and no desktop case control entry.
