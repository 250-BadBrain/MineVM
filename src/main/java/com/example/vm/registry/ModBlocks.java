package com.example.vm.registry;

import com.example.vm.ComputerInMinecraft;
import com.example.vm.block.ComputerBlock;
import com.example.vm.block.ComputerMonitorBlock;
import com.example.vm.block.ComputerOccupancyBlock;
import com.example.vm.block.ComputerPeripheralBlock;
import com.example.vm.computer.ComputerFormFactor;
import com.example.vm.item.ComputerPeripheralType;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    private static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, ComputerInMinecraft.MOD_ID);

    public static final RegistryObject<Block> COMPUTER_BLOCK =
            BLOCKS.register("computer_block", ComputerBlock::new);

    public static final RegistryObject<Block> DESKTOP_COMPUTER =
            BLOCKS.register("desktop_computer", () -> new ComputerBlock(ComputerFormFactor.DESKTOP));

    public static final RegistryObject<Block> LAPTOP_COMPUTER =
            BLOCKS.register("laptop_computer", () -> new ComputerBlock(ComputerFormFactor.LAPTOP));

    public static final RegistryObject<Block> COMPUTER_MONITOR =
            BLOCKS.register("computer_monitor", ComputerMonitorBlock::new);

    public static final RegistryObject<Block> COMPUTER_OCCUPANCY =
            BLOCKS.register("computer_occupancy", ComputerOccupancyBlock::new);

    public static final RegistryObject<Block> KEYBOARD =
            BLOCKS.register("keyboard", () -> new ComputerPeripheralBlock(ComputerPeripheralType.KEYBOARD));

    public static final RegistryObject<Block> MOUSE =
            BLOCKS.register("mouse", () -> new ComputerPeripheralBlock(ComputerPeripheralType.MOUSE));

    private ModBlocks() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }
}
