# MineVM Code Review, 2026-10-25

## Fixed Findings

1. QMP eject used a backend name as a qdev identifier. Real QEMU returned DeviceNotFound. Eject now addresses the backend with the matching device argument; integration checks query actual inserted media after ejection.
2. Soft-floppy insertion consumed the player's currently held stack after asynchronous I/O. Switching slots could duplicate a disk, and overlapping requests could desynchronize physical and virtual media. The item is now reserved immediately, changes are serialized per drive, and failed operations refund it. Removed drives detach their medium, and stale links are rejected.
3. Hard-disk boot omitted floppy media and sometimes omitted the drive itself. Cold starts now carry inserted media identity, prepare writable images, and retain an empty legacy drive for later insertion.
4. A single long-lived VNC receiver blocked all subsequent VMs. Receivers now use a bounded concurrent pool. Clipboard, colour-map, and bell messages no longer corrupt framebuffer parsing.
5. QMP response parsing matched strings inside events and leaked sockets when negotiation failed. Parsing now inspects JSON response fields, command errors do not trigger duplicate sends, and failed negotiations close their socket. A failed media command no longer incorrectly marks a live VM as failed.
6. BIOS continuations modified block entities off the server thread and looked up completion targets through the player's current dimension. They now mutate the original validated computer on the server thread and reject removed or powered-off computers. Shutdown completion also targets the original entity.
7. Queue saturation could throw into gameplay code, and shutdown abandoned pending futures. Failed futures now represent rejected operations, pending VM futures settle on shutdown, and backend lifecycle calls are serialized against close.
8. Network packet registrations did not restrict direction. They now distinguish client-bound from server-bound messages. BIOS distance/loading checks run before block-entity lookup. Managed paths reject dot-directory names, and invalid persisted computer IDs are regenerated.
9. Win95 disk creation wrote a partition table without executable MBR boot code. Creation now leaves the disk blank, so FDISK can install its normal partition table and bootstrap. Existing disks are not rewritten.
10. The Gradle wrapper referenced a deleted ZIP in a developer's Downloads directory. It now uses the official Gradle 8.8 distribution.

## Validation

The complete Gradle build and opt-in real-QEMU integration test passed together: 43 tasks, 40 executed, 3 up-to-date. Whitespace checks passed. The integration test used disposable files only and validated new-disk initialization, empty-drive startup, writable media startup, hot swapping, write protection, and actual ejection.

Run the optional integration test with an installed QEMU executable:

```powershell
.\gradlew.bat runQemuMediaIntegrationSelfTest '-PqemuSystemPath=D:/qemu/qemu-system-x86_64.exe' --console=plain
```

That test also uses qemu-img.exe and qemu-io.exe from the same installation. Normal mod operation no longer needs qemu-io.exe for disk initialization.

The updated artifact is build/libs/minevm-1.1.0-dev.jar. No Minecraft client was launched and no existing guest disk was modified. Complete guest installation and physical-item interaction need an in-game regression pass; these checks do not establish that Windows 95's IOS protection error is resolved for every installation image.

For a new Win95 installation, create an active primary DOS partition with FDISK, reboot the guest, format the partition, then run Setup from the CD. Rewire floppy drives while connected computers are off and no media change is pending.
