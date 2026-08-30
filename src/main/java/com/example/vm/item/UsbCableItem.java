package com.example.vm.item;

import com.example.vm.block.ComputerPeripheralBlock;
import com.example.vm.blockentity.ComputerBlockEntity;
import com.example.vm.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

public class UsbCableItem extends Item {
    private static final String TAG_PERIPHERAL_POS = "UsbPeripheralPos";
    private static final String TAG_PERIPHERAL_TYPE = "UsbPeripheralType";

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
            context.getPlayer().sendSystemMessage(Component.literal("USB Cable | Selected " + peripheralBlock.peripheralType() + "."));
            return InteractionResult.CONSUME;
        }

        if (!(blockEntity instanceof ComputerBlockEntity computer) || !isComputerCase(block)) {
            context.getPlayer().sendSystemMessage(Component.literal("USB Cable | Select a placed keyboard or mouse, then use on a computer case."));
            return InteractionResult.CONSUME;
        }

        ComputerPeripheralType selectedType = selectedPeripheralType(context);
        if (selectedType != null) {
            connectPeripheral(computer, selectedType);
            stack.removeTagKey(TAG_PERIPHERAL_POS);
            stack.removeTagKey(TAG_PERIPHERAL_TYPE);
            consumeCable(context);
            context.getPlayer().sendSystemMessage(Component.literal("USB Cable | Connected " + selectedType + " to computer case."));
            return InteractionResult.CONSUME;
        }

        InteractionHand otherHand = context.getHand() == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack peripheralStack = context.getPlayer().getItemInHand(otherHand);
        ComputerPeripheralType fallbackType = peripheralTypeFromStack(peripheralStack);
        if (fallbackType == null) {
            context.getPlayer().sendSystemMessage(Component.literal("USB Cable | Select a keyboard or mouse first."));
            return InteractionResult.CONSUME;
        }

        connectPeripheral(computer, fallbackType);
        consumeCable(context);
        context.getPlayer().sendSystemMessage(Component.literal("USB Cable | Connected " + fallbackType + " to computer case."));
        return InteractionResult.SUCCESS;
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

    private static ComputerPeripheralType peripheralTypeFromStack(ItemStack stack) {
        if (stack.getItem() instanceof ComputerPeripheralItem peripheralItem) {
            return peripheralItem.peripheralType();
        }
        if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ComputerPeripheralBlock peripheralBlock) {
            return peripheralBlock.peripheralType();
        }
        return null;
    }

    private static void connectPeripheral(ComputerBlockEntity computer, ComputerPeripheralType peripheralType) {
        if (peripheralType == ComputerPeripheralType.KEYBOARD) {
            computer.connectKeyboard();
        } else if (peripheralType == ComputerPeripheralType.MOUSE) {
            computer.connectMouse();
        }
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
