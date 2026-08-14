package com.example.vm.screen;

import java.util.Arrays;

public record ScreenFrame(
        int width,
        int height,
        int changedX,
        int changedY,
        int changedWidth,
        int changedHeight,
        int[] argbPixels
) {
    public ScreenFrame {
        if (width < 1 || height < 1) {
            throw new IllegalArgumentException("frame dimensions must be positive");
        }
        if (changedX < 0 || changedY < 0 || changedWidth < 1 || changedHeight < 1) {
            throw new IllegalArgumentException("changed rectangle must be positive and inside the frame");
        }
        if (changedX + changedWidth > width || changedY + changedHeight > height) {
            throw new IllegalArgumentException("changed rectangle must be inside the frame");
        }
        if (argbPixels == null || argbPixels.length != changedWidth * changedHeight) {
            throw new IllegalArgumentException("argbPixels must match changed rectangle area");
        }

        argbPixels = Arrays.copyOf(argbPixels, argbPixels.length);
    }

    @Override
    public int[] argbPixels() {
        return Arrays.copyOf(argbPixels, argbPixels.length);
    }
}
