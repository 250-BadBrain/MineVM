package com.example.vm.test;

import com.example.vm.qmp.QmpCommand;
import com.example.vm.qmp.QmpEndpoint;

import java.nio.file.Path;

public final class QmpCommandSelfTest {
    private QmpCommandSelfTest() {
    }

    public static void main(String[] args) {
        assertEquals(QmpCommand.capabilities(), "{\"execute\":\"qmp_capabilities\"}");
        assertEquals(QmpCommand.execute(QmpCommand.SYSTEM_POWERDOWN), "{\"execute\":\"system_powerdown\"}");
        assertEquals(QmpCommand.execute(QmpCommand.SYSTEM_RESET), "{\"execute\":\"system_reset\"}");
        assertEquals(QmpCommand.execute(QmpCommand.QUERY_STATUS), "{\"execute\":\"query-status\"}");
        assertEquals(QmpCommand.eject("system_disc"), "{\"execute\":\"eject\",\"arguments\":{\"device\":\"system_disc\",\"force\":true}}");
        assertEquals(QmpCommand.eject("boot_floppy"), "{\"execute\":\"eject\",\"arguments\":{\"device\":\"boot_floppy\",\"force\":true}}");
        assertEquals(
                QmpCommand.changeMedium("boot_floppy", Path.of("E:\\MinecraftVMs\\computer-test\\floppies\\disk.img"), false),
                "{\"execute\":\"blockdev-change-medium\",\"arguments\":{\"device\":\"boot_floppy\",\"filename\":\"E:\\\\MinecraftVMs\\\\computer-test\\\\floppies\\\\disk.img\",\"format\":\"raw\",\"read-only-mode\":\"read-write\"}}"
        );
        assertEquals(
                QmpCommand.changeMediumLegacy("boot_floppy", Path.of("E:\\MinecraftVMs\\computer-test\\floppies\\disk.img"), true),
                "{\"execute\":\"blockdev-change-medium\",\"arguments\":{\"device\":\"boot_floppy\",\"filename\":\"E:\\\\MinecraftVMs\\\\computer-test\\\\floppies\\\\disk.img\",\"format\":\"raw\",\"read-only\":true}}"
        );

        QmpEndpoint endpoint = QmpEndpoint.forComputer("computer-test");
        assertEquals(endpoint.host(), "127.0.0.1");
        if (endpoint.port() < 43000 || endpoint.port() > 62999) {
            throw new AssertionError("Unexpected QMP port: " + endpoint.port());
        }
        assertEquals(endpoint, QmpEndpoint.forComputer("computer-test"));
        assertEquals(endpoint.toQemuArgument(), "tcp:127.0.0.1:" + endpoint.port() + ",server,nowait");
    }

    private static void assertEquals(Object actual, Object expected) {
        if (!actual.equals(expected)) {
            throw new AssertionError("Expected " + expected + " but got " + actual);
        }
    }
}
