package com.example.vm.qmp;

import com.example.vm.input.KeyboardInput;
import com.example.vm.input.MouseButtonInput;
import com.example.vm.input.MouseMoveInput;
import com.example.vm.input.MouseWheelInput;
import com.example.vm.input.VirtualMachineInput;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public final class QmpInputCommand {
    static final int QEMU_ABSOLUTE_POINTER_MAX = 0x7FFF;

    private QmpInputCommand() {
    }

    public static String toJson(VirtualMachineInput input) {
        if (input instanceof KeyboardInput keyboardInput) {
            return keyboardToJson(keyboardInput);
        }
        if (input instanceof MouseButtonInput mouseButtonInput) {
            return mouseButtonToJson(mouseButtonInput);
        }
        if (input instanceof MouseMoveInput mouseMoveInput) {
            return mouseMoveToJson(mouseMoveInput);
        }
        if (input instanceof MouseWheelInput mouseWheelInput) {
            return mouseWheelToJson(mouseWheelInput);
        }

        throw new IllegalArgumentException("Unsupported input event: " + input);
    }

    private static String keyboardToJson(KeyboardInput input) {
        if (input.pressed()) {
            return sendKeyCommand(input.key());
        }

        JsonObject eventData = new JsonObject();
        eventData.addProperty("down", input.pressed());

        JsonObject key = new JsonObject();
        key.addProperty("type", "qcode");
        key.addProperty("data", input.key());
        eventData.add("key", key);

        return inputSendEvent("key", eventData);
    }

    private static String sendKeyCommand(String key) {
        JsonObject command = new JsonObject();
        command.addProperty("execute", "human-monitor-command");

        JsonObject arguments = new JsonObject();
        arguments.addProperty("command-line", "sendkey " + key);
        command.add("arguments", arguments);
        return command.toString();
    }

    private static String mouseButtonToJson(MouseButtonInput input) {
        JsonObject eventData = new JsonObject();
        eventData.addProperty("down", input.pressed());
        eventData.addProperty("button", qmpButton(input.button()));

        return inputSendEvent("btn", eventData);
    }

    private static String mouseMoveToJson(MouseMoveInput input) {
        JsonObject eventData = new JsonObject();
        eventData.addProperty("axis", "x");
        eventData.addProperty("value", toAbsolutePointerCoordinate(input.x(), input.screenWidth()));

        JsonObject yEventData = new JsonObject();
        yEventData.addProperty("axis", "y");
        yEventData.addProperty("value", toAbsolutePointerCoordinate(input.y(), input.screenHeight()));

        JsonObject command = baseInputSendEvent();
        JsonArray events = command.getAsJsonObject("arguments").getAsJsonArray("events");
        events.add(inputEvent("abs", eventData));
        events.add(inputEvent("abs", yEventData));
        return command.toString();
    }

    private static String mouseWheelToJson(MouseWheelInput input) {
        String button = input.steps() > 0 ? "wheel-up" : "wheel-down";
        int count = Math.abs(input.steps());
        JsonObject command = baseInputSendEvent();
        JsonArray events = command.getAsJsonObject("arguments").getAsJsonArray("events");
        for (int index = 0; index < count; index++) {
            events.add(inputEvent("btn", wheelEventData(button, true)));
            events.add(inputEvent("btn", wheelEventData(button, false)));
        }
        return command.toString();
    }

    public static int toAbsolutePointerCoordinate(int coordinate, int size) {
        if (size <= 1) {
            return 0;
        }
        int clamped = Math.max(0, Math.min(size - 1, coordinate));
        return Math.round((float) clamped * QEMU_ABSOLUTE_POINTER_MAX / (size - 1));
    }

    private static String inputSendEvent(String type, JsonObject data) {
        JsonObject command = baseInputSendEvent();
        command.getAsJsonObject("arguments").getAsJsonArray("events").add(inputEvent(type, data));
        return command.toString();
    }

    private static JsonObject baseInputSendEvent() {
        JsonObject command = new JsonObject();
        command.addProperty("execute", "input-send-event");

        JsonObject arguments = new JsonObject();
        arguments.add("events", new JsonArray());
        command.add("arguments", arguments);
        return command;
    }

    private static JsonObject inputEvent(String type, JsonObject data) {
        JsonObject event = new JsonObject();
        event.addProperty("type", type);
        event.add("data", data);
        return event;
    }

    private static JsonObject wheelEventData(String button, boolean down) {
        JsonObject eventData = new JsonObject();
        eventData.addProperty("down", down);
        eventData.addProperty("button", button);
        return eventData;
    }

    private static String qmpButton(int button) {
        return switch (button) {
            case 0 -> "left";
            case 1 -> "middle";
            case 2 -> "right";
            default -> "side";
        };
    }
}
