# Guest Compatibility Evidence

Status is deliberately conservative. USER_REPORTED means the developer/user previously reported a working installation path; it is not an automated reproduction, and older image revisions were not all recorded. INSTALLATION_PENDING means a profile exists but the complete install-to-cold-boot path is not established. `/minevm compatibility` reports the same distinction in game.

| Guest | Evidence | Image/version record | Profile |
| --- | --- | --- | --- |
| Ubuntu Server | USER_REPORTED | Revision not recorded | BIOS, virtio disk/video/network |
| Ubuntu Desktop | USER_REPORTED | Revision not recorded | BIOS, virtio disk/video/network |
| FreeDOS | USER_REPORTED | 1.4 LiveCD | BIOS, IDE, qemu32, one core, standard VGA |
| Windows XP | USER_REPORTED | Revision not recorded | BIOS, IDE, qemu32, at most two cores, Cirrus |
| Windows 95 | INSTALLATION_PENDING | OSR2/OSR2.5 investigated; final boot failures remain a required regression check | BIOS, IDE, Pentium, single-thread TCG, at most 256 MiB, Cirrus |
| Windows 98 | INSTALLATION_PENDING | Second Edition candidate; no completed installation recorded | BIOS, IDE, Pentium, single-thread TCG, at most 256 MiB, Cirrus |
| Windows Vista | INSTALLATION_PENDING | 32-bit candidate; no completed installation recorded | BIOS, IDE, qemu32, at most two cores, standard VGA, e1000 |
| Windows 10 | INSTALLATION_PENDING | Profile only | UEFI/SATA; installation not verified |
| Windows 11 | INSTALLATION_PENDING | Profile only | UEFI/Secure Boot/TPM requested; complete provisioning not verified |

Win98/Vista have distinct configurable ISO entries, disc items and recipes. These additions do not certify all editions, language variants, service packs, modified ISOs or floppy sets. Installation media must contain a suitable boot path and the appropriate drivers. In particular, the currently configured Win95 boot floppy is not a generic boot-floppy implementation for every guest.

For a new disk, configured capacity is subject to guest minimums: FreeDOS 1 GiB, Win95 2 GiB, Win98 2 GiB, XP 10 GiB, Ubuntu Desktop 25 GiB, Vista 32 GiB, modern Windows 64 GiB. Win95 is also capped at 2 GiB. Existing disks are not resized. The config default is 20 GiB, so a minimum of 2 GiB does not imply a Win98 disk is always 2 GiB.

## Required Manual Checks

Record one row per actual image revision, not just one row per product name:

| Image revision / SHA-256 | QEMU version / accelerator | Fresh install | Disk cold boot | Restart | Shutdown | Media swap | Input | Idle 15 min | Result / logs |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Pending | Pending | Pending | Pending | Pending | Pending | Pending | Pending | Pending | Pending |

Run in this order: Win95 final boot regression, Win98 SE, 32-bit Vista. Run modern Windows later, separately from the legacy BIOS path. For each candidate:

1. Use a new test computer or a stopped VM reset after backing up important data. Record JAR revision, image SHA-256, QEMU version and effective command from `qemu-command.txt`.
2. Complete partitioning, activation, formatting and installation using the guest's tools. Do not mark an installer welcome screen as a successful installation.
3. Remove only the media requested by the installer. Keep installation media available while it still needs source files. Test swapping the read-only boot floppy for an independently writable blank floppy.
4. Boot from the installed disk without depending on a boot floppy, including after leaving and reopening the Minecraft world.
5. Verify guest restart, guest shutdown, in-game power cycle, peripheral removal/reconnection, corners/center clicking, modifiers and scrolling.
6. Wait at least 15 minutes without input, then resume control without rebooting the guest. A successful FreeDOS check does not establish this for GUI guests.
7. Save the exact failure message plus `/minevm status` and QEMU stderr if anything fails. Do not apply `FDISK` or formatting to an existing valuable disk just to diagnose a boot error.

## Automated Evidence Is Different

Self-tests check profile command lines, codec bounds, lease expiry and recovery semantics. Real QEMU tests check empty MBR creation, writable/read-only media swaps, actual QMP ejection, stopped-only maintenance and guest-profile restoration using disposable disks. They do not install Windows, exercise a desktop GUI or prove an image works.

Do not distribute proprietary system images, product keys or modified boot images with MineVM. Users supply installation media they are entitled to use. Record known-working versions and hashes without bundling those files.
