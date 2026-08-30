# MineVM

MineVM is a Minecraft Forge 1.20.1 mod that embeds real QEMU virtual machines into Minecraft as placeable desktop and laptop computers.

## Current Stable Scope

- Desktop computer case, monitor, laptop, keyboard, mouse, HDMI cable, USB cable, and system disc items.
- In-world BIOS for booting installed disks or inserted system discs.
- Local QEMU backend with per-computer disks and managed VM state.
- VNC-based display, keyboard input, mouse input, cursor rendering, power control, and disc eject behavior.
- Tested guest paths for Ubuntu Server, Ubuntu Desktop, and Windows XP. Windows 10/11 discs are exposed for ongoing compatibility work.

## Build

Run:

```powershell
.\gradlew.bat build --console=plain
```

The release jar is written to:

```text
build\libs\minevm-1.0.0.jar
```

Drop the jar into a Forge 1.20.1 `mods` folder. Runtime tools such as QEMU are configured outside the jar through the generated MineVM config file.

## Runtime Configuration

MineVM expects local executable paths and ISO paths to be configured in the Forge common config. The important values are the QEMU executable paths, VM storage directory, and per-disc ISO paths for supported guests.

VM disks are ordinary local files under the configured VM storage directory. Keep enough free disk space for the guest operating systems you install.
