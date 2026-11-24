package com.example.vm.vm;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

public final class DiskMaintenance {
    public enum Operation { BACKUP, RESTORE, RESET }

    public static Path backup(Path disk) throws IOException {
        if (!Files.isRegularFile(disk)) throw new IOException("VM disk does not exist: " + disk);
        Path target = disk.resolveSibling(disk.getFileName() + ".backup");
        Path temporary = disk.resolveSibling(disk.getFileName() + ".backup-" + UUID.randomUUID() + ".tmp");
        try {
            Files.copy(disk, temporary);
            try { moveAfterHandleRelease(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (java.nio.file.AtomicMoveNotSupportedException unsupported) {
                moveAfterHandleRelease(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally { Files.deleteIfExists(temporary); }
        return target;
    }

    public static Path archive(Path disk) throws IOException {
        Path archive = disk.resolveSibling(disk.getFileName() + ".retained-" + UUID.randomUUID());
        if (Files.exists(disk)) moveAfterHandleRelease(disk, archive);
        return archive;
    }

    public static Path restore(Path disk) throws IOException {
        Path backup = disk.resolveSibling(disk.getFileName() + ".backup");
        if (!Files.isRegularFile(backup)) throw new IOException("No backup exists for this VM");
        Path temporary = disk.resolveSibling(disk.getFileName() + ".restore-" + UUID.randomUUID() + ".tmp");
        try {
            Files.copy(backup, temporary);
            Path retained = archive(disk);
            try { moveAfterHandleRelease(temporary, disk); }
            catch (IOException failure) {
                if (Files.exists(retained) && !Files.exists(disk)) moveAfterHandleRelease(retained, disk);
                throw failure;
            }
            return retained;
        } finally { Files.deleteIfExists(temporary); }
    }

    private static void moveAfterHandleRelease(Path source, Path target, java.nio.file.CopyOption... options) throws IOException {
        for (int attempt = 0; ; attempt++) {
            try {
                Files.move(source, target, options);
                return;
            } catch (java.nio.file.AtomicMoveNotSupportedException unsupported) {
                throw unsupported;
            } catch (java.nio.file.FileSystemException failure) {
                // Windows can briefly retain a handle after QEMU reports process exit.
                if (attempt >= 20) throw failure;
                try { Thread.sleep(50); }
                catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new java.io.InterruptedIOException("Interrupted waiting for the stopped VM disk handle");
                }
            }
        }
    }

    private DiskMaintenance() {}
}
