# BIOS Action Network Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Wire BIOS buttons to server-authoritative actions without letting the client UI or block entity directly manage QEMU.

**Architecture:** Add a Forge `SimpleChannel` with one C2S BIOS action packet carrying a computer `BlockPos` and an enum action. Client buttons send only intent; the server validates player distance and block entity, then delegates to a service that uses `VirtualMachineManager` asynchronously. Disc boot creates the VM using the inserted disc OS profile; disk boot starts an already-created VM only.

**Tech Stack:** Minecraft Forge 1.20.1, Java 17, Forge networking, existing VM manager/backend abstractions, Gradle self-test verification.

## Global Constraints

- Minecraft client screen must not call QEMU, QMP, or `VirtualMachineManager` directly.
- BlockEntity must only store computer data and disc state.
- Server packet handling must validate player, distance, and block entity type.
- VM create/start/stop must remain asynchronous through `VirtualMachineManager`.
- OS installation flow starts by booting an inserted system disc.
- Tests must not launch QEMU.

---

### Task 1: Add BIOS Action Model

**Files:**
- Create: `src/main/java/com/example/vm/computer/ComputerBiosAction.java`
- Create: `src/main/java/com/example/vm/computer/ComputerBiosActions.java`
- Modify: `src/main/java/com/example/vm/config/ComputerRuntimeConfig.java`

**Interfaces:**
- Produces: `ComputerBiosAction` enum values `BOOT_FROM_DISC`, `BOOT_VM_DISK`, `POWER`, `EJECT_DISC`
- Produces: `ComputerBiosActions.handle(ServerPlayer player, BlockPos pos, ComputerBiosAction action): void`
- Produces: `ComputerRuntimeConfig.descriptorFor(ComputerBlockEntity computer, GuestOperatingSystem guestOperatingSystem): VirtualMachineDescriptor`

- [x] **Step 1: Define BIOS action enum**

Use stable enum names for packet encoding and verification.

- [x] **Step 2: Implement server action service**

Validate player distance, find `ComputerBlockEntity`, send player feedback, and call `VirtualMachineManager` asynchronously.

- [x] **Step 3: Add descriptor overload for disc OS**

Build descriptors from computer hardware plus a guest OS selected from the inserted disc.

### Task 2: Add Forge Network Packet

**Files:**
- Create: `src/main/java/com/example/vm/network/ModNetwork.java`
- Create: `src/main/java/com/example/vm/network/ComputerBiosActionPacket.java`
- Modify: `src/main/java/com/example/vm/ComputerInMinecraft.java`

**Interfaces:**
- Produces: `ModNetwork.register(): void`
- Produces: `ModNetwork.sendBiosAction(BlockPos pos, ComputerBiosAction action): void`
- Produces: `ComputerBiosActionPacket.encode`, `decode`, and `handle`

- [x] **Step 1: Register a `SimpleChannel`**

Use mod id `computer_in_minecraft` and protocol version `1`.

- [x] **Step 2: Implement packet encode/decode/handle**

Encode `BlockPos` plus enum action; handle on server with `ComputerBiosActions.handle`.

- [x] **Step 3: Register networking during mod construction**

Call `ModNetwork.register()` from the mod constructor after registries are attached.

### Task 3: Wire BIOS Buttons and Verification

**Files:**
- Modify: `src/main/java/com/example/vm/client/ComputerBiosScreen.java`
- Modify: `build.gradle`

**Interfaces:**
- Client buttons send the matching `ComputerBiosAction` through `ModNetwork`.
- Verification rejects direct VM/QEMU calls in the client screen.

- [x] **Step 1: Wire each button**

Map `Boot from Disc`, `Boot VM Disk`, `Power`, and `Eject Disc` to their enum values.

- [x] **Step 2: Add verification task**

Check packet, channel registration, server validation, and client button mappings.

- [x] **Step 3: Run full build**

Run: `.\gradlew.bat build --no-daemon --max-workers=1 --console=plain --stacktrace`
Expected: `BUILD SUCCESSFUL`

## Self-Review

Spec coverage: BIOS actions are now routed through server networking and VM manager boundaries; installation remains disc-only.

Placeholder scan: No placeholder instructions remain.

Type consistency: Packet, enum, network registration, and button mapping names match across tasks.
