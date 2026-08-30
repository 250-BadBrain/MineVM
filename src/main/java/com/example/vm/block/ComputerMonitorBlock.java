package com.example.vm.block;

import com.example.vm.blockentity.ComputerMonitorBlockEntity;
import com.example.vm.blockentity.ComputerBlockEntity;
import com.example.vm.blockentity.ComputerOccupancyBlockEntity;
import com.example.vm.network.ModNetwork;
import com.example.vm.registry.ModBlocks;
import com.example.vm.item.SystemDiscItem;
import com.example.vm.MineVM;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import com.example.vm.vm.VirtualMachineState;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class ComputerMonitorBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(-16.0D, 5.0D, 3.0D, 32.0D, 32.0D, 7.0D),
            Block.box(6.0D, 2.0D, 7.0D, 10.0D, 10.0D, 10.0D),
            Block.box(0.0D, 0.0D, 6.0D, 16.0D, 2.0D, 14.0D),
            Block.box(-12.0D, 5.2D, 2.5D, -8.0D, 6.6D, 3.3D)
    );

    public ComputerMonitorBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(1.8F, 4.0F)
                .requiresCorrectToolForDrops());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return RotatedShapes.clipToBlock(fullShape(state));
    }

    VoxelShape fullShape(BlockState state) {
        return RotatedShapes.rotateHorizontal(SHAPE, state.getValue(FACING));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ComputerMonitorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        return ComputerFootprint.canPlace(context.getLevel(), context.getClickedPos(), ComputerFootprint.monitorOffsets(state.getValue(FACING))) ? state : null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable net.minecraft.world.entity.LivingEntity placer, net.minecraft.world.item.ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        placeOccupancy(level, pos, ComputerFootprint.monitorOffsets(state.getValue(FACING)));
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            removeOccupancy(level, pos, ComputerFootprint.monitorOffsets(state.getValue(FACING)));
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof ComputerMonitorBlockEntity monitor) {
            if (isDisplayPowerButton(state, pos, hit)) {
                monitor.toggleDisplayPower();
                player.sendSystemMessage(Component.literal("Monitor | Display " + (monitor.displayPowered() ? "on" : "off")));
                return InteractionResult.CONSUME;
            }
            if (player.getItemInHand(hand).getItem() instanceof SystemDiscItem) {
                player.sendSystemMessage(Component.literal("Monitor | Insert system discs into the computer case, not the monitor."));
                return InteractionResult.CONSUME;
            }
            if (player.getItemInHand(hand).isEmpty() && player instanceof ServerPlayer serverPlayer) {
                if (tryStartControl(level, pos, serverPlayer, monitor)) {
                    return InteractionResult.CONSUME;
                }
            }
            String linkedComputer = monitor.linkedComputerId().orElse("NONE");
            player.sendSystemMessage(Component.literal("Monitor | Linked computer: " + linkedComputer + " | Screen control: immersive"));
        }
        return InteractionResult.CONSUME;
    }

    private static boolean tryStartControl(Level level, BlockPos monitorPos, ServerPlayer player, ComputerMonitorBlockEntity monitor) {
        if (monitor.linkedComputerPos().isEmpty()) {
            player.sendSystemMessage(Component.literal("Monitor | Connect HDMI to a computer case first."));
            return true;
        }
        if (!monitor.displayPowered()) {
            player.sendSystemMessage(Component.literal("Monitor | Turn on the monitor display first."));
            return true;
        }

        BlockEntity sourceEntity = level.getBlockEntity(monitor.linkedComputerPos().get());
        if (!(sourceEntity instanceof ComputerBlockEntity computer)) {
            player.sendSystemMessage(Component.literal("Monitor | Linked computer case is missing."));
            return true;
        }

        if (!computer.poweredOn()) {
            player.sendSystemMessage(Component.literal("Monitor | Press the computer case power button first."));
            return true;
        }

        if (!computer.hasDesktopInputDevices()) {
            player.sendSystemMessage(Component.literal("Monitor | Connect keyboard and mouse over USB first."));
            return true;
        }

        startControlAfterStateRefresh(player, monitorPos, monitor.linkedComputerPos().get(), computer);
        return true;
    }

    private static void startControlAfterStateRefresh(ServerPlayer player, BlockPos monitorPos, BlockPos computerPos, ComputerBlockEntity computer) {
        String computerId = computer.getComputerId();
        MineVM.VM_MANAGER.getState(computerId)
                .whenComplete((state, throwable) -> player.server.execute(() -> {
                    if (throwable != null) {
                        computer.setVmDisplayActive(false);
                        player.sendSystemMessage(Component.literal("Monitor | VM state check failed: " + rootMessage(throwable)));
                        return;
                    }
                    boolean vmDisplayActive = computer.vmDisplayActive() && state == VirtualMachineState.RUNNING;
                    if (computer.vmDisplayActive() && !vmDisplayActive) {
                        computer.setVmDisplayActive(false);
                    }
                    if (vmDisplayActive) {
                        MineVM.SCREEN_SERVICE.refreshVncReceiver(computerId);
                    }
                    ModNetwork.startScreenControl(player, monitorPos, computerPos, computerId, false, vmDisplayActive);
                }));
    }

    private static String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current instanceof java.util.concurrent.CompletionException && current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }

    private static boolean isDisplayPowerButton(BlockState state, BlockPos pos, BlockHitResult hit) {
        Direction facing = state.getValue(FACING);
        double x = hit.getLocation().x - pos.getX();
        double y = hit.getLocation().y - pos.getY();
        double z = hit.getLocation().z - pos.getZ();
        double localX = switch (facing) {
            case SOUTH -> 1.0D - x;
            case WEST -> 1.0D - z;
            case EAST -> z;
            default -> x;
        };
        double localZ = switch (facing) {
            case SOUTH -> 1.0D - z;
            case WEST -> x;
            case EAST -> 1.0D - x;
            default -> z;
        };
        return localZ <= 0.22D && localX >= -0.75D && localX <= -0.50D && y >= 0.32D && y <= 0.42D;
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

    private static void placeOccupancy(Level level, BlockPos origin, java.util.List<BlockPos> offsets) {
        if (level.isClientSide) {
            return;
        }
        for (BlockPos offset : offsets) {
            BlockPos pos = origin.offset(offset);
            level.setBlock(pos, ModBlocks.COMPUTER_OCCUPANCY.get().defaultBlockState(), 3);
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof ComputerOccupancyBlockEntity occupancy) {
                occupancy.setOrigin(origin);
            }
        }
    }

    private static void removeOccupancy(LevelAccessor level, BlockPos origin, java.util.List<BlockPos> offsets) {
        for (BlockPos offset : offsets) {
            BlockPos pos = origin.offset(offset);
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof ComputerOccupancyBlockEntity occupancy && occupancy.origin().filter(origin::equals).isPresent()) {
                level.destroyBlock(pos, false);
            }
        }
    }

}
