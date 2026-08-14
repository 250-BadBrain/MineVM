package com.example.vm.screen;

import java.util.Arrays;
import java.util.Optional;

final class ScreenFrameBuffer {
    private int width;
    private int height;
    private int[] pixels = new int[0];
    private int dirtyX;
    private int dirtyY;
    private int dirtyMaxX;
    private int dirtyMaxY;
    private boolean dirty;
    private ScreenFrame lastSnapshot;

    synchronized void apply(ScreenFrame frame) {
        if (frame.width() != width || frame.height() != height || pixels.length != frame.width() * frame.height()) {
            width = frame.width();
            height = frame.height();
            pixels = new int[width * height];
            Arrays.fill(pixels, 0xFF000000);
            dirtyX = 0;
            dirtyY = 0;
            dirtyMaxX = width;
            dirtyMaxY = height;
            dirty = true;
        }

        int[] changedPixels = frame.argbPixels();
        for (int y = 0; y < frame.changedHeight(); y++) {
            int sourceOffset = y * frame.changedWidth();
            int targetOffset = (frame.changedY() + y) * width + frame.changedX();
            System.arraycopy(changedPixels, sourceOffset, pixels, targetOffset, frame.changedWidth());
        }

        includeDirtyRectangle(frame.changedX(), frame.changedY(), frame.changedWidth(), frame.changedHeight());
        lastSnapshot = snapshotDirtyRectangle();
    }

    synchronized Optional<ScreenFrame> snapshot() {
        if (dirty) {
            lastSnapshot = snapshotDirtyRectangle();
            dirty = false;
        }
        return Optional.ofNullable(lastSnapshot);
    }

    private void includeDirtyRectangle(int x, int y, int rectangleWidth, int rectangleHeight) {
        if (!dirty) {
            dirtyX = x;
            dirtyY = y;
            dirtyMaxX = x + rectangleWidth;
            dirtyMaxY = y + rectangleHeight;
            dirty = true;
            return;
        }
        dirtyX = Math.min(dirtyX, x);
        dirtyY = Math.min(dirtyY, y);
        dirtyMaxX = Math.max(dirtyMaxX, x + rectangleWidth);
        dirtyMaxY = Math.max(dirtyMaxY, y + rectangleHeight);
    }

    private ScreenFrame snapshotDirtyRectangle() {
        int snapshotWidth = dirtyMaxX - dirtyX;
        int snapshotHeight = dirtyMaxY - dirtyY;
        int[] snapshotPixels = new int[snapshotWidth * snapshotHeight];
        for (int y = 0; y < snapshotHeight; y++) {
            int sourceOffset = (dirtyY + y) * width + dirtyX;
            int targetOffset = y * snapshotWidth;
            System.arraycopy(pixels, sourceOffset, snapshotPixels, targetOffset, snapshotWidth);
        }
        return new ScreenFrame(width, height, dirtyX, dirtyY, snapshotWidth, snapshotHeight, snapshotPixels);
    }
}
