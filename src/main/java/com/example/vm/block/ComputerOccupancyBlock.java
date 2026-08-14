package com.example.vm.block;

import com.example.vm.blockentity.ComputerOccupancyBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.client.extensions.common.IClientBlockExtensions;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class ComputerOccupancyBlock extends BaseEntityBlock {
    public ComputerOccupancyBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.NONE)
                .strength(1.8F, 4.0F)
                .noOcclusion()
                .noParticlesOnBreak());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void initializeClient(Consumer<IClientBlockExtensions> consumer) {
        consumer.accept(new IClientBlockExtensions() {
            @Override
            public boolean addHitEffects(BlockState state, Level level, net.minecraft.world.phys.HitResult target, net.minecraft.client.particle.ParticleEngine manager) {
                return true;
            }

            @Override
            public boolean addDestroyEffects(BlockState state, Level level, BlockPos pos, net.minecraft.client.particle.ParticleEngine manager) {
                return true;
            }
        });
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return linkedShape(level, pos, context, false);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return linkedShape(level, pos, context, true);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ComputerOccupancyBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        BlockPos origin = origin(level, pos);
        if (origin == null || origin.equals(pos)) {
            if (!level.isClientSide) {
                level.removeBlock(pos, false);
            }
            return InteractionResult.PASS;
        }
        if (level.getBlockState(origin).isAir()) {
            if (!level.isClientSide) {
                level.removeBlock(pos, false);
            }
            return InteractionResult.CONSUME;
        }
        BlockState originState = level.getBlockState(origin);
        return originState.use(level, player, hand, new BlockHitResult(hit.getLocation(), hit.getDirection(), origin, hit.isInside()));
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        BlockPos origin = origin(level, pos);
        if (origin != null && !origin.equals(pos) && !level.isClientSide) {
            if (level.getBlockState(origin).isAir()) {
                level.removeBlock(pos, false);
            } else {
                level.destroyBlock(origin, !player.getAbilities().instabuild, player);
            }
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder builder) {
        return Collections.emptyList();
    }

    private static VoxelShape linkedShape(BlockGetter level, BlockPos pos, CollisionContext context, boolean collision) {
        BlockPos origin = origin(level, pos);
        if (origin == null || origin.equals(pos)) {
            return Shapes.empty();
        }

        BlockState originState = level.getBlockState(origin);
        if (!(originState.getBlock() instanceof ComputerBlock) && !(originState.getBlock() instanceof ComputerMonitorBlock)) {
            return Shapes.empty();
        }

        VoxelShape originShape;
        if (originState.getBlock() instanceof ComputerBlock computerBlock) {
            originShape = computerBlock.fullShape(originState);
        } else if (originState.getBlock() instanceof ComputerMonitorBlock monitorBlock) {
            originShape = monitorBlock.fullShape(originState);
        } else {
            originShape = collision
                    ? originState.getCollisionShape(level, origin, context)
                    : originState.getShape(level, origin, context);
        }
        VoxelShape localSlice = originShape.move(origin.getX() - pos.getX(), origin.getY() - pos.getY(), origin.getZ() - pos.getZ());
        return RotatedShapes.clipToBlock(localSlice);
    }

    @Nullable
    public static BlockPos origin(BlockGetter level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof ComputerOccupancyBlockEntity occupancy) {
            return occupancy.origin().orElse(null);
        }
        return null;
    }
}
