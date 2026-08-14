package com.example.vm.screen;

import net.minecraft.core.BlockPos;

public record ScreenControlSession(String playerId, BlockPos screenPos, ScreenControlMode mode) {
    public static final String RELEASE_KEY_COMBO = "CTRL+ALT";

    public ScreenControlSession {
        if (playerId == null || playerId.isBlank()) {
            throw new IllegalArgumentException("playerId must not be blank");
        }
        if (screenPos == null) {
            throw new IllegalArgumentException("screenPos must not be null");
        }
        if (mode == null) {
            mode = ScreenControlMode.GUI_FALLBACK;
        }
    }

    public boolean capturesInput() {
        return mode == ScreenControlMode.IMMERSIVE_CAPTURE;
    }
}
