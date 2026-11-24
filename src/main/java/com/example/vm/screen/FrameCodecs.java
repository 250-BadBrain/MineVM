package com.example.vm.screen;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import java.util.zip.DataFormatException;

public final class FrameCodecs {
    public static final int MAX_WIDTH = 512;
    public static final int MAX_HEIGHT = 320;
    public static final int MAX_PAYLOAD = MAX_WIDTH * MAX_HEIGHT * 4 + 1024;
    public static final FrameCodec RAW = new Codec(false);
    public static final FrameCodec DEFLATE = new Codec(true);

    public static FrameCodec get(String id) throws IOException {
        return switch (id) { case "raw" -> RAW; case "deflate" -> DEFLATE; default -> throw new IOException("Unsupported frame codec"); };
    }

    public static ScreenFrame scale(ScreenFrame full) {
        if (full.changedX() != 0 || full.changedY() != 0 || full.changedWidth() != full.width() || full.changedHeight() != full.height())
            throw new IllegalArgumentException("Network scaling requires a full snapshot");
        double ratio = Math.min(1, Math.min((double) MAX_WIDTH / full.width(), (double) MAX_HEIGHT / full.height()));
        int w = Math.max(1, (int) (full.width() * ratio));
        int h = Math.max(1, (int) (full.height() * ratio));
        int[] source = full.argbPixels();
        int[] pixels = new int[w * h];
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++)
            pixels[y * w + x] = source[(int) ((long) y * full.height() / h) * full.width() + (int) ((long) x * full.width() / w)];
        return new ScreenFrame(w, h, 0, 0, w, h, pixels);
    }

    private static int byteCount(int w, int h) throws IOException {
        if (w < 1 || h < 1 || w > MAX_WIDTH || h > MAX_HEIGHT) throw new IOException("Invalid network framebuffer dimensions");
        return w * h * 4;
    }

    private record Codec(boolean compressed) implements FrameCodec {
        @Override public String id() { return compressed ? "deflate" : "raw"; }
        @Override public byte[] encode(ScreenFrame frame) throws IOException {
            ByteBuffer raw = ByteBuffer.allocate(byteCount(frame.width(), frame.height()));
            if (frame.argbPixels().length != frame.width() * frame.height()) throw new IOException("Codec requires a full frame");
            for (int pixel : frame.argbPixels()) raw.putInt(pixel);
            if (!compressed) return raw.array();
            Deflater deflater = new Deflater(Deflater.BEST_SPEED);
            try {
                deflater.setInput(raw.array()); deflater.finish();
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                byte[] chunk = new byte[8192];
                while (!deflater.finished()) output.write(chunk, 0, deflater.deflate(chunk));
                return output.toByteArray();
            } finally { deflater.end(); }
        }
        @Override public ScreenFrame decode(int w, int h, byte[] bytes) throws IOException {
            int count = byteCount(w, h);
            if (bytes.length > MAX_PAYLOAD) throw new IOException("Oversized network framebuffer");
            byte[] raw = bytes;
            if (compressed) {
                Inflater inflater = new Inflater();
                try {
                    inflater.setInput(bytes);
                    raw = new byte[count];
                    int filled = 0;
                    while (filled < count && !inflater.finished()) {
                        int read = inflater.inflate(raw, filled, count - filled);
                        if (read == 0) break;
                        filled += read;
                    }
                    if (filled != count || !inflater.finished() || inflater.getRemaining() != 0) throw new IOException("Invalid compressed frame length");
                } catch (DataFormatException failure) { throw new IOException("Invalid compressed framebuffer", failure); }
                finally { inflater.end(); }
            }
            if (raw.length != count) throw new IOException("Invalid raw frame length");
            int[] pixels = new int[w * h];
            ByteBuffer input = ByteBuffer.wrap(raw);
            for (int index = 0; index < pixels.length; index++) pixels[index] = input.getInt();
            return new ScreenFrame(w, h, 0, 0, w, h, pixels);
        }
    }
    private FrameCodecs() {}
}
