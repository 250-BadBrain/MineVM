package com.example.vm.blockentity;

import com.example.vm.floppy.FloppyMedia;
import com.example.vm.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class FloppyDriveBlockEntity extends BlockEntity {
    private static final String TAG_INSERTED_WIN95_BOOT_FLOPPY = "InsertedWin95BootFloppy";
    private static final String TAG_MEDIA_KIND = "MediaKind";
    private static final String TAG_MEDIA_ID = "MediaId";
    private static final String TAG_LINKED_COMPUTER_ID = "LinkedComputerId";
    private static final String TAG_LINKED_COMPUTER_POS = "LinkedComputerPos";

    private FloppyMedia media;
    private String linkedComputerId;
    private BlockPos linkedComputerPos;
    private boolean mediaChanging;
    private String deviceId = java.util.UUID.randomUUID().toString();

    public FloppyDriveBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.FLOPPY_DRIVE_BLOCK_ENTITY.get(), pos, blockState);
    }

    public boolean hasWin95BootFloppy() {
        return media != null && media.isWindows95Boot();
    }

    public String deviceId() {
        return deviceId;
    }

    public boolean hasMedia() {
        return media != null;
    }

    public Optional<FloppyMedia> media() {
        return Optional.ofNullable(media);
    }

    public boolean insert(FloppyMedia newMedia) {
        if (newMedia == null || media != null) {
            return false;
        }
        media = newMedia;
        markUpdated();
        return true;
    }

    public Optional<FloppyMedia> eject() {
        if (media == null) {
            return Optional.empty();
        }
        FloppyMedia removed = media;
        media = null;
        markUpdated();
        return Optional.of(removed);
    }

    public void linkComputer(String computerId, BlockPos computerPos) {
        linkedComputerId = computerId;
        linkedComputerPos = computerPos;
        markUpdated();
    }

    public Optional<String> linkedComputerId() {
        return Optional.ofNullable(linkedComputerId);
    }

    public Optional<BlockPos> linkedComputerPos() {
        return Optional.ofNullable(linkedComputerPos);
    }

    public Optional<String> connectedComputerId() {
        if (level == null || linkedComputerPos == null || !level.hasChunkAt(linkedComputerPos)) {
            return Optional.empty();
        }
        BlockEntity entity = level.getBlockEntity(linkedComputerPos);
        if (entity instanceof ComputerBlockEntity computer
                && computer.getComputerId().equals(linkedComputerId)
                && computer.linkedFloppyDrivePos().filter(worldPosition::equals).isPresent()) {
            return Optional.of(linkedComputerId);
        }
        return Optional.empty();
    }

    public boolean beginMediaChange() {
        if (mediaChanging) {
            return false;
        }
        mediaChanging = true;
        return true;
    }

    public boolean mediaChanging() {
        return mediaChanging;
    }

    public void endMediaChange() {
        mediaChanging = false;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("DeviceId", deviceId);
        if (media != null) {
            tag.putString(TAG_MEDIA_KIND, media.kind().name());
            if (!media.mediaId().isEmpty()) {
                tag.putString(TAG_MEDIA_ID, media.mediaId());
            }
        }
        if (linkedComputerId != null && !linkedComputerId.isBlank()) {
            tag.putString(TAG_LINKED_COMPUTER_ID, linkedComputerId);
        }
        if (linkedComputerPos != null) {
            tag.putLong(TAG_LINKED_COMPUTER_POS, linkedComputerPos.asLong());
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("DeviceId")) deviceId = tag.getString("DeviceId");
        media = readMedia(tag);
        linkedComputerId = tag.contains(TAG_LINKED_COMPUTER_ID) ? tag.getString(TAG_LINKED_COMPUTER_ID) : null;
        linkedComputerPos = tag.contains(TAG_LINKED_COMPUTER_POS) ? BlockPos.of(tag.getLong(TAG_LINKED_COMPUTER_POS)) : null;
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void markUpdated() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private static FloppyMedia readMedia(CompoundTag tag) {
        if (tag.contains(TAG_MEDIA_KIND)) {
            try {
                FloppyMedia.Kind kind = FloppyMedia.Kind.valueOf(tag.getString(TAG_MEDIA_KIND));
                return kind == FloppyMedia.Kind.WINDOWS_95_BOOT
                        ? FloppyMedia.windows95Boot()
                        : FloppyMedia.blank(tag.getString(TAG_MEDIA_ID));
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
        return tag.getBoolean(TAG_INSERTED_WIN95_BOOT_FLOPPY) ? FloppyMedia.windows95Boot() : null;
    }
}
