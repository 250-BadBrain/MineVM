# Project Audit Repair Plan

**Goal:** Repair reproducible media, QMP, screen, and asynchronous gameplay defects in the existing development tree.

**Architecture:** Preserve the current Forge registries and backend interfaces. Treat physical media changes as a server-thread transaction, carry media identity through cold starts, and verify QMP behavior against an isolated local QEMU instance.

**Tech Stack:** Java 17, Forge 1.20.1, QEMU QMP/RFB, Gradle self tests.

## Constraints

- Preserve existing uncommitted work and virtual disk contents.
- Do not start Minecraft or modify the player's installed guest disks.
- Preserve existing boot-request constructors for callers.

## Tasks

- [x] Correct QMP eject backend addressing; test command errors, misleading events, and failed handshake cleanup in `QmpClientSelfTest`.
- [x] Reserve the inserted item before asynchronous I/O, serialize changes per drive, refund on failure, and detach media on drive removal in `FloppyDriveBlock` and `FloppyDriveBlockEntity`.
- [x] Carry inserted writable/boot media through hard-disk and disc cold starts in `VirtualMachineBootRequest`, `ComputerBiosActions`, `LocalQemuBackend`, and `QemuCommandBuilder`. Assert empty-drive presence and read-write image paths in `LocalQemuBackendSelfTest`.
- [x] Run independent long-lived VNC receivers concurrently in `ScreenService`; verify two fake VNC servers connect simultaneously in `ScreenServiceSelfTest`.
- [x] Keep BIOS continuation mutations on the original server level and reject stale/powered-off computers in `ComputerBiosActions`.
- [x] Run `./gradlew.bat build --console=plain`, `git diff --check`, and an isolated real-QEMU removable-media check. Record remaining verification limits.

## Additional Confirmed Repairs

- Removed the handcrafted Win95 partition-table sector, which contained no executable MBR bootstrap. New virtual disks remain blank for the guest installer to partition.
- Changed the Gradle wrapper from a missing local ZIP to the official 8.8 distribution.
- Restricted network packet directions, rejected remote BIOS requests before any chunk lookup, and rejected dot-directory disk names and invalid persisted computer IDs.
- Settled pending VM futures on shutdown and returned queue saturation through failed futures. Serialized QEMU lifecycle operations with close to prevent process creation racing shutdown.
- Preserved VNC framing when clipboard, colour-map, and bell messages arrive between screen updates.

## Verification

Command: `./gradlew.bat build runQemuMediaIntegrationSelfTest -PqemuSystemPath=D:/qemu/qemu-system-x86_64.exe --console=plain`

Result: BUILD SUCCESSFUL, 43 actionable tasks, 40 executed and 3 up-to-date.

`git diff --check` passed. The real-QEMU check verified a zeroed new disk sector, an empty persistent floppy drive, writable cold-start media, read-only/read-write hot swaps, and actual removal reported by `query-block` after eject. Disposable processes exited and the final run removed its temporary media.

No Minecraft client was launched. Full Win95 installation, physical-item interactions, and multiplayer gameplay remain manual regression tests. Existing guest disks were not modified. Compiler deprecation notices remain; they are not build failures.
