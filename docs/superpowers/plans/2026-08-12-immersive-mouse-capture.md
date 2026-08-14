# Immersive Mouse Capture Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Prevent physical mouse movement from rotating the player camera while immersive screen control is active.

**Architecture:** Keep BIOS mouse cursor hidden for early BIOS authenticity. On entering control mode, capture the current client player rotation; during active control, continuously restore camera/body rotation and suppress Minecraft key bindings so mouse movement is reserved for future screen/VM input instead of player view control.

**Tech Stack:** Minecraft Forge 1.20.1, Java 17, client tick event, Gradle verification.

## Global Constraints

- `Ctrl+Alt` remains the release combo.
- BIOS remains keyboard/selection driven; no visible BIOS cursor is added.
- No GUI fallback is reintroduced.
- Full verification command: `./gradlew.bat build --no-daemon --max-workers=1 --console=plain --stacktrace`.

---

### Task 1: Camera Lock State

**Files:**
- Modify: `src/main/java/com/example/vm/client/ImmersiveControlClient.java`

**Interfaces:**
- Produces: `lockCameraToScreen()`
- Produces: `captureCurrentCamera()`

- [ ] Store yaw/pitch/body rotation on start.
- [ ] Restore rotations while active.
- [ ] Clear stored camera on stop.

### Task 2: Tick Integration

**Files:**
- Modify: `src/main/java/com/example/vm/client/ClientForgeEvents.java`

**Interfaces:**
- Consumes: `ImmersiveControlClient.lockCameraToScreen()`

- [ ] Call camera lock during active client tick after suppressing bindings.

### Task 3: Verification

**Files:**
- Modify: `build.gradle`

**Interfaces:**
- Requires source markers `lockCameraToScreen`, `captureCurrentCamera`, and rotation fields.

- [ ] Extend immersive control verification.
- [ ] Run full build.
