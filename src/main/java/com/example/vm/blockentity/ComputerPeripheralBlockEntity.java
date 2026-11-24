package com.example.vm.blockentity;

import com.example.vm.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import java.util.Optional;
import java.util.UUID;

public final class ComputerPeripheralBlockEntity extends BlockEntity {
    private String deviceId = UUID.randomUUID().toString();
    private String computerId = "";
    private BlockPos computerPos;

    public ComputerPeripheralBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COMPUTER_PERIPHERAL_BLOCK_ENTITY.get(), pos, state);
    }

    public String deviceId() { return deviceId; }
    public String computerId() { return computerId; }
    public Optional<BlockPos> computerPos() { return Optional.ofNullable(computerPos); }

    public void link(String id, BlockPos pos) {
        computerId = id == null ? "" : id;
        computerPos = pos;
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("DeviceId", deviceId);
        tag.putString("ComputerId", computerId);
        if (computerPos != null) tag.putLong("ComputerPos", computerPos.asLong());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        try { deviceId = UUID.fromString(tag.getString("DeviceId")).toString(); }
        catch (IllegalArgumentException invalid) { deviceId = UUID.randomUUID().toString(); }
        computerId = tag.getString("ComputerId");
        computerPos = tag.contains("ComputerPos") ? BlockPos.of(tag.getLong("ComputerPos")) : null;
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
