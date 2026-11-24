# Writable Floppy Media Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a physical blank writable floppy item that can be inserted into a linked floppy drive and used by Windows 95 Setup while the VM is running.

**Architecture:** Replace the floppy drive's boot-floppy boolean with a persisted media record containing a media kind and, for blank media, a stable disk identifier. The local QEMU backend creates a 1.44 MB raw image per blank floppy and exchanges the medium through QMP. The configured Windows 95 boot image remains read-only and is never used as the writable target.

**Tech Stack:** Forge 1.20.1, Java 17, QEMU QMP, raw 1.44 MB floppy images, existing self-test mains.

## Global Constraints

- The release artifact remains one MineVM JAR plus its existing configuration file.
- The configured Windows 95 boot floppy remains read-only.
- Blank floppies are 1,474,560-byte raw images stored below the owning VM directory.
- Runtime media changes must be issued through QMP only while the VM is running.
- Existing saved drives with `InsertedWin95BootFloppy=true` must load as a Windows 95 boot floppy.

---

### Task 1: Persist floppy media rather than a boot-floppy flag

**Files:**
- Create: `src/main/java/com/example/vm/floppy/FloppyMedia.java`
- Modify: `src/main/java/com/example/vm/blockentity/FloppyDriveBlockEntity.java`
- Modify: `src/main/java/com/example/vm/block/FloppyDriveBlock.java`
- Test: `src/main/java/com/example/vm/test/FloppyMediaSelfTest.java`

**Interfaces:**
- Produces `FloppyMedia.bootFloppy()`, `FloppyMedia.blank(String)`, `kind()`, `mediaId()`, and `toItemStack()`.
- Produces `FloppyDriveBlockEntity.insert(FloppyMedia)`, `eject()`, and `media()`.

- [ ] **Step 1: Write the failing media test**

```java
FloppyMedia blank = FloppyMedia.blank("disk-1");
assertEquals(blank.kind(), FloppyMedia.Kind.BLANK);
assertEquals(blank.mediaId(), "disk-1");
assertEquals(FloppyMedia.bootFloppy().kind(), FloppyMedia.Kind.WINDOWS_95_BOOT);
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew runFloppyMediaSelfTest`

Expected: compilation failure because `FloppyMedia` does not exist.

- [ ] **Step 3: Implement the media record and migrate NBT**

```java
public record FloppyMedia(Kind kind, String mediaId) {
    public enum Kind { WINDOWS_95_BOOT, BLANK }
    public static FloppyMedia bootFloppy() { return new FloppyMedia(Kind.WINDOWS_95_BOOT, ""); }
    public static FloppyMedia blank(String id) { return new FloppyMedia(Kind.BLANK, id); }
}
```

Store `MediaKind` and `MediaId` in the drive block entity. On load, map the legacy `InsertedWin95BootFloppy` value to `FloppyMedia.bootFloppy()` when the new fields are absent.

- [ ] **Step 4: Implement insertion, ejection, drops, and status messages**

```java
if (heldItem.is(ModItems.BLANK_FLOPPY.get())) {
    FloppyMedia media = FloppyMedia.blank(BlankFloppyItem.ensureMediaId(heldItem));
    insertOrScheduleMediaChange(..., media, heldItem);
}
```

Use the returned media to recreate the matching item on eject and when the drive block drops.

- [ ] **Step 5: Run focused verification**

Run: `./gradlew compileJava runFloppyMediaSelfTest`

Expected: `BUILD SUCCESSFUL`.

### Task 2: Add the blank floppy item and runtime QMP medium exchange

**Files:**
- Create: `src/main/java/com/example/vm/item/BlankFloppyItem.java`
- Modify: `src/main/java/com/example/vm/registry/ModItems.java`
- Modify: `src/main/java/com/example/vm/registry/ModCreativeTabs.java`
- Modify: `src/main/java/com/example/vm/qmp/QmpCommand.java`
- Modify: `src/main/java/com/example/vm/vm/backend/VirtualMachineBackend.java`
- Modify: `src/main/java/com/example/vm/vm/backend/NoopVirtualMachineBackend.java`
- Modify: `src/main/java/com/example/vm/vm/VirtualMachineManager.java`
- Modify: `src/main/java/com/example/vm/vm/backend/local/LocalQemuBackend.java`
- Test: `src/main/java/com/example/vm/test/QmpCommandSelfTest.java`
- Test: `src/main/java/com/example/vm/test/VirtualMachineManagerSelfTest.java`

**Interfaces:**
- Produces `BlankFloppyItem.ensureMediaId(ItemStack)`.
- Produces `VirtualMachineBackend.changeBootFloppyMedium(String, Path, boolean)` and manager equivalent.
- Produces `QmpCommand.changeMedium(String, Path, boolean)`.

- [ ] **Step 1: Write the failing QMP command test**

```java
assertEquals(
    QmpCommand.changeMedium("boot_floppy", Path.of("E:/MinecraftVMs/c/floppies/disk.img"), false),
    "{\\\"execute\\\":\\\"blockdev-change-medium\\\",...}"
);
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew runQmpCommandSelfTest`

Expected: compilation failure because `changeMedium` does not exist.

- [ ] **Step 3: Implement stable blank-media storage and QMP exchange**

```java
Path image = commandBuilder.blankFloppyPath(descriptor, mediaId);
Files.createDirectories(image.getParent());
if (!Files.exists(image)) {
    Files.write(image, new byte[1_474_560], StandardOpenOption.CREATE_NEW);
}
sendQmpJson(computerId, QmpCommand.changeMedium(BOOT_FLOPPY_DEVICE_ID, image, false));
```

For a Windows 95 boot floppy, select the existing patched boot-floppy working copy and pass `read-only=true`. For a blank floppy, pass `read-only=false`.

- [ ] **Step 4: Wire the blank item into registry, creative tab, and drive interaction**

```java
public static final RegistryObject<Item> BLANK_FLOPPY =
    ITEMS.register("blank_floppy", () -> new BlankFloppyItem(new Item.Properties().stacksTo(1)));
```

When a linked VM is running, wait for the medium-exchange future before consuming the held item and updating the block entity. On failure, leave the item in the player's hand and show the QMP error.

- [ ] **Step 5: Run focused verification**

Run: `./gradlew compileJava runQmpCommandSelfTest runVirtualMachineManagerSelfTest`

Expected: `BUILD SUCCESSFUL`.

### Task 3: Package resources and run full verification

**Files:**
- Create: `src/main/resources/assets/minevm/models/item/blank_floppy.json`
- Modify: `src/main/resources/assets/minevm/lang/en_us.json`
- Modify: `src/main/resources/assets/minevm/lang/zh_cn.json`
- Modify: `src/main/java/com/example/vm/test/LocalQemuBackendSelfTest.java`

**Interfaces:**
- Consumes `ModItems.BLANK_FLOPPY` and `LocalQemuBackend.blankFloppyPath`.
- Produces a packaged blank floppy item and regression coverage for its image path.

- [ ] **Step 1: Add resource and command-path regression expectations**

```json
{ "parent": "minecraft:item/generated", "textures": { "layer0": "minevm:item/windows_95_disc" } }
```

Add `item.minevm.blank_floppy` with English `Blank Writable Floppy` and Chinese `空白可写软盘`.

- [ ] **Step 2: Verify build and JAR contents**

Run: `./gradlew clean build`

Expected: `BUILD SUCCESSFUL`; the JAR contains `blank_floppy.json` and the new classes.

- [ ] **Step 3: Perform manual Win95 test**

1. Insert the Win95 boot floppy and start the installer.
2. When Setup requests a startup disk, Shift+right-click the drive to eject the boot floppy.
3. Insert the blank floppy item and let Setup format/write it.
4. When Setup requests removal, Shift+right-click eject it and continue.

- [ ] **Step 4: Commit**

```text
git add src/main/java src/main/resources docs/superpowers/plans
git commit -m "feat: add writable floppy media"
```
