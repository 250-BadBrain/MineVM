package com.example.vm;

import com.example.vm.config.ComputerRuntimeConfig;
import com.example.vm.input.InputService;
import com.example.vm.registry.ModBlocks;
import com.example.vm.registry.ModBlockEntities;
import com.example.vm.registry.ModCreativeTabs;
import com.example.vm.registry.ModItems;
import com.example.vm.network.ModNetwork;
import com.example.vm.screen.ScreenService;
import com.example.vm.vm.VirtualMachineManager;
import com.example.vm.vm.backend.NoopVirtualMachineBackend;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(MineVM.MOD_ID)
public class MineVM {
    public static final String MOD_ID = "minevm";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final VirtualMachineManager VM_MANAGER =
            new VirtualMachineManager(new NoopVirtualMachineBackend());
    public static final ScreenService SCREEN_SERVICE = new ScreenService();
    public static final InputService INPUT_SERVICE = new InputService(VM_MANAGER);

    public MineVM(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ComputerRuntimeConfig.SPEC);

        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModItems.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModNetwork.register();

        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        VM_MANAGER.replaceBackend(ComputerRuntimeConfig.createBackend());
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        VM_MANAGER.close();
        SCREEN_SERVICE.close();
        INPUT_SERVICE.close();
    }
}
