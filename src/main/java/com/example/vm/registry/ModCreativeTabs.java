package com.example.vm.registry;

import com.example.vm.MineVM;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {
    private static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MineVM.MOD_ID);

    public static final RegistryObject<CreativeModeTab> COMPUTER_TAB = CREATIVE_TABS.register("computer_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.minevm"))
                    .icon(() -> ModItems.DESKTOP_COMPUTER.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.DESKTOP_COMPUTER.get());
                        output.accept(ModItems.COMPUTER_MONITOR.get());
                        output.accept(ModItems.HDMI_CABLE.get());
                        output.accept(ModItems.USB_CABLE.get());
                        output.accept(ModItems.KEYBOARD.get());
                        output.accept(ModItems.MOUSE.get());
                        output.accept(ModItems.LAPTOP_COMPUTER.get());
                        output.accept(ModItems.UBUNTU_SERVER_DISC.get());
                        output.accept(ModItems.UBUNTU_DESKTOP_DISC.get());
                        output.accept(ModItems.FREEDOS_DISC.get());
                        output.accept(ModItems.WINDOWS_95_DISC.get());
                        output.accept(ModItems.WINDOWS_XP_DISC.get());
                        output.accept(ModItems.WINDOWS_10_DISC.get());
                        output.accept(ModItems.WINDOWS_11_DISC.get());
                    })
                    .build());

    private ModCreativeTabs() {
    }

    public static void register(IEventBus modEventBus) {
        CREATIVE_TABS.register(modEventBus);
    }
}
