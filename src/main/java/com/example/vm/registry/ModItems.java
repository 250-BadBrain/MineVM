package com.example.vm.registry;

import com.example.vm.MineVM;
import com.example.vm.item.BootFloppyItem;
import com.example.vm.item.BlankFloppyItem;
import com.example.vm.item.HdmiCableItem;
import com.example.vm.item.SystemDiscItem;
import com.example.vm.item.UsbCableItem;
import com.example.vm.vm.GuestOperatingSystem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MineVM.MOD_ID);

    public static final RegistryObject<Item> COMPUTER_BLOCK_ITEM =
            ITEMS.register("computer_block", () -> new BlockItem(ModBlocks.COMPUTER_BLOCK.get(), new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> DESKTOP_COMPUTER =
            ITEMS.register("desktop_computer", () -> new BlockItem(ModBlocks.DESKTOP_COMPUTER.get(), new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> LAPTOP_COMPUTER =
            ITEMS.register("laptop_computer", () -> new BlockItem(ModBlocks.LAPTOP_COMPUTER.get(), new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> COMPUTER_MONITOR =
            ITEMS.register("computer_monitor", () -> new BlockItem(ModBlocks.COMPUTER_MONITOR.get(), new Item.Properties()));

    public static final RegistryObject<Item> HDMI_CABLE =
            ITEMS.register("hdmi_cable", () -> new HdmiCableItem(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> USB_CABLE =
            ITEMS.register("usb_cable", () -> new UsbCableItem(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> KEYBOARD =
            ITEMS.register("keyboard", () -> new BlockItem(ModBlocks.KEYBOARD.get(), new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> MOUSE =
            ITEMS.register("mouse", () -> new BlockItem(ModBlocks.MOUSE.get(), new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> FLOPPY_DRIVE =
            ITEMS.register("floppy_drive", () -> new BlockItem(ModBlocks.FLOPPY_DRIVE.get(), new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> WINDOWS_95_BOOT_FLOPPY =
            ITEMS.register("windows_95_boot_floppy", () -> new BootFloppyItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> BLANK_FLOPPY =
            ITEMS.register("blank_floppy", () -> new BlankFloppyItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> UBUNTU_SERVER_DISC =
            ITEMS.register("ubuntu_server_disc", () -> new SystemDiscItem(
                    GuestOperatingSystem.UBUNTU_SERVER,
                    "item.minevm.ubuntu_server_disc",
                    new Item.Properties().stacksTo(1)
            ));

    public static final RegistryObject<Item> UBUNTU_DESKTOP_DISC =
            ITEMS.register("ubuntu_desktop_disc", () -> new SystemDiscItem(
                    GuestOperatingSystem.UBUNTU_DESKTOP,
                    "item.minevm.ubuntu_desktop_disc",
                    new Item.Properties().stacksTo(1)
            ));

    public static final RegistryObject<Item> FREEDOS_DISC =
            ITEMS.register("freedos_disc", () -> new SystemDiscItem(
                    GuestOperatingSystem.FREEDOS,
                    "item.minevm.freedos_disc",
                    new Item.Properties().stacksTo(1)
            ));

    public static final RegistryObject<Item> WINDOWS_95_DISC =
            ITEMS.register("windows_95_disc", () -> new SystemDiscItem(
                    GuestOperatingSystem.WINDOWS_95,
                    "item.minevm.windows_95_disc",
                    new Item.Properties().stacksTo(1)
            ));

    public static final RegistryObject<Item> WINDOWS_98_DISC = ITEMS.register("windows_98_disc",
            () -> new SystemDiscItem(GuestOperatingSystem.WINDOWS_98, "item.minevm.windows_98_disc", new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> WINDOWS_VISTA_DISC = ITEMS.register("windows_vista_disc",
            () -> new SystemDiscItem(GuestOperatingSystem.WINDOWS_VISTA, "item.minevm.windows_vista_disc", new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> WINDOWS_10_DISC =
            ITEMS.register("windows_10_disc", () -> new SystemDiscItem(
                    GuestOperatingSystem.WINDOWS_10,
                    "item.minevm.windows_10_disc",
                    new Item.Properties().stacksTo(1)
            ));

    public static final RegistryObject<Item> WINDOWS_XP_DISC =
            ITEMS.register("windows_xp_disc", () -> new SystemDiscItem(
                    GuestOperatingSystem.WINDOWS_XP,
                    "item.minevm.windows_xp_disc",
                    new Item.Properties().stacksTo(1)
            ));

    public static final RegistryObject<Item> WINDOWS_11_DISC =
            ITEMS.register("windows_11_disc", () -> new SystemDiscItem(
                    GuestOperatingSystem.WINDOWS_11,
                    "item.minevm.windows_11_disc",
                    new Item.Properties().stacksTo(1)
            ));

    private ModItems() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
