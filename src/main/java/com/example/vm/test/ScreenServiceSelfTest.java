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
            if (latestFrame.isEmpty() || latestFrame.get().width() != 1024 || latestFrame.get().changedWidth() != 2) {
                throw new AssertionError("Unexpected latest frame: " + latestFrame);
            }
            Optional<ScreenFrame> snapshot = service.latestFrameSnapshot("computer-test");
            if (snapshot.isEmpty() || snapshot.get().changedHeight() != 2) {
                throw new AssertionError("ScreenService must expose a non-blocking render snapshot");
            }

            int[] copiedPixels = latestFrame.get().argbPixels();
            copiedPixels[0] = 0;
            if (latestFrame.get().argbPixels()[0] != 0xFF000000) {
                throw new AssertionError("ScreenFrame must protect its pixel buffer");
            }

            service.close();
            service.updateFrame("computer-test", frame).get(10, TimeUnit.SECONDS);
            if (service.getLatestFrame("computer-test").get(10, TimeUnit.SECONDS).isEmpty()) {
                throw new AssertionError("ScreenService must recover after close");
            }
        }
    }
}
