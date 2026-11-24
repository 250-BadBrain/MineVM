package com.example.vm.floppy;

import java.util.UUID;

public record FloppyMedia(Kind kind, String mediaId) {
    public enum Kind {
        WINDOWS_95_BOOT,
        BLANK
    }

    public FloppyMedia {
        if (kind == null) {
            throw new IllegalArgumentException("kind must not be null");
        }
        mediaId = mediaId == null ? "" : mediaId;
        if (kind == Kind.BLANK) {
            UUID.fromString(mediaId);
        } else if (!mediaId.isEmpty()) {
            throw new IllegalArgumentException("Boot floppy media must not have an id");
        }
    }

    public static FloppyMedia windows95Boot() {
        return new FloppyMedia(Kind.WINDOWS_95_BOOT, "");
    }

    public static FloppyMedia blank(String mediaId) {
        return new FloppyMedia(Kind.BLANK, mediaId);
    }

    public boolean isWindows95Boot() {
        return kind == Kind.WINDOWS_95_BOOT;
    }

    public boolean isWritable() {
        return kind == Kind.BLANK;
    }
}
