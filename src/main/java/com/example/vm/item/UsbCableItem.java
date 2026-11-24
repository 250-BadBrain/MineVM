package com.example.vm.item;

import com.example.vm.block.ComputerPeripheralBlock;
import com.example.vm.blockentity.ComputerBlockEntity;
import com.example.vm.blockentity.FloppyDriveBlockEntity;
import com.example.vm.blockentity.ComputerPeripheralBlockEntity;
import com.example.vm.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

public class UsbCableItem extends Item {
    private static final String TAG_PERIPHERAL_POS = "UsbPeripheralPos";
    private static final String TAG_PERIPHERAL_TYPE = "UsbPeripheralType";
    private static final String USB_DEVICE_FLOPPY_DRIVE = "FLOPPY_DRIVE";

    public UsbCableItem(Properties properties) {
        super(properties);
    }

    public CableType cableType() {
        return CableType.USB;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide || context.getPlayer() == null) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity blockEntity = context.getLevel().getBlockEntity(context.getClickedPos());
        Block block = context.getLevel().getBlockState(context.getClickedPos()).getBlock();
        ItemStack stack = context.getItemInHand();
        if (block instanceof ComputerPeripheralBlock peripheralBlock) {
            CompoundTag tag = stack.getOrCreateTag();
            tag.putLong(TAG_PERIPHERAL_POS, context.getClickedPos().asLong());
            tag.putString(TAG_PERIPHERAL_TYPE, peripheralBlock.peripheralType().name());
            if (blockEntity instanceof ComputerPeripheralBlockEntity device) tag.putString("UsbDeviceId", device.deviceId());
            tag.putString("UsbDimension", context.getLevel().dimension().location().toString());
            context.getPlayer().sendSystemMessage(Component.translatable("message.minevm.usb_selected", peripheralBlock.peripheralType().name()));
            return InteractionResult.CONSUME;
        }
        if (blockEntity instanceof FloppyDriveBlockEntity drive) {
            CompoundTag tag = stack.getOrCreateTag();
            tag.putLong(TAG_PERIPHERAL_POS, context.getClickedPos().asLong());
            tag.putString(TAG_PERIPHERAL_TYPE, USB_DEVICE_FLOPPY_DRIVE);
            tag.putString("UsbDeviceId", drive.deviceId());
            tag.putString("UsbDimension", context.getLevel().dimension().location().toString());
            context.getPlayer().sendSystemMessage(Component.translatable("message.minevm.usb_floppy_selected"));
            return InteractionResult.CONSUME;
        }

        if (!(blockEntity instanceof ComputerBlockEntity computer) || !isComputerCase(block)) {
            context.getPlayer().sendSystemMessage(Component.translatable("message.minevm.place_peripheral"));
            return InteractionResult.CONSUME;
        }
        if (!computer.canManage(context.getPlayer())) {
            context.getPlayer().sendSystemMessage(Component.translatable("message.minevm.not_owner"));
            return InteractionResult.CONSUME;
        }
        CompoundTag selection = stack.getTag();
        if (selection != null && selection.contains(TAG_PERIPHERAL_POS)
                && (!context.getLevel().dimension().location().toString().equals(selection.getString("UsbDimension"))
                || BlockPos.of(selection.getLong(TAG_PERIPHERAL_POS)).distSqr(context.getClickedPos()) > 1024
                || !context.getLevel().hasChunkAt(BlockPos.of(selection.getLong(TAG_PERIPHERAL_POS))))) {
            context.getPlayer().sendSystemMessage(Component.translatable("message.minevm.invalid_endpoint"));
            return InteractionResult.CONSUME;
        }

        if (tryConnectSelectedFloppyDrive(context, computer, stack)) {
            return InteractionResult.CONSUME;
        }

        ComputerPeripheralType selectedType = selectedPeripheralType(context);
        if (selectedType != null) {
            BlockPos devicePos = BlockPos.of(stack.getTag().getLong(TAG_PERIPHERAL_POS));
            if (!(context.getLevel().getBlockEntity(devicePos) instanceof ComputerPeripheralBlockEntity device)
                    || !device.deviceId().equals(stack.getTag().getString("UsbDeviceId"))) {
                context.getPlayer().sendSystemMessage(Component.translatable("message.minevm.invalid_endpoint"));
                return InteractionResult.CONSUME;
            }
            ComputerBlockEntity previous = device.computerPos().filter(context.getLevel()::hasChunkAt)
                    .map(context.getLevel()::getBlockEntity).filter(ComputerBlockEntity.class::isInstance)
                    .map(ComputerBlockEntity.class::cast).orElse(null);
            if (device.computerPos().filter(pos -> !context.getLevel().hasChunkAt(pos)).isPresent()
                    || previous != null && (!previous.canManage(context.getPlayer()) || previous.poweredOn() || previous.powerTransitioning())) {
                context.getPlayer().sendSystemMessage(Component.translatable("message.minevm.rewire_power_off"));
                return InteractionResult.CONSUME;
            }
            if (previous != null) previous.disconnectPeripheral(devicePos);
            computer.connectPeripheral(selectedType, device);
            stack.removeTagKey(TAG_PERIPHERAL_POS);
            stack.removeTagKey(TAG_PERIPHERAL_TYPE);
            consumeCable(context);
            context.getPlayer().sendSystemMessage(Component.translatable("message.minevm.usb_connected", selectedType.name()));
            return InteractionResult.CONSUME;
        }

        context.getPlayer().sendSystemMessage(Component.translatable("message.minevm.place_peripheral"));
        return InteractionResult.CONSUME;
    }

    private boolean tryConnectSelectedFloppyDrive(UseOnContext context, ComputerBlockEntity computer, ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_PERIPHERAL_POS) || !USB_DEVICE_FLOPPY_DRIVE.equals(tag.getString(TAG_PERIPHERAL_TYPE))) {
            return false;
        }

        BlockPos floppyDrivePos = BlockPos.of(tag.getLong(TAG_PERIPHERAL_POS));
        BlockEntity selectedBlockEntity = context.getLevel().getBlockEntity(floppyDrivePos);
        if (!(selectedBlockEntity instanceof FloppyDriveBlockEntity floppyDrive)
                || !floppyDrive.deviceId().equals(tag.getString("UsbDeviceId"))) {
            context.getPlayer().sendSystemMessage(Component.translatable("message.minevm.invalid_endpoint"));
            stack.removeTagKey(TAG_PERIPHERAL_POS);
            stack.removeTagKey(TAG_PERIPHERAL_TYPE);
            return true;
        }

        ComputerBlockEntity previous = floppyDrive.linkedComputerPos()
                .filter(context.getLevel()::hasChunkAt)
                .map(context.getLevel()::getBlockEntity)
                .filter(ComputerBlockEntity.class::isInstance)
                .map(ComputerBlockEntity.class::cast).orElse(null);
        if (computer.poweredOn() || computer.powerTransitioning() || floppyDrive.mediaChanging()
                || floppyDrive.linkedComputerPos().filter(pos -> !context.getLevel().hasChunkAt(pos)).isPresent()
                || previous != null && (previous.poweredOn() || previous.powerTransitioning() || !previous.canManage(context.getPlayer()))) {
            context.getPlayer().sendSystemMessage(Component.translatable("message.minevm.rewire_power_off"));
            return true;
        }

        computer.linkFloppyDrive(floppyDrivePos);
        floppyDrive.linkComputer(computer.getComputerId(), context.getClickedPos());
        stack.removeTagKey(TAG_PERIPHERAL_POS);
        stack.removeTagKey(TAG_PERIPHERAL_TYPE);
        consumeCable(context);
        context.getPlayer().sendSystemMessage(Component.translatable("message.minevm.usb_floppy_connected"));
        return true;
    }

    private ComputerPeripheralType selectedPeripheralType(UseOnContext context) {
        CompoundTag tag = context.getItemInHand().getTag();
        if (tag == null || !tag.contains(TAG_PERIPHERAL_POS) || !tag.contains(TAG_PERIPHERAL_TYPE)) {
            return null;
        }

        BlockPos peripheralPos = BlockPos.of(tag.getLong(TAG_PERIPHERAL_POS));
        Block selectedBlock = context.getLevel().getBlockState(peripheralPos).getBlock();
        if (!(selectedBlock instanceof ComputerPeripheralBlock peripheralBlock)) {
            return null;
        }

        ComputerPeripheralType storedType;
        try {
            storedType = ComputerPeripheralType.valueOf(tag.getString(TAG_PERIPHERAL_TYPE));
        } catch (IllegalArgumentException exception) {
            return null;
        }
        return peripheralBlock.peripheralType() == storedType ? storedType : null;
    }

    private static boolean isComputerCase(Block block) {
        return block == ModBlocks.DESKTOP_COMPUTER.get() || block == ModBlocks.COMPUTER_BLOCK.get();
    }

    private static void consumeCable(UseOnContext context) {
        if (!context.getPlayer().getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }
    }
}
