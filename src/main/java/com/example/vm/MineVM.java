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
        com.example.vm.network.MultiplayerScreens.start();
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        com.example.vm.network.MultiplayerScreens.close();
        VM_MANAGER.close();
        SCREEN_SERVICE.close();
        INPUT_SERVICE.close();
        com.example.vm.screen.FrameMetrics.clear();
    }

    @SubscribeEvent
    public void onServerTick(net.minecraftforge.event.TickEvent.ServerTickEvent event) {
        if (event.phase == net.minecraftforge.event.TickEvent.Phase.END)
            com.example.vm.network.MultiplayerScreens.tick(event.getServer());
    }

    @SubscribeEvent
    public void onPlayerLogout(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player)
            com.example.vm.network.MultiplayerScreens.disconnected(player);
    }

    @SubscribeEvent
    public void onRegisterCommands(net.minecraftforge.event.RegisterCommandsEvent event) {
        com.example.vm.computer.ComputerCommands.register(event);
    }

    @SubscribeEvent
    public void onPlayerLogin(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player
                && !player.getPersistentData().getBoolean("MineVMWelcome")) {
            player.getPersistentData().putBoolean("MineVMWelcome", true);
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.minevm.welcome")
                    .withStyle(style -> style.withClickEvent(new net.minecraft.network.chat.ClickEvent(
                            net.minecraft.network.chat.ClickEvent.Action.SUGGEST_COMMAND, "/minevm help"))));
        }
    }
}
