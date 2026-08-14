# Complete Wiring Immersive Control Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Complete HDMI/USB wiring, laptop built-in device logic, in-world BIOS/status screen rendering, and Ctrl+Alt immersive control exit.

**Architecture:** Desktop computer cases own VM/data and USB peripheral state. Monitors store linked computer ids via HDMI. Laptops use their existing computer block entity but expose built-in screen/keyboard/mouse capabilities. A client-side screen-state renderer draws BIOS/status text on monitor and laptop block surfaces, while a network-triggered control session captures the user until Ctrl+Alt exits.

**Tech Stack:** Minecraft Forge 1.20.1, Java 17, Forge networking, block entities, item NBT, client render events.

## Global Constraints

- No QEMU or VM backend calls from client rendering/input classes.
- HDMI connects computer case to monitor.
- USB connects keyboard and mouse to computer case.
- Laptop has built-in screen, keyboard, and mouse capability.
- BIOS/OS status must be visible on the in-world monitor/laptop surface.
- Ctrl+Alt exits immersive control.
- Tests must not launch QEMU.

---

### Task 1: Persist Wiring State

**Files:**
- Modify: `ComputerBlockEntity.java`
- Modify: `ComputerMonitorBlockEntity.java`

**Interfaces:**
- Computer block entity stores linked monitor pos, keyboard connected, mouse connected.
- Monitor block entity stores linked computer id and linked computer pos.

- [x] **Step 1: Add NBT-backed fields**

Use `LinkedMonitor`, `KeyboardConnected`, `MouseConnected`, `LinkedComputerId`, `LinkedComputerPos`.

### Task 2: Implement HDMI And USB Items

**Files:**
- Modify: `HdmiCableItem.java`
- Modify: `UsbCableItem.java`
- Modify: `ComputerPeripheralItem.java`

**Interfaces:**
- HDMI first click computer case, second click monitor.
- USB cable connects offhand keyboard/mouse to computer case.

- [x] **Step 1: Implement item interactions**

Use item NBT for pending HDMI source.

### Task 3: Implement Immersive Control Network

**Files:**
- Create packets for start/stop control.
- Modify monitor and computer blocks.
- Modify `ModNetwork.java`.

**Interfaces:**
- Right-click linked monitor enters control when case has keyboard and mouse.
- Right-click laptop enters control using built-ins.

- [x] **Step 1: Add start control packet**

Server validates links/capabilities then sends client control session.

### Task 4: Client Control Session And Ctrl+Alt Exit

**Files:**
- Create `ImmersiveControlClient.java`
- Modify `ClientModEvents.java`

**Interfaces:**
- Client stores active session.
- Ctrl+Alt exits session.
- Mouse/key events are consumed while active where Forge exposes cancellable events.

- [x] **Step 1: Add event handlers**

Keep implementation side-effect-free with respect to VM backend.

### Task 5: In-World Screen Rendering

**Files:**
- Create monitor and laptop renderers.
- Register BERs in client events.

**Interfaces:**
- Monitor renders linked/unlinked status.
- Laptop renders built-in BIOS/status label.

- [x] **Step 1: Add text rendering on block surfaces**

Use Font drawing on block entity renderers.

### Task 6: Verification

**Files:**
- Modify `build.gradle`

**Interfaces:**
- Verify wiring fields, packet registration, renderers, Ctrl+Alt marker, and item behavior markers.

- [x] **Step 1: Run full build**

Run full Gradle build.

## Self-Review

Spec coverage: This plan covers HDMI, USB, laptop built-ins, in-world screen rendering, immersive control, and Ctrl+Alt exit.

Placeholder scan: No placeholder instructions remain.

Type consistency: Packet, item, renderer, and block entity names match planned files.
