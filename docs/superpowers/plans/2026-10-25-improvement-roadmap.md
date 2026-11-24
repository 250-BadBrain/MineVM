# MineVM Usability and Multiplayer Implementation Plan

**Goal:** Implement the six requested improvement directions while preserving single-JAR packaging and existing virtual disks.

**Architecture:** Keep QEMU and media work on the VM executor. Add physical peripheral identity, managed disk maintenance, explicit compatibility evidence, frame metrics, replaceable transport/codec interfaces, and server-authoritative multiplayer subscriptions and control leases. Keep the current VNC source; H.264 remains an upgrade selected after profiling rather than an unmeasured default.

**Tech Stack:** Forge 1.20.1, Java 17, QEMU, Java Deflater/Inflater, Forge networking and commands. No additional runtime library.

## Ordered Deliverables

- [x] Compatibility registry, documented installation matrix and report command; distinguish user-reported, automated, and not-yet-tested results. Add Win98 and Vista profiles with experimental status, not invented successful installations.
- [x] Persisted peripheral block entities and reciprocal keyboard/mouse wiring; unlink on destruction/rewiring; reject unloaded, distant, cross-dimension and replaced endpoints.
- [x] Player-visible configuration checks, runtime diagnostics, stopped-only disk backup/restore/reset commands with retained recovery copies and permission checks.
- [x] Frame receive/merge/upload/network metrics, QEMU process CPU time, replaceable screen transport and codec interfaces; bounded compressed/raw frame paths.
- [x] Chinese/English messages for new workflows, recipes, first-login guidance and installation/configuration documentation. Existing art stays unchanged.
- [x] Server-owned frame subscriptions, bounded async frame encoding/decoding, viewing radius and bandwidth limits, exclusive renewable control leases, release/disconnect handling and explicit VM ownership.
- [x] Focused self tests, full build, real-QEMU media/maintenance validation, dedicated-server classloading checks where feasible, and a manual multiplayer/OS-installation checklist.

## Verification Rules

- All destructive maintenance requires a stopped VM and an authorized nearby player; reset archives the previous disk.
- Network frame decoders validate dimensions and exact decompressed length; reject truncated or oversized payloads.
- Pure tests do not mark operating-system installations as verified.
- Never delete an existing player's guest disk during development verification.

## Completion Notes

The implementation and automated layers are complete. Full Windows 95/98/Vista installations, 15-minute per-guest idle checks and two-client gameplay remain explicitly manual checks in docs/COMPATIBILITY.md and docs/TESTING.md. H.264 is not implemented or required; this iteration supplies replaceable interfaces, profiling and bounded RAW/DEFLATE multiplayer delivery without new runtime dependencies. Existing keyboard/mouse links require one deliberate reconnection after migration from boolean-only wiring.
