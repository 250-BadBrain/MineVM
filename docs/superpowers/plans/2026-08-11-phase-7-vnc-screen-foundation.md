# Phase 7 VNC Screen Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the first screen display foundation by exposing a per-computer VNC endpoint from QEMU and creating async screen service primitives for later frame receiving/rendering.

**Architecture:** Keep VNC endpoint calculation in `screen`, QEMU command wiring in `LocalQemuBackend`, and game interaction in `ComputerBlock`. Frame transfer is represented by immutable screen frame records and an async service boundary, not by tick polling.

**Tech Stack:** Minecraft Forge 1.20.1, Java 17, QEMU VNC, Gradle JavaExec self-tests.

## Global Constraints

- Minecraft main thread must not receive or decode VM screen data.
- Screen receiving must be asynchronous.
- No per-tick screenshots.
- QEMU remains behind `VirtualMachineBackend`.
- Tests must not launch QEMU or connect to VNC.

---

### Task 1: VNC Endpoint Model

**Files:**
- Create: `src/main/java/com/example/vm/screen/VncEndpoint.java`
- Create: `src/main/java/com/example/vm/test/VncEndpointSelfTest.java`

**Interfaces:**
- Produces: `VncEndpoint.forComputer(String computerId): VncEndpoint`
- Produces: `toQemuArgument(): String`
- Produces: `toDisplayAddress(): String`

- [x] **Step 1: Add deterministic endpoint mapping**

Map computer id to local QEMU VNC display numbers in a bounded range.

- [x] **Step 2: Add self-test**

Assert endpoint stability and QEMU argument format.

### Task 2: Screen Service Boundary

**Files:**
- Create: `src/main/java/com/example/vm/screen/ScreenFrame.java`
- Create: `src/main/java/com/example/vm/screen/ScreenService.java`
- Create: `src/main/java/com/example/vm/test/ScreenServiceSelfTest.java`

**Interfaces:**
- Produces: `ScreenService.updateFrame(String computerId, ScreenFrame frame): CompletableFuture<Void>`
- Produces: `ScreenService.getLatestFrame(String computerId): CompletableFuture<Optional<ScreenFrame>>`

- [x] **Step 1: Add immutable frame record**

Store width, height, changed rectangle, and ARGB pixels.

- [x] **Step 2: Add async service**

Use a daemon executor and never require Minecraft tick polling.

- [x] **Step 3: Add self-test**

Assert frame storage/retrieval and executor thread use.

### Task 3: QEMU VNC Wiring And Game Entry

**Files:**
- Modify: `src/main/java/com/example/vm/vm/backend/local/QemuCommandBuilder.java`
- Modify: `src/main/java/com/example/vm/block/ComputerBlock.java`
- Modify: `src/main/java/com/example/vm/ComputerInMinecraft.java`
- Modify: `src/main/java/com/example/vm/test/LocalQemuBackendSelfTest.java`
- Modify: `build.gradle`

**Interfaces:**
- QEMU start command includes `-vnc 127.0.0.1:<display>`.
- Clicking display area sends the VNC endpoint message in-game.

- [x] **Step 1: Add VNC to QEMU command**

QEMU exposes a per-computer localhost-only VNC server.

- [x] **Step 2: Add in-game display endpoint message**

Display-area clicks show the VNC endpoint as the Phase 7 visible checkpoint.

- [x] **Step 3: Add verification task**

Run build and assert all Phase 7 files/classes/command args are present.

## Self-Review

Spec coverage: This builds the VNC display foundation while preserving asynchronous screen boundaries and avoiding tick screenshots.

Placeholder scan: No placeholder instructions remain.

Type consistency: All referenced classes and methods match this plan.
