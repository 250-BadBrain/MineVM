# QEMU Preflight Connection Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Connect the existing LocalQemuBackend safely to in-game power/BIOS operations with clear preflight feedback.

**Architecture:** Add a read-only runtime preflight report that validates backend selection, QEMU executables, VM storage, and disc ISO paths without launching QEMU. Use it from physical power and Boot Disc actions before create/start. Keep all VM execution through `VirtualMachineManager` async methods.

**Tech Stack:** Forge 1.20.1, Java 17, QEMU CLI, Gradle self-tests.

## Global Constraints

- No QEMU work on Minecraft main thread.
- Default backend remains Noop unless config enables local QEMU.
- Boot Disc requires an inserted disc and configured ISO path.
- Power button with NOT_CREATED creates then starts.
- Full verification: `./gradlew.bat build --no-daemon --max-workers=1 --console=plain --stacktrace`.

---

### Task 1: Runtime Preflight Report
- Add `ComputerRuntimePreflight` with `validatePowerOn` and `validateSystemDiscBoot`.
- It reports missing QEMU paths, storage errors, backend disabled, and ISO errors.

### Task 2: Use Preflight In Actions
- Physical power button checks power-on preflight before create/start.
- BIOS Boot Disc checks ISO preflight before backend call.
- Messages are concise and player-facing.

### Task 3: Tests And Verification
- Add self-test for preflight disabled/default and missing ISO messages.
- Add Gradle verification markers.
- Run full build.
