package com.example.vm.qmp;

import com.google.gson.JsonObject;

public final class QmpCommand {
    public static final String QMP_CAPABILITIES = "qmp_capabilities";
    public static final String SYSTEM_POWERDOWN = "system_powerdown";
    public static final String SYSTEM_RESET = "system_reset";
    public static final String EJECT = "eject";

    private QmpCommand() {
    }

    public static String capabilities() {
        return execute(QMP_CAPABILITIES);
    }

    public static String execute(String command) {
        if (command == null || command.isBlank()) {
            throw new IllegalArgumentException("command must not be blank");
        }

        JsonObject json = new JsonObject();
        json.addProperty("execute", command);
        return json.toString();
    }

    public static String eject(String deviceId) {
        if (deviceId == null || deviceId.isBlank()) {
            throw new IllegalArgumentException("deviceId must not be blank");
        }

        JsonObject arguments = new JsonObject();
        arguments.addProperty("device", deviceId);
        arguments.addProperty("force", true);

        JsonObject json = new JsonObject();
        json.addProperty("execute", EJECT);
        json.add("arguments", arguments);
        return json.toString();
    }
}
