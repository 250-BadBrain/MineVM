package com.example.vm.item;

import com.example.vm.blockentity.ComputerBlockEntity;
import com.example.vm.blockentity.ComputerMonitorBlockEntity;
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

public class HdmiCableItem extends Item {
    private static final String TAG_SOURCE_POS = "HdmiSourcePos";
    private static final String TAG_SOURCE_ID = "HdmiSourceComputerId";

    public HdmiCableItem(Properties properties) {
        super(properties);
    }

    public CableType cableType() {
        return CableType.HDMI;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide || context.getPlayer() == null) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity blockEntity = context.getLevel().getBlockEntity(context.getClickedPos());
        ItemStack stack = context.getItemInHand();
        if (blockEntity instanceof ComputerBlockEntity computer && isComputerCase(context.getLevel().getBlockState(context.getClickedPos()).getBlock())) {
            if (!computer.canManage(context.getPlayer())) {
                context.getPlayer().sendSystemMessage(Component.translatable("message.minevm.not_owner"));
                return InteractionResult.CONSUME;
            }
            CompoundTag tag = stack.getOrCreateTag();
            tag.putLong(TAG_SOURCE_POS, context.getClickedPos().asLong());
            tag.putString(TAG_SOURCE_ID, computer.getComputerId());
            tag.putString("HdmiDimension", context.getLevel().dimension().location().toString());
            context.getPlayer().sendSystemMessage(Component.translatable("message.minevm.hdmi_selected", computer.getComputerId()));
            return InteractionResult.CONSUME;
        }

        if (blockEntity instanceof ComputerMonitorBlockEntity monitor) {
            CompoundTag tag = stack.getTag();
            if (tag == null || !tag.contains(TAG_SOURCE_POS) || !tag.contains(TAG_SOURCE_ID)) {
                context.getPlayer().sendSystemMessage(Component.translatable("message.minevm.hdmi_select_case"));
                return InteractionResult.CONSUME;
            }

            BlockPos computerPos = BlockPos.of(tag.getLong(TAG_SOURCE_POS));
            if (!context.getLevel().dimension().location().toString().equals(tag.getString("HdmiDimension"))
                    || computerPos.distSqr(context.getClickedPos()) > 1024 || !context.getLevel().hasChunkAt(computerPos)) {
                context.getPlayer().sendSystemMessage(Component.translatable("message.minevm.invalid_endpoint"));
                return InteractionResult.CONSUME;
            }
            BlockEntity sourceEntity = context.getLevel().getBlockEntity(computerPos);
            if (!(sourceEntity instanceof ComputerBlockEntity computer)
                    || !computer.getComputerId().equals(tag.getString(TAG_SOURCE_ID))
                    || !computer.canManage(context.getPlayer())) {
                context.getPlayer().sendSystemMessage(Component.translatable("message.minevm.invalid_endpoint"));
                stack.removeTagKey(TAG_SOURCE_POS);
                stack.removeTagKey(TAG_SOURCE_ID);
                return InteractionResult.CONSUME;
            }

            if (monitor.linkedComputerPos().filter(pos -> !context.getLevel().hasChunkAt(pos)).isPresent()) {
                context.getPlayer().sendSystemMessage(Component.translatable("message.minevm.invalid_endpoint"));
                return InteractionResult.CONSUME;
            }
            BlockEntity previous = monitor.linkedComputerPos().map(context.getLevel()::getBlockEntity).orElse(null);
            if (previous instanceof ComputerBlockEntity oldComputer && !oldComputer.canManage(context.getPlayer())) {
                context.getPlayer().sendSystemMessage(Component.translatable("message.minevm.not_owner"));
                return InteractionResult.CONSUME;
            }
            monitor.linkComputer(computer.getComputerId(), computerPos);
            computer.linkMonitor(context.getClickedPos());
            stack.removeTagKey(TAG_SOURCE_POS);
            stack.removeTagKey(TAG_SOURCE_ID);
            consumeCable(context);
            context.getPlayer().sendSystemMessage(Component.translatable("message.minevm.hdmi_connected"));
            return InteractionResult.CONSUME;
        }

        context.getPlayer().sendSystemMessage(Component.translatable("message.minevm.hdmi_select_case"));
        return InteractionResult.SUCCESS;
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
