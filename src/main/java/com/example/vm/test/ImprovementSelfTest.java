package com.example.vm.test;

import com.example.vm.network.ControlLeases;
import com.example.vm.network.StreamBudget;
import com.example.vm.screen.FrameCodecs;
import com.example.vm.screen.ScreenFrame;
import com.example.vm.vm.DiskMaintenance;
import com.example.vm.vm.GuestCompatibility;
import com.example.vm.vm.GuestOperatingSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;
import java.util.UUID;

public final class ImprovementSelfTest {
    public static void main(String[] args) throws Exception {
        verifyCodecs();
        verifyLeasesAndBudget();
        verifyDiskRecovery();
        for (var guest : GuestOperatingSystem.values()) {
            var entry = GuestCompatibility.forGuest(guest);
            if (entry.requiredChecks().size() != 7) throw new AssertionError("Incomplete compatibility checklist");
        }
        for (var guest : new GuestOperatingSystem[] {GuestOperatingSystem.WINDOWS_95, GuestOperatingSystem.WINDOWS_98, GuestOperatingSystem.WINDOWS_VISTA})
            if (GuestCompatibility.forGuest(guest).evidence() != GuestCompatibility.Evidence.INSTALLATION_PENDING)
                throw new AssertionError("A hardware-profile test must not invent a completed installation");
    }

    private static void verifyCodecs() throws Exception {
        int[] pixels = new Random(17).ints(640 * 480).toArray();
        ScreenFrame frame = FrameCodecs.scale(new ScreenFrame(640, 480, 0, 0, 640, 480, pixels));
        if (frame.width() > 512 || frame.height() > 320) throw new AssertionError("Network resize is not bounded");
        for (var codec : new com.example.vm.screen.FrameCodec[] {FrameCodecs.RAW, FrameCodecs.DEFLATE}) {
            byte[] bytes = codec.encode(frame);
            if (bytes.length > FrameCodecs.MAX_PAYLOAD) throw new AssertionError("Frame exceeds payload limit");
            ScreenFrame decoded = codec.decode(frame.width(), frame.height(), bytes);
            if (!Arrays.equals(frame.argbPixels(), decoded.argbPixels())) throw new AssertionError("Codec changed pixels");
            try { codec.decode(65535, 65535, bytes); throw new AssertionError("Oversized dimensions were accepted"); }
            catch (java.io.IOException expected) { }
            try { codec.decode(frame.width(), frame.height(), Arrays.copyOf(bytes, bytes.length - 1)); throw new AssertionError("Truncation accepted"); }
            catch (java.io.IOException expected) { }
            try { codec.decode(frame.width(), frame.height(), Arrays.copyOf(bytes, bytes.length + 1)); throw new AssertionError("Trailing bytes accepted"); }
            catch (java.io.IOException expected) { }
        }
        int[] solid = new int[512 * 320];
        Arrays.fill(solid, 0xFF123456);
        byte[] compressed = FrameCodecs.DEFLATE.encode(new ScreenFrame(512, 320, 0, 0, 512, 320, solid));
        if (compressed.length >= solid.length) throw new AssertionError("Solid frames should compress");
        try { FrameCodecs.DEFLATE.decode(1, 1, compressed); throw new AssertionError("Decompression overflow accepted"); }
        catch (java.io.IOException expected) { }
    }

    private static void verifyLeasesAndBudget() {
        UUID first = UUID.randomUUID(); UUID second = UUID.randomUUID();
        ControlLeases leases = new ControlLeases(1000);
        if (!leases.acquire("vm", first, 100) || leases.acquire("vm", second, 200)) throw new AssertionError("Control is not exclusive");
        leases.release("vm", second);
        if (!leases.owns("vm", first, 500)) throw new AssertionError("Another player released the lease");
        if (!leases.acquire("vm", first, 500) || leases.owns("vm", first, 1500)) throw new AssertionError("Lease renewal or expiry is wrong");
        if (!leases.acquire("vm", second, 1500)) throw new AssertionError("Expired control was not available");
        leases.clear();
        if (leases.owns("vm", second, 1500)) throw new AssertionError("Shutdown kept a lease");
        StreamBudget budget = new StreamBudget(1000, 100);
        if (!budget.consume(2000, 100) || budget.consume(1, 100) || !budget.consume(1000, 1100) || budget.consume(1, 1100))
            throw new AssertionError("Bandwidth budget is not enforced");
        if (budget.consume(500, 1000)) throw new AssertionError("Clock rollback refilled budget");
    }

    private static void verifyDiskRecovery() throws Exception {
        Path directory = Files.createTempDirectory("minevm-maintenance-test-");
        Path disk = directory.resolve("disk.qcow2");
        Path archive = null;
        try {
            Files.writeString(disk, "installed guest");
            DiskMaintenance.backup(disk);
            Files.writeString(disk, "new guest data");
            archive = DiskMaintenance.restore(disk);
            if (!Files.readString(disk).equals("installed guest") || !Files.readString(archive).equals("new guest data"))
                throw new AssertionError("Restore lost current or backup content");
            Files.delete(archive);
            archive = DiskMaintenance.archive(disk);
            if (Files.exists(disk) || !Files.readString(archive).equals("installed guest")) throw new AssertionError("Reset archive lost disk");
            Files.delete(archive);
            archive = DiskMaintenance.restore(disk);
            if (!Files.readString(disk).equals("installed guest")) throw new AssertionError("Cannot restore a missing disk");
        } finally {
            if (archive != null) Files.deleteIfExists(archive);
            Files.deleteIfExists(disk);
            Files.deleteIfExists(directory.resolve("disk.qcow2.backup"));
            Files.delete(directory);
        }
    }
}
