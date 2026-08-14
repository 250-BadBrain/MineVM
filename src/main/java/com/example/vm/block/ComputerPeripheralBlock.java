package com.example.vm.block;

import com.example.vm.item.ComputerPeripheralType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class ComputerPeripheralBlock extends Block {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape KEYBOARD_SHAPE = Block.box(1.0D, 0.0D, 4.0D, 15.0D, 1.0D, 12.0D);
    private static final VoxelShape MOUSE_SHAPE = Block.box(5.0D, 0.0D, 4.0D, 11.0D, 2.0D, 12.0D);

    private final ComputerPeripheralType peripheralType;

    public ComputerPeripheralBlock(ComputerPeripheralType peripheralType) {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(0.6F, 1.0F));
        this.peripheralType = peripheralType;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public ComputerPeripheralType peripheralType() {
        return peripheralType;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = peripheralType == ComputerPeripheralType.KEYBOARD ? KEYBOARD_SHAPE : MOUSE_SHAPE;
        return RotatedShapes.rotateHorizontal(shape, state.getValue(FACING));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}
