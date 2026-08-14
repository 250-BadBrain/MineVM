package com.example.vm.blockentity;

import com.example.vm.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class ComputerOccupancyBlockEntity extends BlockEntity {
    private static final String TAG_ORIGIN = "Origin";
    private BlockPos origin;

    public ComputerOccupancyBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.COMPUTER_OCCUPANCY_BLOCK_ENTITY.get(), pos, blockState);
    }

    public Optional<BlockPos> origin() {
        return Optional.ofNullable(origin);
    }

    public void setOrigin(BlockPos origin) {
        this.origin = origin;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (origin != null) {
            tag.putLong(TAG_ORIGIN, origin.asLong());
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        origin = tag.contains(TAG_ORIGIN) ? BlockPos.of(tag.getLong(TAG_ORIGIN)) : null;
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
}
