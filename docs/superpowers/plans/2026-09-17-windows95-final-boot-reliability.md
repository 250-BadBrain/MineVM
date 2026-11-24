# Windows 95 Final Boot Reliability Implementation Plan

> Superseded in part by the 2026-10-25 audit: QMP eject `id` addresses a qdev, not a `-drive` backend. The current implementation uses `device` for backend names, verified against real QEMU.

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make Windows 95 media changes truthful at the QEMU layer and boot reliably after the installer finishes.

**Architecture:** Keep physical-media state in the floppy-drive block entity, but make the backend verify QMP media changes before Minecraft returns an item. Use a Windows 95-specific QEMU compatibility profile with an era-appropriate CPU, the cross-version `pc` i440FX/PIIX machine alias, and single-thread TCG rather than the generic TCG CPU model.

**Tech Stack:** Forge 1.20.1, Java 17, QEMU QMP, Gradle self-test tasks.

## Global Constraints

- Preserve the single-JAR mod distribution model and external QEMU paths in configuration.
- Do not distribute Windows 95 media.
- Keep FreeDOS, XP, and modern guest command lines unchanged unless a test requires shared behavior.

---

### Task 1: Verify QEMU floppy ejection semantics

**Files:**
- Modify: `src/main/java/com/example/vm/qmp/QmpCommand.java`
- Modify: `src/main/java/com/example/vm/vm/backend/local/LocalQemuBackend.java`
- Modify: `src/main/java/com/example/vm/test/QmpCommandSelfTest.java`

**Interfaces:**
- Produces: `QmpCommand.eject(String)` that uses the QMP command form QEMU accepts for an actual removable-device eject.
- Produces: `LocalQemuBackend.ejectBootFloppy(String)` that completes only after QMP acknowledges the eject.

- [ ] **Step 1: Add failing QMP JSON assertions**

```java
assertEquals(
    QmpCommand.eject("boot_floppy"),
    "{\"execute\":\"eject\",\"arguments\":{\"id\":\"boot_floppy\",\"force\":true}}"
);
```

- [ ] **Step 2: Run the QMP self test and confirm the old device-key JSON fails**

Run: `./gradlew.bat qmpCommandSelfTest --console=plain`
Expected: FAIL because `eject` uses `device` instead of `id`.

- [ ] **Step 3: Implement the QMP-compatible command and preserve error propagation**

```java
arguments.addProperty("id", deviceId);
arguments.addProperty("force", true);
```

- [ ] **Step 4: Run the QMP self test**

Run: `./gradlew.bat qmpCommandSelfTest --console=plain`
Expected: BUILD SUCCESSFUL.

### Task 2: Add a Windows 95 compatibility QEMU profile

**Files:**
- Modify: `src/main/java/com/example/vm/vm/backend/local/QemuCommandBuilder.java`
- Modify: `src/main/java/com/example/vm/test/LocalQemuBackendSelfTest.java`

**Interfaces:**
- Produces: Windows 95 command arguments that select the portable `pc` i440FX/PIIX alias, a Pentium-class CPU, and single-thread TCG.
- Preserves: `QemuCommandBuilder.startCommand(VirtualMachineDescriptor, VirtualMachineBootRequest)` public signature.

- [ ] **Step 1: Add failing Windows 95 command assertions**

```java
assertContains(floppyBootCommand, "pc");
assertContains(floppyBootCommand, "pentium");
assertContains(floppyBootCommand, "tcg,thread=single");
```

- [ ] **Step 2: Run the local backend self test and confirm the profile assertions fail**

Run: `./gradlew.bat localQemuBackendSelfTest --console=plain`
Expected: FAIL because Windows 95 currently runs with `qemu32`, multi-thread TCG, and no explicit legacy machine alias.

- [ ] **Step 3: Add the narrowly scoped legacy profile**

```java
if (descriptor.guestOperatingSystem() == GuestOperatingSystem.WINDOWS_95) {
    command.add("-machine");
    command.add("pc");
}
```

```java
case WINDOWS_95 -> {
    command.add("-cpu");
    command.add("pentium");
}
```

- [ ] **Step 4: Run the local backend self test**

Run: `./gradlew.bat localQemuBackendSelfTest --console=plain`
Expected: BUILD SUCCESSFUL.

### Task 3: Perform full regression verification

**Files:**
- Modify: none unless a verification failure identifies a defect.

**Interfaces:**
- Verifies: all custom checks and the assembled development JAR.

- [ ] **Step 1: Run the full build**

Run: `./gradlew.bat build --console=plain`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 2: Check the patch for whitespace errors**

Run: `git diff --check`
Expected: no whitespace-error output.

- [ ] **Step 3: Produce the updated development JAR**

Run: `Get-Item -LiteralPath 'build/libs/minevm-1.1.0-dev.jar'`
Expected: the file exists and has a current timestamp.
