package com.example.vm.blockentity;

import com.example.vm.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class ComputerMonitorBlockEntity extends BlockEntity {
    private static final String TAG_LINKED_COMPUTER_ID = "LinkedComputerId";
    private static final String TAG_LINKED_COMPUTER_POS = "LinkedComputerPos";
    private static final String TAG_DISPLAY_POWERED = "DisplayPowered";
    private String linkedComputerId = "";
    private BlockPos linkedComputerPos;
    private boolean displayPowered;

    public ComputerMonitorBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.COMPUTER_MONITOR_BLOCK_ENTITY.get(), pos, blockState);
    }

    public Optional<String> linkedComputerId() {
        return linkedComputerId.isBlank() ? Optional.empty() : Optional.of(linkedComputerId);
    }

    public Optional<BlockPos> linkedComputerPos() {
        return Optional.ofNullable(linkedComputerPos);
    }

    public boolean displayPowered() {
        return displayPowered;
    }

    public void toggleDisplayPower() {
        displayPowered = !displayPowered;
        markUpdated();
    }

    public void linkComputer(String computerId, BlockPos computerPos) {
        linkedComputerId = computerId == null ? "" : computerId;
        linkedComputerPos = computerPos;
        markUpdated();
    }

    public void clearLink() {
        linkedComputerId = "";
        linkedComputerPos = null;
        markUpdated();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (!linkedComputerId.isBlank()) {
            tag.putString(TAG_LINKED_COMPUTER_ID, linkedComputerId);
        }
        if (linkedComputerPos != null) {
            tag.putLong(TAG_LINKED_COMPUTER_POS, linkedComputerPos.asLong());
        }
        tag.putBoolean(TAG_DISPLAY_POWERED, displayPowered);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        linkedComputerId = tag.contains(TAG_LINKED_COMPUTER_ID) ? tag.getString(TAG_LINKED_COMPUTER_ID) : "";
        linkedComputerPos = tag.contains(TAG_LINKED_COMPUTER_POS) ? BlockPos.of(tag.getLong(TAG_LINKED_COMPUTER_POS)) : null;
        displayPowered = tag.getBoolean(TAG_DISPLAY_POWERED);
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
}
