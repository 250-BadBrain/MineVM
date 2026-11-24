package com.example.vm.screen;

import java.io.IOException;

public interface FrameCodec {
    String id();
    byte[] encode(ScreenFrame frame) throws IOException;
    ScreenFrame decode(int width, int height, byte[] bytes) throws IOException;
}
