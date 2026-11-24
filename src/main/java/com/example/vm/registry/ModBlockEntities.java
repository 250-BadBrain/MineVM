package com.example.vm.registry;

import com.example.vm.MineVM;
import com.example.vm.blockentity.ComputerBlockEntity;
import com.example.vm.blockentity.ComputerMonitorBlockEntity;
import com.example.vm.blockentity.ComputerOccupancyBlockEntity;
import com.example.vm.blockentity.FloppyDriveBlockEntity;
import com.example.vm.blockentity.ComputerPeripheralBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MineVM.MOD_ID);

    public static final RegistryObject<BlockEntityType<ComputerBlockEntity>> COMPUTER_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("computer_block_entity",
                    () -> BlockEntityType.Builder.of(
                            ComputerBlockEntity::new,
                            ModBlocks.COMPUTER_BLOCK.get(),
                            ModBlocks.DESKTOP_COMPUTER.get(),
                            ModBlocks.LAPTOP_COMPUTER.get()
                    ).build(null));

    public static final RegistryObject<BlockEntityType<ComputerMonitorBlockEntity>> COMPUTER_MONITOR_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("computer_monitor_block_entity",
                    () -> BlockEntityType.Builder.of(
                            ComputerMonitorBlockEntity::new,
                            ModBlocks.COMPUTER_MONITOR.get()
                    ).build(null));

    public static final RegistryObject<BlockEntityType<ComputerOccupancyBlockEntity>> COMPUTER_OCCUPANCY_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("computer_occupancy_block_entity",
                    () -> BlockEntityType.Builder.of(
                            ComputerOccupancyBlockEntity::new,
                            ModBlocks.COMPUTER_OCCUPANCY.get()
                    ).build(null));

    public static final RegistryObject<BlockEntityType<FloppyDriveBlockEntity>> FLOPPY_DRIVE_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("floppy_drive_block_entity",
                    () -> BlockEntityType.Builder.of(
                            FloppyDriveBlockEntity::new,
                            ModBlocks.FLOPPY_DRIVE.get()
                    ).build(null));

    private ModBlockEntities() {
    }

    public static final RegistryObject<BlockEntityType<ComputerPeripheralBlockEntity>> COMPUTER_PERIPHERAL_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("computer_peripheral_block_entity", () -> BlockEntityType.Builder.of(
                    ComputerPeripheralBlockEntity::new, ModBlocks.KEYBOARD.get(), ModBlocks.MOUSE.get()).build(null));

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITIES.register(modEventBus);
    }
}
