package com.example.vm.qmp;

import com.google.gson.JsonObject;

import java.nio.file.Path;

public final class QmpCommand {
    public static final String QMP_CAPABILITIES = "qmp_capabilities";
    public static final String SYSTEM_POWERDOWN = "system_powerdown";
    public static final String SYSTEM_RESET = "system_reset";
    public static final String QUERY_STATUS = "query-status";
    public static final String EJECT = "eject";
    public static final String BLOCKDEV_CHANGE_MEDIUM = "blockdev-change-medium";

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
        // These names come from -drive id=; QMP's alternative "id" expects a qdev id.
        arguments.addProperty("device", deviceId);
        arguments.addProperty("force", true);

        JsonObject json = new JsonObject();
        json.addProperty("execute", EJECT);
        json.add("arguments", arguments);
        return json.toString();
    }

    public static String changeMedium(String deviceId, Path imagePath, boolean readOnly) {
        if (deviceId == null || deviceId.isBlank()) {
            throw new IllegalArgumentException("deviceId must not be blank");
        }
        if (imagePath == null) {
            throw new IllegalArgumentException("imagePath must not be null");
        }

        JsonObject arguments = new JsonObject();
        arguments.addProperty("device", deviceId);
        arguments.addProperty("filename", imagePath.toAbsolutePath().normalize().toString());
        arguments.addProperty("format", "raw");
        arguments.addProperty("read-only-mode", readOnly ? "read-only" : "read-write");

        JsonObject json = new JsonObject();
        json.addProperty("execute", BLOCKDEV_CHANGE_MEDIUM);
        json.add("arguments", arguments);
        return json.toString();
    }

    public static String changeMediumLegacy(String deviceId, Path imagePath, boolean readOnly) {
        if (deviceId == null || deviceId.isBlank()) {
            throw new IllegalArgumentException("deviceId must not be blank");
        }
        if (imagePath == null) {
            throw new IllegalArgumentException("imagePath must not be null");
        }

        JsonObject arguments = new JsonObject();
        arguments.addProperty("device", deviceId);
        arguments.addProperty("filename", imagePath.toAbsolutePath().normalize().toString());
        arguments.addProperty("format", "raw");
        arguments.addProperty("read-only", readOnly);

        JsonObject json = new JsonObject();
        json.addProperty("execute", BLOCKDEV_CHANGE_MEDIUM);
        json.add("arguments", arguments);
        return json.toString();
    }
}
