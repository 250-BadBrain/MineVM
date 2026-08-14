# Disc-Only BIOS Flow Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove the misleading standalone OS install action from the BIOS and make all installation flow through booting an inserted system disc.

**Architecture:** Keep BIOS as a device boot menu, not an installer UI. The screen exposes bootable devices and hardware actions only; later installation logic will attach to the disc boot path and VM backend. Verification ensures the old `Install OS` action does not reappear in the BIOS foundation.

**Tech Stack:** Minecraft Forge 1.20.1, Java 17, Forge client screen, Gradle verification.

## Global Constraints

- BIOS UI must not call QEMU or QMP directly.
- OS installation must be initiated from an inserted system disc, not a standalone online install option.
- BlockEntity must not manage VM processes.
- Opening BIOS must not block the Minecraft thread.
- Tests must not launch QEMU.

---

### Task 1: Replace Installer Action With Disk Boot Action

**Files:**
- Modify: `src/main/java/com/example/vm/client/ComputerBiosScreen.java`
- Modify: `build.gradle`

**Interfaces:**
- Consumes: `ComputerBiosMenu.computerPos(): BlockPos`
- Produces: BIOS buttons named `Boot from Disc`, `Boot VM Disk`, `Power`, and `Eject Disc`

- [x] **Step 1: Update BIOS visible actions**

Replace the `Install OS` button with `Boot VM Disk`, keeping the same button layout and leaving button handlers empty until backend commands are wired.

- [x] **Step 2: Update BIOS explanatory text**

Replace `Install source: inserted disc metadata` and `ISO binding will be configured in the next stage.` with text explaining that setup runs by booting an inserted disc.

- [x] **Step 3: Strengthen verification**

Update `verifyBiosMenuFoundation` so it requires `Boot VM Disk` and fails if `Install OS` appears in `ComputerBiosScreen.java`.

- [x] **Step 4: Run build**

Run: `.\gradlew.bat build --no-daemon --max-workers=1 --console=plain --stacktrace`
Expected: `BUILD SUCCESSFUL`

## Self-Review

Spec coverage: The BIOS no longer exposes a standalone install action, and installation semantics now point to booting an inserted system disc.

Placeholder scan: No placeholder instructions remain.

Type consistency: The task only changes visible BIOS text and verification checks; no public Java interface changes are introduced.
