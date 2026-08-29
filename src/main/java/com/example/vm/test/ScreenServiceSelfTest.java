package com.example.vm.test;

import com.example.vm.screen.ScreenFrame;
import com.example.vm.screen.ScreenService;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public final class ScreenServiceSelfTest {
    private ScreenServiceSelfTest() {
    }

    public static void main(String[] args) throws Exception {
        try (ScreenService service = new ScreenService()) {
            ScreenFrame frame = new ScreenFrame(1024, 768, 10, 20, 2, 2, new int[] {
                    0xFF000000,
                    0xFFFFFFFF,
                    0xFFFF0000,
                    0xFF00FF00
            });

            service.updateFrame("computer-test", frame).get(10, TimeUnit.SECONDS);
            Optional<ScreenFrame> latestFrame = service.getLatestFrame("computer-test").get(10, TimeUnit.SECONDS);
            if (latestFrame.isEmpty() || latestFrame.get().width() != 1024 || latestFrame.get().changedWidth() != 1024) {
                throw new AssertionError("Unexpected latest frame: " + latestFrame);
            }
            Optional<ScreenFrame> snapshot = service.latestFrameSnapshot("computer-test");
            if (snapshot.isEmpty() || snapshot.get().changedHeight() != 768) {
                throw new AssertionError("ScreenService must expose a non-blocking render snapshot");
            }

            int[] copiedPixels = latestFrame.get().argbPixels();
            copiedPixels[0] = 0;
            if (latestFrame.get().argbPixels()[0] != 0xFF000000) {
                throw new AssertionError("ScreenFrame must protect its pixel buffer");
            }

            ScreenFrame secondFrame = new ScreenFrame(1024, 768, 12, 20, 2, 2, new int[] {
                    0xFF0000FF,
                    0xFF00FFFF,
                    0xFFFFFF00,
                    0xFFFF00FF
            });
            service.updateFrame("computer-test", secondFrame).get(10, TimeUnit.SECONDS);
            Optional<ScreenFrame> mergedSnapshot = service.latestFrameSnapshot("computer-test");
            if (mergedSnapshot.isEmpty() || mergedSnapshot.get().changedX() != 12 || mergedSnapshot.get().changedWidth() != 2) {
                throw new AssertionError("ScreenService must expose the latest dirty rectangle after render consumption: " + mergedSnapshot);
            }
            service.updateFrame("computer-test", frame).get(10, TimeUnit.SECONDS);
            service.updateFrame("computer-test", secondFrame).get(10, TimeUnit.SECONDS);
            Optional<ScreenFrame> coalescedSnapshot = service.latestFrameSnapshot("computer-test");
            if (coalescedSnapshot.isEmpty() || coalescedSnapshot.get().changedX() != 10 || coalescedSnapshot.get().changedWidth() != 4) {
                throw new AssertionError("ScreenService must coalesce dirty rectangles between render snapshots: " + coalescedSnapshot);
            }

            ScreenFrame tinyFrame = new ScreenFrame(10, 10, 1, 1, 1, 1, new int[] {0xFF123456});
            service.updateFrame("computer-keyframe", tinyFrame).get(10, TimeUnit.SECONDS);
            service.latestFrameSnapshot("computer-keyframe");
            for (int index = 0; index < 120; index++) {
                service.updateFrame("computer-keyframe", tinyFrame).get(10, TimeUnit.SECONDS);
            }
            Optional<ScreenFrame> keyframeSnapshot = service.latestFrameSnapshot("computer-keyframe");
            if (keyframeSnapshot.isEmpty() || keyframeSnapshot.get().changedWidth() != 10 || keyframeSnapshot.get().changedHeight() != 10) {
                throw new AssertionError("ScreenService must emit periodic full-frame keyframes: " + keyframeSnapshot);
            }

            ScreenFrame fullSeedFrame = new ScreenFrame(10, 10, 0, 0, 1, 1, new int[] {0xFF000000});
            service.updateFrame("computer-large-dirty", fullSeedFrame).get(10, TimeUnit.SECONDS);
            service.latestFrameSnapshot("computer-large-dirty");
            ScreenFrame largeDirtyFrame = new ScreenFrame(10, 10, 0, 0, 5, 8, new int[40]);
            service.updateFrame("computer-large-dirty", largeDirtyFrame).get(10, TimeUnit.SECONDS);
            Optional<ScreenFrame> largeDirtySnapshot = service.latestFrameSnapshot("computer-large-dirty");
            if (largeDirtySnapshot.isEmpty() || largeDirtySnapshot.get().changedWidth() != 10 || largeDirtySnapshot.get().changedHeight() != 10) {
                throw new AssertionError("ScreenService must promote large dirty regions to full-frame updates: " + largeDirtySnapshot);
            }

            service.close();
            service.updateFrame("computer-test", frame).get(10, TimeUnit.SECONDS);
            if (service.getLatestFrame("computer-test").get(10, TimeUnit.SECONDS).isEmpty()) {
                throw new AssertionError("ScreenService must recover after close");
            }
        }
    }
}
