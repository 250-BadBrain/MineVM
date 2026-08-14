package com.example.vm.block;

import com.example.vm.ComputerInMinecraft;
import com.example.vm.blockentity.ComputerBlockEntity;
import com.example.vm.blockentity.ComputerOccupancyBlockEntity;
import com.example.vm.computer.ComputerDiscActions;
import com.example.vm.computer.ComputerHardwareConfiguration;
import com.example.vm.computer.ComputerFormFactor;
import com.example.vm.computer.ComputerInteractionResolver;
import com.example.vm.computer.ComputerInteractionTarget;
import com.example.vm.computer.ComputerPowerActions;
import com.example.vm.computer.ComputerStatusMessage;
import com.example.vm.config.ComputerRuntimeConfig;
import com.example.vm.item.SystemDiscItem;
import com.example.vm.network.ModNetwork;
import com.example.vm.registry.ModBlocks;
import com.example.vm.screen.VncEndpoint;
import com.example.vm.vm.VirtualMachineDescriptor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletionException;

public class ComputerBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape DESKTOP_SHAPE = Shapes.or(
            Block.box(3.0D, 0.0D, 3.0D, 13.0D, 15.0D, 13.0D),
            Block.box(3.2D, 10.0D, 2.4D, 5.2D, 12.0D, 2.9D)
    );
    private static final VoxelShape LAPTOP_SHAPE = Shapes.or(
            Block.box(-16.0D, 0.0D, 2.0D, 32.0D, 3.5D, 30.0D),
            Block.box(-16.0D, 3.0D, 26.0D, 32.0D, 32.0D, 30.5D),
            Block.box(-13.0D, 3.2D, 7.0D, -8.0D, 4.0D, 11.0D)
    );

    private final ComputerFormFactor formFactor;

    public ComputerBlock() {
        this(ComputerFormFactor.DESKTOP);
    }

    public ComputerBlock(ComputerFormFactor formFactor) {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(2.5F, 6.0F)
                .requiresCorrectToolForDrops());
        this.formFactor = formFactor;
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
        VoxelShape shape = formFactor == ComputerFormFactor.LAPTOP ? LAPTOP_SHAPE : DESKTOP_SHAPE;
        return RotatedShapes.rotateHorizontal(shape, state.getValue(FACING));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ComputerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        if (formFactor != ComputerFormFactor.LAPTOP) {
            return state;
        }
        return ComputerFootprint.canPlace(context.getLevel(), context.getClickedPos(), ComputerFootprint.laptopOffsets(state.getValue(FACING))) ? state : null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable net.minecraft.world.entity.LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (formFactor == ComputerFormFactor.LAPTOP) {
            placeOccupancy(level, pos, ComputerFootprint.laptopOffsets(state.getValue(FACING)));
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (formFactor == ComputerFormFactor.LAPTOP && !state.is(newState.getBlock())) {
            removeOccupancy(level, pos, ComputerFootprint.laptopOffsets(state.getValue(FACING)));
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof ComputerBlockEntity computer)) {
            return InteractionResult.PASS;
        }

        MinecraftServer server = level.getServer();
        if (server == null) {
            return InteractionResult.SUCCESS;
        }

        ItemStack heldItem = player.getItemInHand(hand);
        if (heldItem.getItem() instanceof SystemDiscItem systemDiscItem) {
            insertSystemDisc(player, computer, heldItem, systemDiscItem);
            return InteractionResult.CONSUME;
        }

        if (player.isShiftKeyDown() && heldItem.isEmpty()) {
            ComputerDiscActions.ejectIfStopped(server, player, computer, pos);
            return InteractionResult.CONSUME;
        }

        ComputerInteractionTarget target = ComputerInteractionResolver.resolve(formFactor, state.getValue(FACING), pos, hit);
        if (!player.isShiftKeyDown() && heldItem.isEmpty()) {
            if (player instanceof ServerPlayer serverPlayer) {
                if (target == ComputerInteractionTarget.POWER_BUTTON) {
                    togglePower(server, player, computer);
                } else if (formFactor == ComputerFormFactor.LAPTOP && target == ComputerInteractionTarget.DISPLAY) {
                    if (!computer.poweredOn()) {
                        player.sendSystemMessage(Component.literal("Computer " + computer.getComputerId() + " | Press the laptop power button first."));
                        return InteractionResult.CONSUME;
                    }
                    ModNetwork.startScreenControl(serverPlayer, pos, pos, computer.getComputerId(), true, computer.vmDisplayActive());
                } else {
                    showComputerStatus(server, player, computer);
                }
                return InteractionResult.CONSUME;
            }
        } else if (target == ComputerInteractionTarget.POWER_BUTTON || player.isShiftKeyDown()) {
            togglePower(server, player, computer);
        } else if (target == ComputerInteractionTarget.DISPLAY) {
            showDisplayEndpoint(player, computer);
        } else {
            showComputerStatus(server, player, computer);
        }

        return InteractionResult.CONSUME;
    }

    @Override
    public boolean triggerEvent(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, int id, int param) {
        super.triggerEvent(state, level, pos, id, param);
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity != null && blockEntity.triggerEvent(id, param);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return false;
    }

    @Override
    public int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return 0;
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
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING);
    }

    private static void showComputerStatus(MinecraftServer server, Player player, ComputerBlockEntity computer) {
        String computerId = computer.getComputerId();
        ComputerHardwareConfiguration configuration = computer.getHardwareConfiguration();
        VirtualMachineDescriptor descriptor = ComputerRuntimeConfig.descriptorFor(computer);

        ensureRegistered(computer)
                .thenCompose(ignored -> ComputerInMinecraft.VM_MANAGER.getState(computerId))
                .whenComplete((vmState, throwable) -> server.execute(() -> {
                    if (throwable != null) {
                        player.sendSystemMessage(Component.literal("Computer " + computerId + " | VM status failed: " + rootMessage(throwable)));
                        return;
                    }

                    String screenAddress = VncEndpoint.forComputer(computerId).toDisplayAddress();
                    String insertedDisc = computer.insertedDisc().map(Enum::name).orElse("");
                    player.sendSystemMessage(Component.literal(ComputerStatusMessage.describe(computerId, configuration, vmState, screenAddress, descriptor.guestOperatingSystem(), insertedDisc)));
                }));
    }

    private static java.util.concurrent.CompletableFuture<VirtualMachineDescriptor> ensureRegistered(ComputerBlockEntity computer) {
        if (!ComputerRuntimeConfig.vmDiskExists(computer)) {
            return java.util.concurrent.CompletableFuture.completedFuture(ComputerRuntimeConfig.descriptorFor(computer));
        }
        VirtualMachineDescriptor descriptor = ComputerRuntimeConfig.descriptorFor(computer);
        return ComputerInMinecraft.VM_MANAGER.create(descriptor).thenApply(ignored -> descriptor);
    }

    private static void insertSystemDisc(Player player, ComputerBlockEntity computer, ItemStack heldItem, SystemDiscItem systemDiscItem) {
        if (!computer.insertDisc(systemDiscItem.guestOperatingSystem())) {
            player.sendSystemMessage(Component.literal("Computer " + computer.getComputerId() + " | Disc slot occupied"));
            return;
        }

        if (!player.getAbilities().instabuild) {
            heldItem.shrink(1);
        }
        player.sendSystemMessage(Component.literal("Computer " + computer.getComputerId() + " | Inserted disc: " + systemDiscItem.guestOperatingSystem()));
    }

    private static void showDisplayEndpoint(Player player, ComputerBlockEntity computer) {
        VncEndpoint endpoint = VncEndpoint.forComputer(computer.getComputerId());
        player.sendSystemMessage(Component.literal("Computer " + computer.getComputerId() + " | Screen VNC: " + endpoint.toDisplayAddress()));
    }

    private static void togglePower(MinecraftServer server, Player player, ComputerBlockEntity computer) {
        String computerId = computer.getComputerId();

        if (computer.powerTransitioning()) {
            player.sendSystemMessage(Component.literal("Computer " + computerId + " | Power change already in progress"));
            return;
        }

        if (computer.poweredOn()) {
            ComputerPowerActions.powerOff(server, player, computer.getBlockPos(), computer);
            return;
        }

        computer.setPoweredOn(true);
        computer.setVmDisplayActive(false);
        player.sendSystemMessage(Component.literal("Computer " + computerId + " | Power on | BIOS ready"));
    }

    private static String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current instanceof CompletionException && current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
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
