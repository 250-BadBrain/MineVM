package com.example.vm.block;

import com.example.vm.MineVM;
import com.example.vm.blockentity.FloppyDriveBlockEntity;
import com.example.vm.floppy.FloppyMedia;
import com.example.vm.item.BlankFloppyItem;
import com.example.vm.registry.ModItems;
import com.example.vm.vm.VirtualMachineState;
import com.example.vm.vm.VirtualMachineHandle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ItemLike;
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
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletableFuture;

public class FloppyDriveBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape SHAPE = Block.box(3.0D, 0.0D, 4.0D, 13.0D, 4.0D, 12.0D);

    public FloppyDriveBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(0.8F, 1.2F));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return RotatedShapes.rotateHorizontal(SHAPE, state.getValue(FACING));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FloppyDriveBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof FloppyDriveBlockEntity floppyDrive)) {
            return InteractionResult.PASS;
        }
        var linked = floppyDrive.linkedComputerPos().filter(level::hasChunkAt).map(level::getBlockEntity);
        if (linked.orElse(null) instanceof com.example.vm.blockentity.ComputerBlockEntity computer
                && floppyDrive.connectedComputerId().filter(computer.getComputerId()::equals).isPresent()
                && !computer.canManage(player)) {
            player.sendSystemMessage(Component.translatable("message.minevm.not_owner"));
            return InteractionResult.CONSUME;
        }

        ItemStack heldItem = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            ejectFloppy(level, player, floppyDrive);
            return InteractionResult.CONSUME;
        }

        if (heldItem.getItem() == ModItems.WINDOWS_95_BOOT_FLOPPY.get()) {
            insertFloppy(level, player, hand, floppyDrive, FloppyMedia.windows95Boot());
            return InteractionResult.CONSUME;
        }

        if (heldItem.getItem() == ModItems.BLANK_FLOPPY.get()) {
            insertFloppy(level, player, hand, floppyDrive, FloppyMedia.blank(BlankFloppyItem.ensureMediaId(heldItem)));
            return InteractionResult.CONSUME;
        }

        String linkedComputer = floppyDrive.linkedComputerId().orElse("NONE");
        String media = floppyDrive.media().map(FloppyDriveBlock::mediaName).orElse("EMPTY");
        player.sendSystemMessage(Component.literal("Floppy Drive | Linked computer: " + linkedComputer + " | Media: " + media));
        return InteractionResult.CONSUME;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        BlockEntity blockEntity = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (blockEntity instanceof FloppyDriveBlockEntity floppyDrive && floppyDrive.hasMedia()) {
            List<ItemStack> drops = new ArrayList<>();
            drops.add(new ItemStack((ItemLike) this));
            floppyDrive.media().map(FloppyDriveBlock::itemFor).ifPresent(drops::add);
            return drops;
        }
        return Collections.singletonList(new ItemStack(this));
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

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && !state.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof FloppyDriveBlockEntity drive) {
            drive.connectedComputerId().ifPresent(computerId -> MineVM.VM_MANAGER.ejectBootFloppy(computerId)
                    .exceptionally(failure -> {
                        MineVM.LOGGER.warn("Failed to detach removed floppy drive for {}", computerId, failure);
                        return null;
                    }));
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    private static void ejectFloppy(Level level, Player player, FloppyDriveBlockEntity floppyDrive) {
        if (!floppyDrive.hasMedia()) {
            player.sendSystemMessage(Component.literal("Floppy Drive | Empty."));
            return;
        }

        if (!floppyDrive.beginMediaChange()) {
            player.sendSystemMessage(Component.literal("Floppy Drive | Media change already in progress."));
            return;
        }
        MinecraftServer server = level.getServer();
        if (server == null) {
            floppyDrive.endMediaChange();
            return;
        }
        changeVmMedia(floppyDrive.connectedComputerId(), null)
                .whenComplete((ignored, failure) -> server.execute(() -> {
                    floppyDrive.endMediaChange();
                    if (!isCurrentDrive(level, floppyDrive)) {
                        return;
                    }
                    if (failure != null) {
                        player.sendSystemMessage(Component.literal("Floppy Drive | VM floppy eject failed: " + rootMessage(failure)));
                        return;
                    }
                    ejectNow(player, floppyDrive, "Ejected floppy.");
                }));
    }

    private static void ejectNow(Player player, FloppyDriveBlockEntity floppyDrive, String message) {
        floppyDrive.eject().ifPresentOrElse(media -> {
            returnItem(player, itemFor(media));
            player.sendSystemMessage(Component.literal("Floppy Drive | " + message));
        }, () -> player.sendSystemMessage(Component.literal("Floppy Drive | Empty.")));
    }

    private static void insertFloppy(Level level, Player player, InteractionHand hand, FloppyDriveBlockEntity floppyDrive, FloppyMedia media) {
        if (floppyDrive.hasMedia()) {
            player.sendSystemMessage(Component.literal("Floppy Drive | A floppy is already inserted."));
            return;
        }

        MinecraftServer server = level.getServer();
        if (server == null) {
            return;
        }
        ItemStack held = player.getItemInHand(hand);
        if (!matchesMediaItem(held, media) || !floppyDrive.beginMediaChange()) {
            player.sendSystemMessage(Component.literal("Floppy Drive | Media change already in progress."));
            return;
        }
        ItemStack reserved = player.getAbilities().instabuild ? ItemStack.EMPTY : held.split(1);
        changeVmMedia(floppyDrive.connectedComputerId(), media)
                .whenComplete((ignored, failure) -> server.execute(() -> {
                    floppyDrive.endMediaChange();
                    if (failure != null || !isCurrentDrive(level, floppyDrive) || !floppyDrive.insert(media)) {
                        returnItem(player, reserved);
                        if (failure != null) {
                            player.sendSystemMessage(Component.literal("Floppy Drive | VM media insert failed: " + rootMessage(failure)));
                        }
                        return;
                    }
                    player.sendSystemMessage(Component.literal("Floppy Drive | Inserted " + mediaName(media) + "."));
                }));
    }

    private static CompletableFuture<VirtualMachineHandle> changeVmMedia(java.util.Optional<String> computerId, FloppyMedia media) {
        if (computerId.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        String id = computerId.get();
        try {
            return MineVM.VM_MANAGER.getState(id).thenCompose(state -> {
                if (state == VirtualMachineState.RUNNING || state == VirtualMachineState.STARTING) {
                    return media == null ? MineVM.VM_MANAGER.ejectBootFloppy(id) : MineVM.VM_MANAGER.changeBootFloppyMedium(id, media);
                }
                if (state == VirtualMachineState.STOPPING) {
                    return CompletableFuture.failedFuture(new IllegalStateException("VM is stopping"));
                }
                return CompletableFuture.completedFuture(null);
            });
        } catch (java.util.concurrent.RejectedExecutionException failure) {
            return CompletableFuture.failedFuture(failure);
        }
    }

    private static boolean isCurrentDrive(Level level, FloppyDriveBlockEntity drive) {
        return !drive.isRemoved() && level.hasChunkAt(drive.getBlockPos())
                && level.getBlockEntity(drive.getBlockPos()) == drive;
    }

    private static void returnItem(Player player, ItemStack stack) {
        if (!stack.isEmpty() && !player.addItem(stack)) {
            player.drop(stack, false);
        }
    }

    private static boolean matchesMediaItem(ItemStack stack, FloppyMedia media) {
        return media.isWindows95Boot()
                ? stack.is(ModItems.WINDOWS_95_BOOT_FLOPPY.get())
                : stack.is(ModItems.BLANK_FLOPPY.get()) && media.mediaId().equals(BlankFloppyItem.ensureMediaId(stack));
    }

    private static ItemStack itemFor(FloppyMedia media) {
        if (media.isWindows95Boot()) {
            return new ItemStack(ModItems.WINDOWS_95_BOOT_FLOPPY.get());
        }
        ItemStack blank = new ItemStack(ModItems.BLANK_FLOPPY.get());
        BlankFloppyItem.setMediaId(blank, media.mediaId());
        return blank;
    }

    private static String mediaName(FloppyMedia media) {
        return media.isWindows95Boot() ? "Windows 95 boot floppy" : "blank writable floppy";
    }

    private static String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current instanceof CompletionException && current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }
}
