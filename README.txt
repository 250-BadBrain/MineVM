# MineVM

MineVM is a Minecraft Forge 1.20.1 mod that embeds real QEMU virtual machines into Minecraft as placeable desktop and laptop computers.

## Development Paused

Development is paused as of 2026-11-25. The current source snapshot is 1.1.0-dev, not a new stable release. Start with docs/HANDOFF.md before resuming work; it records the implementation state, remaining validation, cleanup status and rebuild commands.

## Current Development Scope

- Desktop computer case, monitor, laptop, physical keyboard and mouse, HDMI/USB cables, floppy drive, boot floppies, writable blank floppies, and system disc items.
- In-world BIOS for booting installed disks or inserted system discs.
- Local QEMU backend with per-computer disks and managed VM state.
- VNC-based display, keyboard input, mouse input, cursor rendering, power control, and disc eject behavior.
- User-reported installation paths for Ubuntu Server, Ubuntu Desktop, Windows XP, and FreeDOS. Windows 95 requires final boot regression testing. Windows 98 and 32-bit Vista have experimental profiles; Windows 10/11 remain unverified.
- Owner/trusted-player control, exclusive renewable control leases, and bounded multiplayer screen streaming using RAW or DEFLATE frames.
- Configuration/runtime diagnostics, frame metrics, stopped-only disk backup/restore/reset, English/Chinese workflow messages, and survival recipes.

## Guides

- Chinese setup, commands and safe disk maintenance: docs/GETTING_STARTED.zh-CN.md
- Compatibility evidence and installation checklist: docs/COMPATIBILITY.md
- Multiplayer limits, profiling and manual verification: docs/TESTING.md

Existing worlds that used the old keyboard/mouse boolean connections must reconnect those physical devices once. Existing VM disks are not migrated or deleted. New item artwork is deferred.

## Build

Run:

```powershell
.\gradlew.bat build --console=plain
```

The release jar is written to:

```text
build\libs\minevm-1.1.0-dev.jar
```

Drop the jar into a Forge 1.20.1 `mods` folder. Runtime tools such as QEMU are configured outside the jar through the generated MineVM config file.

## Runtime Configuration

MineVM expects local executable paths and ISO paths to be configured in the Forge common config. The important values are the QEMU executable paths, VM storage directory, and per-disc ISO paths for supported guests.

VM disks are ordinary local files under the configured VM storage directory. Keep enough free disk space for the guest operating systems you install.

Only the host/server needs QEMU, ISO files and writable disk storage. Clients need the matching mod JAR. Network protocol 3 is not compatible with earlier development builds. H.264/FFmpeg is not enabled or required by this version.
