# Rotated Shapes And Power State Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rotate block shapes with facing, prevent control when hardware is off, and make BIOS Power Off equivalent to pressing the physical computer power button.

**Architecture:** Define north-facing base voxel shapes and rotate them per block state using a small reusable helper. Gate immersive control at server interaction entry using persisted monitor/computer power state. Keep BIOS action execution server-side and update `ComputerBlockEntity.poweredOn` during BIOS power off.

**Tech Stack:** Forge 1.20.1, Java 17, voxel shapes, block entity NBT/update packets, Gradle verification.

## Global Constraints

- No GUI fallback.
- Desktop control starts from monitor only.
- Laptop/desktop cannot enter screen control while powered off.
- BIOS Power Off changes the same computer power state as the physical power button.
- Full verification: `./gradlew.bat build --no-daemon --max-workers=1 --console=plain --stacktrace`.

---

### Task 1: Rotated Shapes
- Create `src/main/java/com/example/vm/block/RotatedShapes.java`.
- Modify computer/monitor/peripheral blocks to return facing-rotated shapes.
- Add self-test coverage or build markers.

### Task 2: Control Power Gates
- Modify monitor block to reject control when display off or linked case off.
- Modify laptop block to reject control when laptop off.

### Task 3: BIOS Power Off State
- Modify `ComputerBiosActions.powerOff` to set `poweredOn` false, matching the physical power button.
- Keep VM stop behavior.

### Task 4: Verification
- Update Gradle checks.
- Run full build.
