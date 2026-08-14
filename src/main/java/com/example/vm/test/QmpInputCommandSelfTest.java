package com.example.vm.test;

import com.example.vm.input.KeyboardInput;
import com.example.vm.input.MouseButtonInput;
import com.example.vm.input.MouseMoveInput;
import com.example.vm.qmp.QmpInputCommand;

public final class QmpInputCommandSelfTest {
    private QmpInputCommandSelfTest() {
    }

    public static void main(String[] args) {
        assertEquals(
                QmpInputCommand.toJson(new KeyboardInput("computer-test", "a", true)),
                "{\"execute\":\"human-monitor-command\",\"arguments\":{\"command-line\":\"sendkey a\"}}"
        );
        assertEquals(
                QmpInputCommand.toJson(new KeyboardInput("computer-test", "a", false)),
                "{\"execute\":\"input-send-event\",\"arguments\":{\"events\":[{\"type\":\"key\",\"data\":{\"down\":false,\"key\":{\"type\":\"qcode\",\"data\":\"a\"}}}]}}"
        );
        assertEquals(
                QmpInputCommand.toJson(new MouseButtonInput("computer-test", 0, false)),
                "{\"execute\":\"input-send-event\",\"arguments\":{\"events\":[{\"type\":\"btn\",\"data\":{\"down\":false,\"button\":\"left\"}}]}}"
        );
        assertEquals(
                QmpInputCommand.toJson(new MouseMoveInput("computer-test", 100, 200, 1024, 768)),
                "{\"execute\":\"input-send-event\",\"arguments\":{\"events\":[{\"type\":\"abs\",\"data\":{\"axis\":\"x\",\"value\":100}},{\"type\":\"abs\",\"data\":{\"axis\":\"y\",\"value\":200}}]}}"
        );
    }

    private static void assertEquals(String actual, String expected) {
        if (!actual.equals(expected)) {
            throw new AssertionError("Expected " + expected + " but got " + actual);
        }
    }
}
