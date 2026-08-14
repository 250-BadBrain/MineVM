# Power Start And Particles Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make first power press create and start the VM, and fix custom block break particles.

**Architecture:** Keep physical power button as the only Power On path. On `NOT_CREATED`, create the VM descriptor then immediately start it so player-visible state becomes RUNNING/STARTING instead of STOPPED. Add `particle` textures to block models so Minecraft has a valid particle texture for destroy effects.

**Tech Stack:** Forge 1.20.1, Java 17, Gradle verification, block model JSON.

## Global Constraints

- No GUI fallback.
- Power button first press transitions `NOT_CREATED` toward running state.
- Block models must define valid `particle` textures.
- Full verification: `./gradlew.bat build --no-daemon --max-workers=1 --console=plain --stacktrace`.

---

### Task 1: First Power Press Starts VM
- Modify `ComputerBlock.registerVirtualMachine` to create then start.
- Update message to say VM started.
- Remove stale registration-only helper text.

### Task 2: Particle Textures
- Add `particle` texture mapping to all custom block models.
- Extend Gradle verification.

### Task 3: Verification
- Run full build.
