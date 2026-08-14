package com.example.vm.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

public class ComputerPeripheralItem extends Item {
    private final ComputerPeripheralType peripheralType;

    public ComputerPeripheralItem(ComputerPeripheralType peripheralType, Properties properties) {
        super(properties);
        this.peripheralType = peripheralType;
    }

    public ComputerPeripheralType peripheralType() {
        return peripheralType;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!context.getLevel().isClientSide && context.getPlayer() != null) {
            context.getPlayer().sendSystemMessage(Component.literal(peripheralType + " | Use USB Cable to connect this peripheral."));
        }
        return InteractionResult.SUCCESS;
    }
}
