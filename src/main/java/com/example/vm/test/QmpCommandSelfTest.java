package com.example.vm.test;

import com.example.vm.qmp.QmpCommand;
import com.example.vm.qmp.QmpEndpoint;

public final class QmpCommandSelfTest {
    private QmpCommandSelfTest() {
    }

    public static void main(String[] args) {
        assertEquals(QmpCommand.capabilities(), "{\"execute\":\"qmp_capabilities\"}");
        assertEquals(QmpCommand.execute(QmpCommand.SYSTEM_POWERDOWN), "{\"execute\":\"system_powerdown\"}");
        assertEquals(QmpCommand.execute(QmpCommand.SYSTEM_RESET), "{\"execute\":\"system_reset\"}");
        assertEquals(QmpCommand.execute(QmpCommand.QUERY_STATUS), "{\"execute\":\"query-status\"}");
        assertEquals(QmpCommand.eject("system_disc"), "{\"execute\":\"eject\",\"arguments\":{\"device\":\"system_disc\",\"force\":true}}");

        QmpEndpoint endpoint = QmpEndpoint.forComputer("computer-test");
        assertEquals(endpoint.host(), "127.0.0.1");
        if (endpoint.port() < 42000 || endpoint.port() > 42999) {
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
